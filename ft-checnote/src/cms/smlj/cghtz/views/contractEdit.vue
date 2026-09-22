<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js"
import {formColumnsOf, formGroupsOf, columnOf, optionsOf} from "../utils/ExcelX.js"
import {notifyError} from "@/framework/services/net/NwCodeMap.js"
import {useRouter, useRoute} from 'vue-router';
import dayjs from 'dayjs';
import DeptPicker from "../components/DeptPicker.vue"
import {
    buildDeptPathMap,
    deptDisplay,
    deptScopeDepts,
    effectiveScope,
    SCOPE
} from "../utils/DeptX.js"
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts"

/**
 * 合同编辑页（每个合同模板一个"页面"）
 * ----------------
 * 一条合同长什么样，由它所在部门的**合同模板**决定（t_contract_template.tb_name 指向的物理表）。
 * 所以本页没有写死任何字段：进页面的入参是 tb（物理表名），字段清单与分组从 gd.json 的
 * contractTables[tb] 读 —— 台账页在哪个部门点的"新增/编辑"，这里就渲染哪张表的表单。
 *
 * 路由：/home/contract/:tb/:uid?     uid 省略 = 新增
 *       query.dept_code             新增时的默认归属部门（从台账页带过来）
 *
 * 三个配置来源（都在 gd.json，本文件不硬编码任何字段名或文案）：
 *   columns[]      字段清单：type / required / options / span / mirror / system+widget / readonly
 *                  （readonly 三态：true 恒只读 / "edit" 仅编辑时只读 / 缺省可写；
 *                    合同编号用它锁住「改」，新增时仍要能填）
 *   formGroups[]   表单分组（标题 + 字段顺序）；没被任何分组引用的列会自动并入「其他」
 */
const route = useRoute();
const router = useRouter();

/** 目标物理表：路由参数，与后端 registeredTable 白名单同源 */
const tb = String(route.params.tb || '')
/** 编辑对象的主键；空 = 新增 */
const uid = String(route.params.uid || '')
const isEdit = !!uid

/** 该模版的字段清单与分组（gd.json 里缺登记时为空） */
const cols = formColumnsOf(tb)
const cfgOk = cols.length > 0

/* ---------------- 账号与数据范围（与台账页同口径） ---------------- */
const {wsCache} = useSessionCache()
const _acc = wsCache.get(ECacheType.ACCOUNT) || {}
const dataScope = effectiveScope(_acc)
const myDeptCode = _acc.dept_code || ''
const onlySelf = dataScope === SCOPE.SELF

/** 归属部门字形：优先展示「公司/部门」全路径（DeptPicker 内部也用它回显） */
const allDeptOptions = ref([])
const deptPathMap = computed(() => buildDeptPathMap(allDeptOptions.value))

/** 可选部门：与后端 expandScope 同口径（4 全量 / 3 本公司子树 / 2 起点子树 / 其余起点） */
const deptOptions = computed(() => deptScopeDepts(allDeptOptions.value, dataScope, myDeptCode) ?? allDeptOptions.value)

/* ---------------- 表单模型 ---------------- */
const formRef = ref()
const form = ref({})
const loading = ref(false)
const saving = ref(false)

/** 字段默认值：数字留空用 undefined（el-input-number 的"未填"），其余按类型给 */
function emptyValue(c) {
    if (c.type === 'float' || c.type === 'int') return undefined
    if (c.type === 'bool') return false
    return ''
}

/** 日期：兼容 PG date 串 / ISO 串 / 时间戳；统一成 el-date-picker 认的 YYYY-MM-DD */
function dateStr(v) {
    const d = dateToDate(v)
    return d ? dayjs(d).format('YYYY-MM-DD') : ''
}

function dateToDate(v) {
    if (v === null || v === undefined || v === '') return null
    if (typeof v === 'number') return new Date(v)
    if (v instanceof Date) return v
    const s = String(v)
    const m = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/)
    // 「2025-09-04」与「2025-09-04 00:00:00」都只取日期部分，避免时区把日期挪一天
    if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
    const d = new Date(s)
    return isNaN(d.getTime()) ? null : d
}

/** 库里的原始值 → 表单值 */
function pickValue(c, raw) {
    if (c.type === 'date') return dateStr(raw)
    if (c.type === 'float' || c.type === 'int') return raw === null || raw === undefined || raw === '' ? undefined : Number(raw)
    if (c.type === 'bool') return raw === true
    return raw === null || raw === undefined ? '' : String(raw)
}

/* ---------------- 「同某字段」联动（如预付款日期默认=签订日期） ----------------
 * 由列上的 mirror 声明，不在本文件里写死哪两个字段联动。
 * 新增时默认勾上（值随源字段走、目标控件只读）；编辑时不主动改值，只有用户动了源字段才跟进。 */
const mirrorCols = computed(() => cfgOk ? cols.filter(c => c.mirror && columnOf(tb, c.mirror)) : [])
const mirrorSources = computed(() => new Set(mirrorCols.value.map(c => c.mirror)))
const mirrorOn = ref({})

function syncMirror() {
    mirrorCols.value.forEach(c => {
        if (mirrorOn.value[c.field]) form.value[c.field] = form.value[c.mirror] || ''
    })
}

/* ---------------- 字段渲染配置 ----------------
 * 每个字段渲染成什么控件，只由列配置推导（type / options / system+widget），页面不认字段名。 */
function widgetOf(c) {
    if (c.system && c.widget === 'dept') return 'dept'
    if (optionsOf(c)) return 'select'
    if (c.type === 'date') return 'date'
    if (c.type === 'float') return 'float'
    if (c.type === 'int') return 'int'
    if (c.type === 'bool') return 'bool'
    return (c.span >= 24) ? 'textarea' : 'text'
}

/**
 * 该列在当前形态下是否只读 —— 由列上的 readonly 声明决定，页面不认字段名：
 *   readonly: true    恒只读
 *   readonly: "edit"  仅编辑时只读（新增可填）—— 合同编号用它：它是这条合同的业务标识，
 *                     编号唯一性校验与付款预警都以它为键，编辑时改掉等于悄悄换了一条合同
 *   缺省              可写
 * 口径：只读是**体验层**的 —— 后端 contractUpdate 不特判 id（编辑态按原值写回 = 无变更），
 * 挡的是"手滑改掉编号"，不挡直调接口。
 */
function readOnlyOf(c) {
    return c.readonly === true || (c.readonly === 'edit' && isEdit)
}

/**
 * 分组后的渲染字段。missing = 分组配置里引用了但该表没有的字段（配置写错时不静默吞掉）。
 */
const groups = computed(() => {
    if (!cfgOk) return []
    return formGroupsOf(tb)
        .map(g => ({
            title: g.title,
            fields: g.fields
                .map(f => cols.find(c => c.field === f))
                .filter(Boolean)
                .map(c => ({
                    ...c,
                    label: c.header,
                    span: c.span || 12,
                    widget: widgetOf(c),
                    opts: optionsOf(c),
                    // 「本人」档：归属判据是 sign_person = 登录者姓名（后端精确比对），
                    // 自由填写（空格 / 别人的名字）会让这条合同从自己的列表里消失 —— 预填并锁只读。
                    // readonly 列（合同编号）同理：只在编辑态锁，新增时它是空白、必须能填。
                    disabled: (c.field === 'sign_person' && onlySelf) || readOnlyOf(c),
                    mirrorOn: !!(c.mirror && mirrorOn.value[c.field]),
                })),
        }))
        .filter(g => g.fields.length)
})

/** 某字段的表头文案（mirror 的复选框要显示"同 XX"） */
function headerOf(field) {
    const c = columnOf(tb, field)
    return c ? c.header : field
}

/** 必填校验：只按列配置生成，不按字段名特判 */
const rules = computed(() => {
    const r = {}
    cols.forEach(c => {
        if (!c.required) return
        const trigger = (c.type === 'float' || c.type === 'int' || optionsOf(c)) ? 'change' : 'blur'
        r[c.field] = [{required: true, message: `请填写${c.header}`, trigger}]
    })
    return r
})

/* ---------------- 数据加载 ---------------- */
onMounted(() => {
    loadDepts()
    initForm()
})

function initForm() {
    const base = {}
    cols.forEach(c => {
        base[c.field] = emptyValue(c)
        if (c.field === 'sign_person' && onlySelf) base[c.field] = _acc.username || ''
    })
    form.value = base

    if (isEdit) {
        loadRow()
        return
    }
    // 新增：归属部门默认取台账页带过来的部门（用户在那里点的新增，语境就是那个部门）
    const dc = String(route.query.dept_code || '')
    if (dc && cols.some(c => c.field === 'dept_code')) form.value.dept_code = dc
    mirrorCols.value.forEach(c => {
        mirrorOn.value[c.field] = true
    })
    syncMirror()
}

function loadRow() {
    loading.value = true
    Singleton.getInstance(SysX).getContract({tb, unique_id: uid}, new AbortController().signal, () => {
    }, (r, data) => {
        loading.value = false
        if (!r) {
            notifyError(data, '读取合同失败')
            return
        }
        const row = data.data || {}
        cols.forEach(c => {
            form.value[c.field] = pickValue(c, row[c.field])
        })
    })
}

// 归属部门字典：登录后已由 SysX 预加载缓存，这里命中缓存即刻返回
function loadDepts() {
    Singleton.getInstance(SysX).getDeptList(null, null, () => {
    }, (r, data) => {
        if (r) allDeptOptions.value = data.data || []
    })
}

/* ---------------- 交互 ---------------- */
/** 任一字段变更后可做的联动；当前只有 mirror（源字段变了才需要跟进） */
function onFieldChange(c) {
    if (mirrorSources.value.has(c.field)) syncMirror()
}

/** DeptPicker 是程序化 emit，不在表单元素的事件链上，不显式 validateField 会残留必填红字 */
function onDeptChange() {
    formRef.value?.validateField('dept_code').catch(() => {
    })
}

/**
 * 表单值 → 请求体（后端 pairsOf 会再核对列名与可写性）。
 * 空白值的落库口径与 ExcelX.emptyOf 一致，按"库里的约束"来，不由前端发挥：
 *   数字 → 0（这些列在库里多是 NOT NULL DEFAULT 0，写 0 不会踩 NOT NULL）
 *   是/否 → false
 *   日期 → null（日期列都可空）
 *   文本 → ''（varchar 的 NOT NULL 只约束"不能是 NULL"，空串是合法值）；
 *          标了 nullWhenEmpty 的列（sign_type）空值归一 null，与"未填写"语义一致
 */
function buildPayload() {
    const out = {tb}
    if (isEdit) out.unique_id = uid
    cols.forEach(c => {
        let v = form.value[c.field]
        if (c.type === 'float') {
            const n = Number(v)
            out[c.field] = Number.isFinite(n) ? n : 0
        } else if (c.type === 'int') {
            const n = Number(v)
            out[c.field] = Number.isFinite(n) ? parseInt(n, 10) : 0
        } else if (c.type === 'bool') {
            out[c.field] = v === true
        } else {
            const s = (v === null || v === undefined) ? '' : String(v).trim()
            if (s === '') out[c.field] = (c.type === 'text' && !c.nullWhenEmpty) ? '' : null
            else out[c.field] = s
        }
    })
    return out
}

function save() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const payload = buildPayload()
        const fn = isEdit
            ? Singleton.getInstance(SysX).updateContract
            : Singleton.getInstance(SysX).createContract
        fn(payload, new AbortController().signal, () => {
        }, (r, data) => {
            saving.value = false
            if (r) {
                ElMessage.success(isEdit ? '保存成功' : '录入成功')
                gotoLedger()
            } else {
                notifyError(data, '操作失败')
            }
        })
    })
}

/** 回台账：带上本次的部门，回去就停在同一个部门的列表上 */
function gotoLedger() {
    const dc = form.value.dept_code || String(route.query.dept_code || '')
    router.push(dc ? {name: 'home_ledger', query: {dept_code: dc}} : {name: 'home_ledger'})
}
</script>

<template>
    <div class="contract-edit-page">
        <div class="page-head">
            <div class="head-left">
                <el-button link type="primary" @click="gotoLedger">← 返回台账</el-button>
                <span class="head-title">{{ isEdit ? '编辑合同' : '新增合同' }}</span>
                <el-tag v-if="tb" size="small" type="info" effect="plain">{{ tb }}</el-tag>
            </div>
            <div class="head-right">
                <el-button @click="gotoLedger">取消</el-button>
                <el-button v-if="cfgOk" type="primary" :loading="saving" @click="save">
                    {{ isEdit ? '保存修改' : '保存' }}
                </el-button>
            </div>
        </div>

        <el-card v-if="!cfgOk" shadow="never" class="empty-card">
            <div class="empty-state">
                <div class="empty-icon">🧩</div>
                <div class="empty-title">数据表 {{ tb }} 的字段配置尚未登记</div>
                <div class="empty-desc">前端配置（data/gd.json）里没有这张表的字段清单，无法渲染编辑表单。请联系管理员补齐配置。</div>
            </div>
        </el-card>

        <el-card v-else shadow="never" class="form-card" v-loading="loading">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="170px">
                <template v-for="g in groups" :key="g.title">
                    <el-divider content-position="left">{{ g.title }}</el-divider>
                    <el-row :gutter="16">
                        <el-col v-for="c in g.fields" :key="c.field" :span="c.span">
                            <el-form-item :label="c.label" :prop="c.field">
                                <!-- 归属部门：下拉 + 组织架构树（数据范围已收窄，越权部门在树上不可选） -->
                                <DeptPicker v-if="c.widget === 'dept'" v-model="form[c.field]" :depts="deptOptions"
                                            :teleported="false" :disabled="c.disabled" @change="onDeptChange"/>

                                <!-- 有可选值的列（付款类型 / 财务环节 / 是否挂账…）值就是库里的存储值 -->
                                <el-select v-else-if="c.widget === 'select'" v-model="form[c.field]" clearable
                                           :disabled="c.disabled" placeholder="请选择" style="width:100%"
                                           @change="onFieldChange(c)">
                                    <el-option v-for="o in c.opts" :key="String(o.v)" :label="o.label" :value="o.v"/>
                                </el-select>

                                <template v-else-if="c.widget === 'date'">
                                    <div class="date-line">
                                        <el-checkbox v-if="c.mirror" v-model="mirrorOn[c.field]" @change="syncMirror">
                                            同{{ headerOf(c.mirror) }}
                                        </el-checkbox>
                                        <el-date-picker v-model="form[c.field]" type="date" format="YYYY-MM-DD"
                                                        value-format="YYYY-MM-DD" style="flex:1"
                                                        :disabled="c.disabled || c.mirrorOn"
                                                        @change="onFieldChange(c)"/>
                                    </div>
                                </template>

                                <el-input-number v-else-if="c.widget === 'float'" v-model="form[c.field]"
                                                 :precision="2" :controls="false" style="width:100%" :disabled="c.disabled"
                                                 @change="onFieldChange(c)"/>
                                <el-input-number v-else-if="c.widget === 'int'" v-model="form[c.field]"
                                                 :precision="0" :controls="false" style="width:100%" :disabled="c.disabled"
                                                 @change="onFieldChange(c)"/>

                                <!-- 是/否：库里没有 options 的布尔列走这里 -->
                                <el-switch v-else-if="c.widget === 'bool'" v-model="form[c.field]"
                                           :disabled="c.disabled" @change="onFieldChange(c)"/>

                                <el-input v-else-if="c.widget === 'textarea'" v-model="form[c.field]" type="textarea"
                                          :rows="3" :disabled="c.disabled" :placeholder="`请输入${c.label}`"
                                          @change="onFieldChange(c)"/>
                                <el-input v-else v-model="form[c.field]" :disabled="c.disabled"
                                          :placeholder="`请输入${c.label}`" clearable @change="onFieldChange(c)"/>
                            </el-form-item>
                        </el-col>
                    </el-row>
                </template>
            </el-form>
        </el-card>
    </div>
</template>

<style lang="scss" scoped>
.contract-edit-page {
    font-size: 12px;

    :deep(.el-form-item__label) {
        font-size: 12px;
    }

    :deep(.el-input__inner) {
        font-size: 12px;
    }

    :deep(.el-select__wrapper),
    :deep(.el-date-editor .el-input__inner) {
        font-size: var(--cghtz-dd-font-size);
    }

    :deep(.el-checkbox),
    :deep(.el-checkbox__label) {
        font-size: 12px;
    }

    .page-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;

        .head-left {
            display: flex;
            align-items: center;
            gap: 10px;

            .head-title {
                font-size: 16px;
                font-weight: 700;
            }
        }

        .head-right {
            display: flex;
            align-items: center;
            gap: 8px;
        }
    }

    /* 「同某字段」复选框与日期并排：勾上后日期控件只读，值跟着源字段走 */
    .date-line {
        display: flex;
        align-items: center;
        gap: 8px;
        width: 100%;
    }

    .empty-card {
        :deep(.el-card__body) {
            padding: 0;
        }

        .empty-state {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 10px;
            min-height: 320px;
            text-align: center;
            padding: 0 24px;

            .empty-icon {
                font-size: 40px;
                line-height: 1;
            }

            .empty-title {
                font-size: 15px;
                font-weight: 600;
                color: #334155;
            }

            .empty-desc {
                font-size: 13px;
                color: #94a3b8;
                max-width: 560px;
                line-height: 1.7;
            }
        }
    }
}
</style>
