<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {buildDeptPathMap, buildScopedDeptTree, matchDept, deptDisplay, deptScopeDepts, effectiveScope} from "../utils/DeptX.js"
import {downloadTemplate, templateColumns, templateExamples, templateHints} from "../utils/ExcelX.js"
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts"
import {notifyError} from "@/framework/services/net/NwCodeMap.js"
import {ElMessage, ElMessageBox} from "element-plus"

/* ---------------- 组织架构字典（左树 + 部门名回显） ---------------- */
const deptOptions = ref([])
const deptPathMap = computed(() => buildDeptPathMap(deptOptions.value))

/** 部门全路径回显：命中字典 → 「公司/部门」；未命中 → 「未知部门(code)」 */
function deptPath(code) {
    return deptDisplay(deptPathMap.value, code)
}

/* ---------------- 合同模版字典 ---------------- */
// 模版是系统配置（由建表脚本/运维登记），页面只选不造；id 与 tb_name 一一对应
const templates = ref([])
const tplByKey = computed(() => {
    const m = {}
    templates.value.forEach(t => {
        if (t && t.id != null) m[String(t.id)] = t
    })
    return m
})

/* ---------------- 模板预览与下载（与"有没有选中部门"完全无关） ----------------
 * 为什么放在这一页的上半区：给部门定模板之前，管理员常常并不清楚某套模板到底有哪些列、
 * 哪些必填。原来这两个入口在「批量导入」页 —— 而那一步先得选部门才能用，
 * 等于"想看一眼模板长什么样"要先挑个部门。这里把它挪到配置动作的上方，随手可看。
 * 本区块不读 deptCode，也不写任何绑定关系；选中/清空部门都不会影响按钮的可用性。 */
const previewTplId = ref('')          // 配合 el-select 用字符串值，'' = 未选择
const previewTpl = computed(() => tplByKey.value[previewTplId.value] || null)
const previewReady = computed(() => !!previewTpl.value)
/**
 * 该模板的物理表是否已接入导入链路（后端 template/list 下发的 importable）。
 * 台账读路径还没按模板路由，所以登记了别的物理表的模板"下载了也导不进去"，
 * 注意：这里只用来在卡片上出红字提示，**不拦下载** —— 模板本身是可离线填报、可转发的表格，
 * 拦住下载既解决不了"导入没打通"，又断掉唯一可用的线下途径（下载页 onDownload 处有详述）。
 * 判据写成 "!== false"：后端没下发该字段（例如命中旧缓存）时不拦，由后端 fail-closed 兜底。
 */
const previewImportable = computed(() => !previewTpl.value || previewTpl.value.importable !== false)
// 表头清单按"该模板引用的物理表"取，与下载模板同源（都读 gd.json 的 contractTables），不会两处漂移
const previewColumns = computed(() => (previewReady.value ? templateColumns(previewTpl.value.tb_name) : []))
const previewRequired = computed(() => previewColumns.value.filter(c => c.required).map(c => c.header))
// 填写说明（每列一句：类型 / 是否必填 / 格式样例）与示范行同样来自 gd.json，
// 与下载下来的模板逐格同源 —— 页面上看到什么，下载下来的就是什么
const previewHints = computed(() => (previewReady.value ? templateHints(previewTpl.value.tb_name) : []))
const previewExamples = computed(() => (previewReady.value ? templateExamples(previewTpl.value.tb_name) : []))
/** 该列在第 1 条示范行里的取值：一眼看到"这格该长什么样" */
function demoOf(field) {
    const ex = previewExamples.value[0]
    const v = ex ? ex[field] : ''
    return v === undefined || v === null || v === '' ? '—' : v
}
const previewOpen = ref(false)

// 只有一套模板时不至于让上半区看起来"没东西可点"：这里给个初值。
// 与导入页的"不替用户默认选中"不同 —— 预览没有任何落库语义，默认值不会造成误解。
watch(templates, ls => {
    if (!previewTplId.value && ls && ls.length) previewTplId.value = String(ls[0].id)
})

/**
 * 下载导入模板：列按该模板引用的物理表取；文件名带上模板名，多模板下载下来的文件不会互相覆盖。
 * 物理表尚未接入导入链路时**照样允许下载**：模板本身就是一张可离线填报、可发给业务同事的表格，
 * 拦住下载既解决不了"导入没打通"，又断掉了唯一可用的线下途径。风险改为卡片上的红字提示；
 * 真到线上导入那一步，后端 fail-closed 会整批拒绝，不会写坏数据。
 */
function onDownload() {
    if (!previewReady.value) {
        ElMessage.warning('请先选择要下载的合同模板')
        return
    }
    downloadTemplate(previewTpl.value.name, previewTpl.value.tb_name)
}

/** 在页面上先对一眼表头（并给出列数/必填清单），省一次"下完才发现拿错模板" */
function onPreview() {
    if (!previewReady.value) {
        ElMessage.warning('请先选择要预览的合同模板')
        return
    }
    previewOpen.value = true
}

/* ---------------- 部门 → 模版 绑定 ---------------- */
// 后端已按操作者数据范围收窄，前端只负责建索引，不再过滤（过滤口径只有一处）
const bindList = ref([])
const bindMap = computed(() => {
    const m = {}
    bindList.value.forEach(b => {
        if (b && b.dept_code) m[b.dept_code] = b
    })
    return m
})
/** 当前选中部门的「自身绑定」记录；null = 该部门未绑定 */
const bind = computed(() => bindMap.value[deptCode.value] || null)

/** 上级索引：生效模版要向组织树上溯 */
const parentOf = computed(() => {
    const m = {}
    deptOptions.value.forEach(d => {
        if (d && d.dept_code) m[d.dept_code] = d.parent_dept_code || ''
    })
    return m
})

/**
 * 该部门「实际生效」的模版 = 自身绑定，否则沿组织树向上取最近的已绑定祖先。
 * 为什么要有它：一个部门没单独绑定时，它用的其实是上级的模版 ——
 * 只显示"未绑定"会让管理员以为这个部门没有模版可用，进而重复绑一遍。
 * 返回 null 表示整条链上都没有绑定。owner 是本部门时是"自己的设置"，否则是"继承"。
 */
const effective = computed(() => {
    let cur = deptCode.value
    const seen = new Set()      // 防脏数据成环
    while (cur && !seen.has(cur)) {
        seen.add(cur)
        const b = bindMap.value[cur]
        if (b) {
            return {
                tpl: tplByKey.value[String(b.tpl_id)] || null,
                tplId: String(b.tpl_id),
                owner: cur,
                ownerPath: deptPath(cur),
            }
        }
        cur = parentOf.value[cur] || ''
    }
    return null
})

/** 树上节点的模版徽标文案：已绑定才显示（未绑定保持树面干净） */
function tplBadge(code) {
    const b = code && bindMap.value[code]
    if (!b) return ''
    const t = tplByKey.value[String(b.tpl_id)]
    return t ? t.name : `#${b.tpl_id}`
}

/* ---------------- 数据范围（与账号页同口径） ---------------- */
// 前端只用来收窄左树候选：受限账号看不到范围外的部门，因此也不该给它们配上模版。
// ⚠️ 不是安全边界 —— 后端 deptTpl/save|delete 各自有 perm:assign + minScope + inScope 三道闸。
const {wsCache} = useSessionCache()
const _acc = wsCache.get(ECacheType.ACCOUNT) || {}
const myScope = effectiveScope(_acc)
const myDeptCode = _acc.dept_code || ''
const myVisibleDepts = computed(() => deptScopeDepts(deptOptions.value, myScope, myDeptCode))
/** 三态：null = 不限制（全集团）/ 空集 = 一个都不可见（fail-closed），刻意不做全量 fallback */
const visibleDeptCodes = computed(() => {
    const vis = myVisibleDepts.value
    return vis === null ? null : new Set(vis.map(d => d.dept_code))
})
const treeData = computed(() => buildScopedDeptTree(deptOptions.value, visibleDeptCodes.value))

/* ---------------- 左树交互 ---------------- */
const treeRef = ref()
const treeKeyword = ref('')
const deptCode = ref('')

/** 表单里的模版下拉值（字符串，配合 el-select；空串 = 未选择） */
const pickedTplId = ref('')

function filterNode(value, data) {
    return matchDept(data, value, deptPathMap.value)
}

watch(treeKeyword, v => {
    treeRef.value?.filter(String(v || ''))
})

function nodeTitle(data) {
    return data.selectable ? deptPath(data.dept_code) : '该部门不在你的数据范围内，仅作为层级路径展示'
}

function onTreeClick(data) {
    if (!data.selectable) {
        ElMessage.warning('该部门不在你的数据范围内，仅作为层级路径展示')
        return
    }
    deptCode.value = data.dept_code
    syncForm()
}

function clearDept() {
    deptCode.value = ''
    pickedTplId.value = ''
    treeRef.value?.setCurrentKey(null)
}

/** 把「当前选中部门的自身绑定」同步到表单 —— 未绑定则留空，不让用户误以为已配 */
function syncForm() {
    const b = bindMap.value[deptCode.value]
    pickedTplId.value = (b && b.tpl_id != null) ? String(b.tpl_id) : ''
}

/* ---------------- 数据加载 ---------------- */
const loading = ref(false)
const saving = ref(false)
const unbinding = ref(false)

const AC_dept = new AbortController()
const AC_tpl = new AbortController()
const AC_bind = new AbortController()

onMounted(() => {
    loadDepts()
    loadTemplates()
    loadBinds()
})

onUnmounted(() => {
    AC_dept.abort()
    AC_tpl.abort()
    AC_bind.abort()
})

function loadDepts() {
    Singleton.getInstance(SysX).getDeptList(null, AC_dept.signal, () => {
    }, (r, data) => {
        if (r) deptOptions.value = data.data || []
    })
}

function loadTemplates() {
    Singleton.getInstance(SysX).getTemplateList(null, AC_tpl.signal, () => {
    }, (r, data) => {
        if (r) templates.value = data.data || []
    })
}

function loadBinds() {
    loading.value = true
    Singleton.getInstance(SysX).getDeptTplList(null, AC_bind.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) {
            bindList.value = data.data || []
            syncForm()
        } else {
            notifyError(data, '加载部门模板绑定失败')
        }
    })
}

/* ---------------- 保存 / 解除绑定 ---------------- */
function save() {
    if (!deptCode.value) {
        ElMessage.warning('请先从左侧选择部门')
        return
    }
    if (!pickedTplId.value) {
        // 空值不等于"解除"：清空下拉只是没选，贸然提交会被后端判成非法入参
        ElMessage.warning('请选择合同模板；若要取消该部门的绑定，请点「解除绑定」')
        return
    }
    saving.value = true
    Singleton.getInstance(SysX).saveDeptTpl({
        dept_code: deptCode.value,
        tpl_id: pickedTplId.value,
    }, null, () => {
    }, (r, data) => {
        saving.value = false
        if (r) {
            const name = tplByKey.value[pickedTplId.value]?.name || ''
            ElMessage.success(`已将「${deptPath(deptCode.value)}」的模板设为 ${name}`)
            loadBinds()
        } else {
            notifyError(data, '保存失败')
        }
    })
}

/**
 * 解除绑定前的二次确认：这一步不可逆 —— 解除后该部门回到"未绑定"，
 * 导入页会直接拦下这个部门（要求先来本页配置），等于把已经定好的口径清掉了。
 */
function unbind() {
    ElMessageBox.confirm(
        `确定解除「${deptPath(deptCode.value)}」的合同模板绑定吗？解除后该部门将没有可用的模板，也无法导入数据。`,
        '解除绑定',
        {type: 'warning', confirmButtonText: '确定解除', cancelButtonText: '取消'}
    ).then(unbindConfirmed).catch(() => {
    })
}

function unbindConfirmed() {
    unbinding.value = true
    Singleton.getInstance(SysX).deleteDeptTpl({dept_code: deptCode.value}, null, () => {
    }, (r, data) => {
        unbinding.value = false
        if (r) {
            ElMessage.success('已解除绑定')
            loadBinds()
        } else {
            notifyError(data, '解除绑定失败')
        }
    })
}

/** 后端下发的是 ISO/时间戳，统一按「YYYY-MM-DD HH:mm」展示 */
function fmtTime(v) {
    if (!v) return '-'
    const d = new Date(v)
    if (Number.isNaN(d.getTime())) return String(v)
    const p = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
</script>

<template>
    <div class="dept-tpl-page">
        <div class="page-head">
            <div class="head-title">部门合同模板</div>
            <div class="head-desc">先在上方预览模板长什么样，再为部门指定默认合同模板；一个部门只能有一套模板，重复设置即覆盖</div>
        </div>

        <div class="dept-tpl-body">
            <!-- 左侧：常驻组织架构。已绑定的部门在节点右侧带模版徽标，一眼看出哪些还没配 -->
            <el-card shadow="never" class="dept-aside">
                <div class="aside-head">
                    <span class="aside-title">组织架构</span>
                    <span v-if="deptCode" class="aside-clear" @click="clearDept">清空</span>
                </div>
                <el-input v-model="treeKeyword" placeholder="搜索部门" clearable size="small" class="aside-search">
                    <template #prefix><span style="color:#94a3b8">🔍</span></template>
                </el-input>
                <div class="tree-box">
                    <el-tree
                        ref="treeRef"
                        :data="treeData"
                        node-key="dept_code"
                        :props="{label: 'dept_name', children: 'children'}"
                        :current-node-key="deptCode || null"
                        :filter-node-method="filterNode"
                        highlight-current
                        default-expand-all
                        :expand-on-click-node="false"
                        @node-click="onTreeClick"
                    >
                        <template #default="{ data }">
                            <span class="tree-node" :class="{'node-plain': !data.selectable}" :title="nodeTitle(data)">
                                <span class="node-name">{{ data.dept_name }}</span>
                                <span v-if="tplBadge(data.dept_code)" class="node-tpl">{{ tplBadge(data.dept_code) }}</span>
                            </span>
                        </template>
                    </el-tree>
                </div>
            </el-card>

            <div class="tpl-col">
                <!-- 上半：模板预览与下载。与左树是否选中部门无关 —— 先看清模板，再决定给谁配 -->
                <el-card shadow="never" class="preview-card">
                    <div class="pv-head">
                        <span class="pv-title">模板预览与下载</span>
                        <span class="pv-sub">与部门选择无关，随时可看</span>
                    </div>
                    <div class="pv-body">
                        <el-select v-model="previewTplId" placeholder="选择要查看的合同模板" clearable style="width:270px">
                            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="String(t.id)">
                                <span class="opt-name">{{ t.name }}</span>
                            </el-option>
                        </el-select>
                        <el-button type="primary" :disabled="!previewReady" @click="onDownload">
                            ⬇️ 下载导入模板
                        </el-button>
                        <el-button :disabled="!previewReady" @click="onPreview">👁 预览模板表头</el-button>
                    </div>
                    <div class="pv-meta">
                        <template v-if="previewTpl">
                            写入物理表 <b>{{ previewTpl.tb_name }}</b> · 共 {{ previewColumns.length }} 列 · 其中必填
                            <b>{{ previewRequired.length }}</b> 项 · 随附 <b>{{ previewExamples.length }}</b> 条示范行
                        </template>
                        <span v-else class="pv-empty">未选择模板，可先在下拉里挑一套看看</span>
                        <span v-if="previewTpl && !previewImportable" class="pv-blocked">
                            该物理表尚未接入导入链路：模板可下载填写，线上导入暂不可用
                        </span>
                    </div>
                    <div v-if="previewRequired.length" class="pv-req">
                        必填字段：{{ previewRequired.join('、') }}
                    </div>
                    <div class="pv-tip">
                        下载的模板共 4 段：第 1 行字段名、第 2 行中文表头、第 3 行填写说明（每列的类型 / 是否必填 / 格式样例）、
                        第 4 行起为示范行。直接在示范行下面接着填即可，说明行与示范行导入时自动忽略
                    </div>
                </el-card>

                <!-- 下半：该部门的默认合同模版 -->
                <el-card shadow="never" class="tpl-card" v-loading="loading">
                    <div v-if="!deptCode" class="empty-hint">
                        <div class="empty-icon">🗂️</div>
                        <div class="empty-text">请从左侧组织架构中选择部门</div>
                        <div class="empty-sub">选中后可查看并调整该部门的默认合同模板</div>
                    </div>

                    <template v-else>
                        <div class="tpl-head">
                            <span class="tpl-dept" :title="deptPath(deptCode)">{{ deptPath(deptCode) }}</span>
                            <span class="tpl-code"> -- {{ deptCode }}</span>
                        </div>

                        <el-divider/>

                        <el-form label-width="96px" class="tpl-form">
                            <el-form-item label="本部门模板">
                                <el-select v-model="pickedTplId" placeholder="未绑定，请选择" clearable style="width:300px">
                                    <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="String(t.id)">
                                        <span class="opt-name">{{ t.name }}</span>
                                    </el-option>
                                </el-select>
                                <div class="form-tip">一个部门只能有一套模板，保存后直接覆盖原有设置</div>
                            </el-form-item>

                            <el-form-item label="当前生效">
                                <el-tag v-if="effective" :type="effective.owner === deptCode ? 'success' : 'warning'" effect="plain">
                                    {{ effective.tpl ? effective.tpl.name : `#${effective.tplId}` }}
                                </el-tag>
                                <span v-else class="none-text">未绑定</span>
                                <div class="form-tip">
                                    <template v-if="effective && effective.owner === deptCode">来自本部门的设置</template>
                                    <template v-else-if="effective">继承自 {{ effective.ownerPath }}</template>
                                    <template v-else>本部门及所有上级部门都没有绑定模板，该部门目前无法导入数据</template>
                                </div>
                            </el-form-item>

                            <el-form-item v-if="bind" label="上次修改">
                                <span class="meta-text">{{ bind.bound_by || '-' }} · {{ fmtTime(bind.bound_at) }}</span>
                            </el-form-item>

                            <el-form-item>
                                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
                                <el-button :disabled="!bind" :loading="unbinding" @click="unbind">解除绑定</el-button>
                            </el-form-item>
                        </el-form>
                    </template>
                </el-card>
            </div>
        </div>

        <!-- 模板表头预览：列清单与下载下来的 Excel 同源，先在页面上对一眼 -->
        <el-dialog v-model="previewOpen" :title="`导入模板表头 · ${previewTpl ? previewTpl.name : ''}`" width="900px">
            <el-table :data="previewColumns" border size="small" max-height="440">
                <el-table-column type="index" label="#" width="46" align="center"/>
                <el-table-column prop="field" label="字段名" width="150"/>
                <el-table-column prop="header" label="Excel 表头" min-width="130"/>
                <el-table-column label="类型" width="72" align="center">
                    <template #default="{row}">
                        <span class="type-tag">{{ row.typeLabel }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="必填" width="56" align="center">
                    <template #default="{row}">
                        <span v-if="row.required" style="color:#dc2626;font-weight:600">是</span>
                    </template>
                </el-table-column>
                <el-table-column label="填写说明" min-width="176">
                    <template #default="{$index}">
                        <span>{{ previewHints[$index] }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="示范值" min-width="150">
                    <template #default="{row}">
                        <span class="demo-val">{{ demoOf(row.field) }}</span>
                    </template>
                </el-table-column>
            </el-table>
            <div style="font-size:12px;color:#94a3b8;margin-top:12px;line-height:1.8">
                下载的模板与上表逐格同源：第 1 行字段名、第 2 行中文表头、第 3 行填写说明、第 4 行起为示范行<template
                    v-if="previewExamples.length">（共 {{ previewExamples.length }} 条）</template>；说明行与示范行导入时自动忽略，请勿改动前三行。
            </div>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.dept-tpl-page {
    /* 与 ledger-page / users-page 保持同一字号基准 */
    font-size: 10px;

    :deep(.el-form-item__label) {
        font-size: 12px;
    }

    :deep(.el-input__inner) {
        font-size: 12px;
    }

    /* 下拉类控件统一走全局变量（见 styles/cghtz.css），压回 el-input__inner 那条 12px */
    :deep(.el-select__wrapper) {
        font-size: var(--cghtz-dd-font-size);
    }

    :deep(.el-button) {
        font-size: 12px;
    }

    :deep(.el-tag) {
        font-size: 12px;
    }

    .page-head {
        margin-bottom: 16px;

        .head-title {
            font-size: 18px;
            font-weight: 700;
        }

        .head-desc {
            font-size: 13px;
            color: #94a3b8;
            margin-top: 4px;
        }
    }

    /* 左树 + 右配置：两栏等高定长，树超高时在 .tree-box 内部滚动 */
    .dept-tpl-body {
        display: flex;
        align-items: stretch;
        gap: 12px;
        min-height: 460px;

        .dept-aside {
            width: 268px;
            flex-shrink: 0;
            height: 620px;
            display: flex;
            flex-direction: column;

            :deep(.el-card__body) {
                flex: 1;
                min-height: 0;
                display: flex;
                flex-direction: column;
                padding: 12px;
            }

            .aside-head {
                display: flex;
                align-items: center;
                justify-content: space-between;

                .aside-title {
                    font-size: 13px;
                    font-weight: 600;
                }

                .aside-clear {
                    font-size: 12px;
                    color: #2563eb;
                    cursor: pointer;
                }
            }

            .aside-search {
                margin-top: 10px;
            }

            .tree-box {
                flex: 1;
                min-height: 0;
                margin-top: 8px;
                overflow: auto;
                border: 1px solid #e2e8f0;
                border-radius: 8px;
                padding: 4px 2px;

                :deep(.el-tree) {
                    font-size: 12px;
                    background: transparent;
                }

                /* 节点内容行是 flex 容器：让文本项 flex:1 + min-width:0 才能真的触发省略号 */
                :deep(.el-tree-node__content) {
                    overflow: hidden;
                }
            }
        }

        /* 右列竖排两张卡：上半 = 模板预览与下载，下半 = 该部门的模板配置。
           高度与左树对齐，超出部分在各自卡片内部滚动。 */
        .tpl-col {
            flex: 1;
            min-width: 0;
            height: 620px;
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .preview-card {
            /* 固定占右列上部约 2/5：内容是紧凑的（下拉 + 两个按钮 + 元信息），
               不给下限的话卡片会扁成一条，看着像"附属说明"而不是上半区的主体。 */
            flex: 0 0 auto;
            min-height: 248px;
            display: flex;
            flex-direction: column;

            :deep(.el-card__body) {
                flex: 1;
                display: flex;
                flex-direction: column;
                justify-content: center;
                padding: 14px 16px;
            }
        }

        .tpl-card {
            flex: 1;
            min-height: 0;

            :deep(.el-card__body) {
                padding: 16px;
                overflow: auto;
            }
        }
    }

    /* ---- 上半区：模板预览与下载 ---- */
    .preview-card {
        .pv-head {
            display: flex;
            align-items: baseline;
            gap: 8px;

            .pv-title {
                font-size: 14px;
                font-weight: 600;
                color: #0f172a;
            }

            .pv-sub {
                font-size: 12px;
                color: #94a3b8;
            }
        }

        .pv-body {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px;
            margin-top: 12px;
        }

        .pv-meta {
            margin-top: 10px;
            font-size: 12px;
            color: #64748b;

            b {
                color: #2563eb;
            }

            .pv-empty {
                color: #e6a23c;
            }

            /* 物理表没接入导入链路：红字阻断，与"未选择"的橙色区分开 */
            .pv-blocked {
                margin-left: 10px;
                color: #dc2626;
            }
        }

        .pv-req {
            margin-top: 6px;
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.6;
        }

        .pv-tip {
            margin-top: 6px;
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.6;
        }
    }

    /* 树节点：部门名占满剩余宽度并省略，已绑定的部门在右侧挂模版徽标 */
    .tree-node {
        flex: 1;
        min-width: 0;
        display: flex;
        align-items: center;
        gap: 6px;
        overflow: hidden;

        .node-name {
            flex: 1;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .node-tpl {
            flex-shrink: 0;
            max-width: 96px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font-size: 11px;
            line-height: 16px;
            padding: 0 5px;
            border-radius: 8px;
            color: #2563eb;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
        }

        /* 祖先路径节点：不在数据范围内，不可点，灰显 */
        &.node-plain {
            color: #c0c4cc;
            cursor: not-allowed;
        }
    }

    .empty-hint {
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: #94a3b8;

        .empty-icon {
            font-size: 34px;
            line-height: 1;
            margin-bottom: 12px;
        }

        .empty-text {
            font-size: 14px;
            color: #475569;
        }

        .empty-sub {
            font-size: 12px;
            margin-top: 6px;
        }
    }

    .tpl-head {
        .tpl-dept {
            font-size: 15px;
            font-weight: 600;
            color: #0f172a;
            word-break: break-all;
        }

        .tpl-code {
            font-size: 12px;
            color: #94a3b8;
            margin-top: 2px;
        }
    }

    .tpl-form {
        .form-tip {
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.5;
            margin-top: 2px;
        }

        .none-text {
            font-size: 12px;
            color: #94a3b8;
        }

        .meta-text {
            font-size: 12px;
            color: #64748b;
        }
    }

    /* 预览弹窗里的类型标签与示范值 */
    .type-tag {
        display: inline-block;
        padding: 0 6px;
        border-radius: 8px;
        font-size: 11px;
        line-height: 18px;
        color: #2563eb;
        background: #eff6ff;
        border: 1px solid #bfdbfe;
    }

    .demo-val {
        color: #64748b;
    }

    /* 下拉选项：左边模版名，右边物理表名（让管理员知道这套模板落在哪张表） */
    .opt-name {
        float: left;
    }

    .opt-tb {
        float: right;
        color: #94a3b8;
        font-size: 11px;
        margin-left: 16px;
    }
}
</style>
