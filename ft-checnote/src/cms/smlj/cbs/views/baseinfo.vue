<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* ============ 路由驱动单页（一个菜单 = 一个功能，不再用 el-tabs 聚合） ============ */
const TABS = ['ent', 'intro', 'ts', 'chem', 'proc', 'cert', 'certemp']
const TITLES = {
    ent: '企业基本信息',
    intro: '企业简介',
    ts: '安全"三同时"',
    chem: '许可的危险化学品',
    proc: '重点监管的危险化工工艺',
    cert: '企业证书',
    certemp: '企业人员证书',
}
const props = defineProps({tab: String})
const activeTab = ref(TABS.includes(props?.tab) ? props.tab : 'ent')
const pageTitle = computed(() => TITLES[activeTab.value] || '')
watch(() => props.tab, t => {
    if (TABS.includes(t) && t !== activeTab.value) activeTab.value = t
})
let AC = new AbortController()

/* ============ 企业基本信息：两大块，字段名与目标站一致（不简写） ============ */
// kind: input / number / date / select / bool(布尔是-否) / yesno(字符串是-否) / textarea / cascader / upload
const ENT_BLOCKS = [
    {
        title: '企业基本信息', items: [
            {key: 'ent_name', label: '企业名称', required: true},
            {key: 'uscc', label: '统一社会信用代码', required: true},
            {key: 'legal_person', label: '法定代表人', required: true},
            {key: 'legal_phone', label: '法定代表人联系电话', required: true},
            {key: 'legal_idcard', label: '法定代表人身份证号', required: true},
            {key: 'chem_reg_code', label: '危险化学品登记信息管理系统企业编码'},
            {key: 'found_date', label: '成立时间', kind: 'date', required: true},
            {key: 'industry_type', label: '行业分类', kind: 'select', options: gd.entIndustry},
            {key: 'supervisor', label: '主管部门', required: true},
            {key: 'staff_count', label: '职工总数', kind: 'number', required: true},
            {key: 'region_code', label: '行政区划', kind: 'cascader', required: true},
            {key: 'address', label: '单位地址', required: true},
            {key: 'fixed_assets', label: '固定资产（万元）', kind: 'number', required: true},
            {key: 'total_area', label: '总建筑面积（㎡）', kind: 'number'},
            {key: 'industry_detail', label: '企业所属行业及类型', kind: 'select', options: gd.entIndustry, disabled: true, required: true, tip: '随行业分类联动'},
            {key: 'sub_units', label: '下级单位'},
            {key: 'economy_type', label: '经济类型', kind: 'select', options: gd.economyTypes, required: true},
            {key: 'run_status', label: '企业经营状态', kind: 'select', options: gd.entRunStatus, required: true},
            {key: 'supervise_level', label: '监管层级', kind: 'select', options: gd.superviseLevels, required: true},
            {key: 'main_person', label: '企业主要负责人', required: true},
            {key: 'main_person_idcard', label: '主要负责人身份证号', required: true},
            {key: 'safety_org', label: '安全管理机构', required: true},
            {key: 'safety_org_phone', label: '安全管理机构电话', required: true},
            {key: 'safety_org_leader', label: '安全管理机构负责人', required: true},
            {key: 'safety_org_leader_phone', label: '安全管理机构负责人手机', required: true},
            {key: 'duty_phone', label: '值班电话', required: true},
            {key: 'full_safety_count', label: '专职安全管理人员数', kind: 'number', required: true},
            {key: 'part_safety_count', label: '兼职安全管理人员数', kind: 'number', required: true},
            {key: 'has_major_hazard', label: '是否有危化品重大危险源', kind: 'bool', required: true},
            {key: 'risk_identified', label: '是否进行风险辨识', kind: 'bool', required: true},
            {key: 'hazard_filed', label: '危化品重大危险源是否备案', kind: 'yesno', required: true},
            {key: 'has_danger_process', label: '涉及危险工艺', kind: 'textarea', wide: true},
            {key: 'capacity', label: '企业生产能力', kind: 'textarea', wide: true},
            {key: 'ent_scale', label: '企业规模', kind: 'select', options: gd.entScales, required: true},
            {key: 'ent_position', label: '企业定位'},
            {key: 'two_key_one_major', label: '是否涉及两重点一重大作业', kind: 'yesno', required: true},
            {key: 'server_addr', label: '企业端服务器地址', required: true},
            {key: 'mqtt_topic', label: 'mqtt_topic'},
            {key: 'attach', label: '附件', kind: 'upload', wide: true},
            {key: 'plant_map_id', label: '厂区四色图', kind: 'upload', required: true},
        ]
    },
    {
        title: '危险化学品生产登记信息', items: [
            {key: 'license_no', label: '许可证编号'},
            {key: 'license_begin', label: '许可证有效开始时间', kind: 'date'},
            {key: 'license_end', label: '许可证有效截至时间', kind: 'date'},
            {key: 'park_name', label: '化工园区名称'},
            {key: 'prod_start_date', label: '投产时间', kind: 'date'},
            {key: 'county', label: '所属县（市、区）'},
            {key: 'register_address', label: '注册地址'},
            {key: 'product_address', label: '生产地址'},
            {key: 'full_safety_count', label: '专职安全管理人员数', kind: 'number', readonly: true, noRule: true},
            {key: 'special_work_count', label: '特种作业人员数', kind: 'number'},
            {key: 'safety_std_level', label: '安全标准级别', kind: 'select', options: gd.safetyStdLevels},
            {key: 'std_cert_expire', label: '安全生产标准化证书有效期截止时间', kind: 'date'},
            {key: 'monitor_system', label: '是否接入危险化学品监测预警系统', kind: 'bool', required: true},
            {key: 'has_supervision_process', label: '是否有重点监管的危险化工工艺', kind: 'bool', required: true},
        ]
    },
]

const ENT_ALL = ENT_BLOCKS.flatMap(b => b.items)

/** 空表单：所有键先铺一遍，保证 el-form 的 prop 始终存在 */
function blankEnt() {
    const f = {}
    ENT_ALL.forEach(it => {
        if (it.kind === 'bool') f[it.key] = null
        else if (it.kind === 'number') f[it.key] = null
        else f[it.key] = null
    })
    return f
}

/** 必填规则由字段表派生，避免两处维护；noRule 项（同 key 只读复现）不参与校验 */
const ENT_RULES = ENT_ALL.filter(it => it.required && !it.noRule).reduce((acc, it) => {
    const action = it.kind === 'upload' ? '请上传'
        : ['select', 'date', 'cascader', 'bool', 'yesno'].includes(it.kind) ? '请选择' : '请输入'
    acc[it.key] = [{required: true, message: `${action}${it.label}`, trigger: it.kind === 'input' || it.kind === 'textarea' ? 'blur' : 'change'}]
    return acc
}, {})

const entLoading = ref(false)
const entSaving = ref(false)
const entFormRef = ref()
const entForm = ref(blankEnt())
const regionArr = ref([])
// 附件/四色图：后端暂无文件服务，只记文件名（不参与 ent/save，避免写坏列）
const attachName = ref('')
const mapName = ref('')

function loadEnt() {
    entLoading.value = true
    sys.biz('cbs/ent/get', {}, AC.signal, () => {}, (r, data) => {
        entLoading.value = false
        if (!r) {
            notifyError(data, '企业信息加载失败')
            return
        }
        // jackson NON_EMPTY 会吞空值：以 blankEnt 打底再覆盖，保证每个键都在
        const row = {...blankEnt(), ...(data.data || {})}
        // 行业分类 → 企业所属行业及类型 联动回填；历史脏值（不在枚举内）按行业分类归一
        if (!row.industry_detail || !gd.entIndustry.includes(row.industry_detail)) {
            row.industry_detail = row.industry_type || null
        }
        entForm.value = row
        regionArr.value = row.region_code ? String(row.region_code).split('/') : []
        attachName.value = ''
        mapName.value = row.plant_map_id || ''
    })
}

/** 附件/四色图：文件服务未接入，只记文件名（四色图落到 plant_map_id 以便必填校验） */
function onFileChange(key, file) {
    if (key === 'attach') {
        attachName.value = file.name
    } else {
        mapName.value = file.name
        entForm.value[key] = file.name
    }
}

function onRegionChange(v) {
    regionArr.value = v || []
    entForm.value.region_code = regionArr.value.join('/') || null
    // 目标站规则：行业分类 → 企业所属行业及类型 自动带出
    entForm.value.industry_detail = entForm.value.industry_type || null
}

function onIndustryChange() {
    entForm.value.industry_detail = entForm.value.industry_type || null
}

function saveEnt() {
    entFormRef.value.validate(valid => {
        if (!valid) {
            ElMessage.warning('请先补全带 * 的必填项')
            return
        }
        entSaving.value = true
        // 提交副本，剔除纯前端字段（附件无对应列）
        const payload = {...entForm.value}
        delete payload.attach
        sys.biz('cbs/ent/save', payload, AC.signal, () => {}, (r, data) => {
            entSaving.value = false
            if (r) ElMessage.success('保存成功')
            else notifyError(data, '保存失败')
        })
    })
}

/* ============ 企业简介 ============ */
const introLoading = ref(false)
const introSaving = ref(false)
const introText = ref('')

function loadIntro() {
    introLoading.value = true
    sys.biz('cbs/ent/get', {}, AC.signal, () => {}, (r, data) => {
        introLoading.value = false
        if (r) introText.value = data.data?.ent_intro || ''
        else notifyError(data, '企业简介加载失败')
    })
}

function saveIntro() {
    introSaving.value = true
    // 简介保存在同一行企业信息上：先取最新整行，只改 ent_intro 再整行提交
    sys.biz('cbs/ent/get', {}, AC.signal, () => {}, (r, data) => {
        if (!r) {
            introSaving.value = false
            notifyError(data, '保存失败')
            return
        }
        const row = {...(data.data || {}), ent_intro: introText.value || null}
        sys.biz('cbs/ent/save', row, AC.signal, () => {}, (r2, data2) => {
            introSaving.value = false
            if (r2) ElMessage.success('保存成功')
            else notifyError(data2, '保存失败')
        })
    })
}

/* ============ 通用子表逻辑（三同时/许可危化品/重点监管工艺/证照） ============ */
function makeCrud({url, key, idField, nameField, pagerKey, extra = () => ({})}) {
    // 每页独立分页配置（gd.json#pager.<pagerKey>）
    const pager = pagerOf(pagerKey)
    const loading = ref(false)
    const rows = ref([])
    const selected = ref([])
    const keyword = ref('')
    const pageNo = ref(1)
    const pageSize = ref(pager.pageSize)
    const total = ref(0)
    const dialogVisible = ref(false)
    const saving = ref(false)
    const formRef = ref()
    const form = ref({})

    function load() {
        AC.abort()
        AC = new AbortController()
        loading.value = true
        const paras = {pageNo: pageNo.value, pageSize: pageSize.value, ...extra()}
        const kw = keyword.value.trim()
        if (kw) paras.kw = kw
        sys.biz(`${url}/list`, paras, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                rows.value = data.data?.rows || []
                total.value = data.data?.total || 0
            } else {
                notifyError(data, '列表加载失败')
            }
        })
    }

    /** 回第一页重查（搜索/重置用） */
    function search() {
        pageNo.value = 1
        load()
    }

    watch([pageNo, pageSize], load)

    // 关键字防抖：停顿 300ms 才回第一页重查
    let kwTimer = null
    watch(keyword, () => {
        clearTimeout(kwTimer)
        kwTimer = setTimeout(search, 300)
    })

    function openDialog(edit, row, blank) {
        form.value = edit ? {...row} : {...blank}
        dialogVisible.value = true
    }

    function save() {
        formRef.value.validate(valid => {
            if (!valid) return
            saving.value = true
            sys.biz(`${url}/save`, form.value, AC.signal, () => {}, (r, data) => {
                saving.value = false
                if (r) {
                    ElMessage.success('保存成功')
                    dialogVisible.value = false
                    load()
                } else {
                    notifyError(data, '保存失败')
                }
            })
        })
    }

    function remove(row) {
        ElMessageBox.confirm(`确定删除「${row[nameField]}」吗？`, '提示', {type: 'warning'}).then(() => {
            loading.value = true
            sys.biz(`${url}/delete`, {[key]: row[idField]}, AC.signal, () => {}, (r, data) => {
                loading.value = false
                if (r) {
                    ElMessage.success('删除成功')
                    load()
                } else {
                    notifyError(data, '删除失败')
                }
            })
        }).catch(() => {})
    }

    /** 勾选批量删：ids 交后端 deleteBatch（软删口径同一处理，去空去重在后端） */
    function removeBatch() {
        const ids = selected.value.map(r => r[idField]).filter(Boolean)
        if (!ids.length) {
            ElMessage.warning('请先勾选要删除的记录')
            return
        }
        ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条记录吗？`, '提示', {type: 'warning'}).then(() => {
            loading.value = true
            sys.biz(`${url}/deleteBatch`, {ids}, AC.signal, () => {}, (r, data) => {
                loading.value = false
                if (r) {
                    ElMessage.success(`已删除 ${ids.length} 条`)
                    selected.value = []
                    load()
                } else {
                    notifyError(data, '批量删除失败')
                }
            })
        }).catch(() => {})
    }

    return {
        pager, loading, rows, selected, keyword, pageNo, pageSize, total,
        dialogVisible, saving, formRef, form, load, search, openDialog, save, remove,
        onSel: list => (selected.value = list || []), removeBatch,
    }
}

/* 三同时 */
const ts = makeCrud({url: 'cbs/ts', key: 'tsId', idField: 'ts_id', nameField: 'project_name', pagerKey: 'baseinfo.ts'})
const TS_BLANK = {project_name: '', project_type: '', approval_date: '', reply_date: '', progress: ''}
const tsRules = {project_name: [{required: true, message: '请输入项目名称', trigger: 'blur'}]}

/* 许可危化品 */
const chem = makeCrud({url: 'cbs/chem', key: 'chemId', idField: 'chem_id', nameField: 'chem_name', pagerKey: 'baseinfo.chem'})
const CHEM_BLANK = {
    chem_name: '', is_key: false, is_poison: false, is_major_hazard: false,
    storage: '', yearly: '', state: '', fire_class: '', catalog_no: '', cas_no: '',
    hazard_class: '', storage_place: '', critical_qty: '', actual_qty: '', longitude: '', remark: '',
}
const chemRules = {chem_name: [{required: true, message: '请输入化学品名称', trigger: 'blur'}]}

/* 重点监管工艺 */
const proc = makeCrud({url: 'cbs/proc', key: 'procId', idField: 'proc_id', nameField: 'proc_name', pagerKey: 'baseinfo.proc'})
const PROC_BLANK = {
    proc_name: '', proc_type: '', chem_involved: '', device_name: '',
    auto_in_use: false, esd_in_use: false, sis_in_use: false, hazop_done: false,
    risk_level: '', remark: '',
}
const procRules = {proc_name: [{required: true, message: '请输入工艺名称', trigger: 'blur'}]}

/* 证照预警：企业证书（cert_type=1）/ 企业人员证书（cert_type=2）两类各自一页、各自分页 */
const cert = makeCrud({
    url: 'cbs/cert', key: 'certId', idField: 'cert_id', nameField: 'cert_name',
    pagerKey: 'baseinfo.certEnt', extra: () => ({certType: 1}),
})
const CERT_BLANK = {
    cert_type: 1, cert_name: '', cert_no: '', issue_org: '', issue_date: '', expire_date: '', warn_days: 30,
}
const certRules = {
    cert_name: [{required: true, message: '请输入证照名称', trigger: 'blur'}],
    warn_days: [{required: true, message: '请输入预警天数', trigger: 'blur'}],
}

const certEmp = makeCrud({
    url: 'cbs/cert', key: 'certId', idField: 'cert_id', nameField: 'holder_name',
    pagerKey: 'baseinfo.certEmp', extra: () => ({certType: 2}),
})
const CERT_EMP_BLANK = {
    cert_type: 2, cert_name: '', holder_name: '', holder_idcard: '', cert_scope: '',
    cert_no: '', issue_org: '', issue_date: '', expire_date: '', warn_days: 30,
}
const certEmpRules = {
    cert_name: [{required: true, message: '请输入证书名称', trigger: 'blur'}],
    holder_name: [{required: true, message: '请输入持证人', trigger: 'blur'}],
    warn_days: [{required: true, message: '请输入预警天数', trigger: 'blur'}],
}

/** 证照预警状态：已到期 > 即将到期(≤warn_days) > 正常 */
function certStatus(row) {
    if (!row.expire_date) return {desc: '未设置到期日', type: 'info'}
    const days = Math.ceil((new Date(row.expire_date) - Date.now()) / 86400000)
    if (days < 0) return {desc: `已过期 ${-days} 天`, type: 'danger'}
    if (days <= (row.warn_days || 30)) return {desc: `${days} 天后到期`, type: 'warning'}
    return {desc: `剩余 ${days} 天`, type: 'success'}
}


/* ============ 区块懒加载 ============ */
const tabLoaded = ref({})

function ensureTab(t) {
    if (!TABS.includes(t) || tabLoaded.value[t]) return
    tabLoaded.value[t] = true
    if (t === 'ent') loadEnt()
    if (t === 'intro') loadIntro()
    if (t === 'ts') ts.load()
    if (t === 'chem') chem.load()
    if (t === 'proc') proc.load()
    if (t === 'cert') cert.load()
    if (t === 'certemp') certEmp.load()
}

watch(activeTab, ensureTab, {immediate: true})
onUnmounted(() => AC.abort())
</script>

<template>
    <div class="baseinfo-page">
        <div class="page-head">
            <h2>{{ pageTitle }}</h2>
        </div>

        <!-- ==================== 企业基本信息（两大块） ==================== -->
        <div v-if="activeTab === 'ent'" class="card" v-loading="entLoading">
            <el-form ref="entFormRef" :model="entForm" :rules="ENT_RULES" label-width="200px"
                     label-position="right" class="ent-form">
                <template v-for="b in ENT_BLOCKS" :key="b.title">
                    <div class="block-title">{{ b.title }}</div>
                    <el-row :gutter="24">
                        <el-col v-for="it in b.items" :key="b.title + it.key" :span="it.wide ? 24 : 12">
                            <el-form-item :label="it.label" :prop="it.noRule ? undefined : it.key">
                                <!-- 下拉 -->
                                <el-select v-if="it.kind === 'select'" v-model="entForm[it.key]" clearable
                                           :disabled="!!it.disabled" style="width:100%"
                                           @change="it.key === 'industry_type' && onIndustryChange()">
                                    <el-option v-for="o in it.options" :key="o" :value="o" :label="o"/>
                                </el-select>
                                <!-- 日期 -->
                                <el-date-picker v-else-if="it.kind === 'date'" v-model="entForm[it.key]"
                                                type="date" value-format="YYYY-MM-DD" style="width:100%"/>
                                <!-- 数字 -->
                                <el-input-number v-else-if="it.kind === 'number'" v-model="entForm[it.key]"
                                                 :controls="false" :disabled="!!it.readonly" style="width:100%"/>
                                <!-- 布尔是非 -->
                                <el-radio-group v-else-if="it.kind === 'bool'" v-model="entForm[it.key]">
                                    <el-radio :value="true">是</el-radio>
                                    <el-radio :value="false">否</el-radio>
                                </el-radio-group>
                                <!-- 字符串是非 -->
                                <el-radio-group v-else-if="it.kind === 'yesno'" v-model="entForm[it.key]">
                                    <el-radio v-for="o in gd.yesNo" :key="o" :value="o">{{ o }}</el-radio>
                                </el-radio-group>
                                <!-- 行政区划 -->
                                <el-cascader v-else-if="it.kind === 'cascader'" :model-value="regionArr"
                                             :options="gd.regions" clearable style="width:100%"
                                             @update:model-value="onRegionChange"/>
                                <!-- 多行文本 -->
                                <el-input v-else-if="it.kind === 'textarea'" v-model="entForm[it.key]"
                                          type="textarea" :rows="2"/>
                                <!-- 附件（文件服务未接入，仅本地记录文件名） -->
                                <el-upload v-else-if="it.kind === 'upload'" :auto-upload="false" :limit="1"
                                           :show-file-list="false"
                                           :on-change="f => onFileChange(it.key, f)">
                                    <el-button>选择文件</el-button>
                                    <span class="file-name">{{ it.key === 'attach' ? attachName : mapName }}</span>
                                    <div class="form-tip">文件服务未接入，选择后仅记录文件名</div>
                                </el-upload>
                                <!-- 普通输入 -->
                                <el-input v-else v-model="entForm[it.key]"/>
                                <div v-if="it.tip" class="form-tip">{{ it.tip }}</div>
                            </el-form-item>
                        </el-col>
                    </el-row>
                </template>
            </el-form>
            <div class="form-footer">
                <el-button type="primary" :loading="entSaving" @click="saveEnt">保存</el-button>
            </div>
        </div>

        <!-- ==================== 企业简介 ==================== -->
        <div v-if="activeTab === 'intro'" class="card" v-loading="introLoading">
            <el-input v-model="introText" type="textarea" :rows="14" placeholder="填写企业简介内容"/>
            <div class="form-footer">
                <el-button type="primary" :loading="introSaving" @click="saveIntro">保存简介</el-button>
            </div>
        </div>

        <!-- ==================== 安全三同时 ==================== -->
        <div v-if="activeTab === 'ts'" class="card">
            <div class="toolbar">
                <el-input v-model="ts.keyword.value" placeholder="搜索项目名称" clearable
                          style="width:220px" :prefix-icon="Search"/>
                <el-button type="primary" @click="ts.openDialog(false, null, TS_BLANK)">新增项目</el-button>
            </div>
            <el-table :data="ts.rows.value" v-loading="ts.loading.value" border stripe>
                <el-table-column prop="project_name" label="项目名称" min-width="200"/>
                <el-table-column prop="project_type" label="项目类型" width="140"/>
                <el-table-column prop="approval_date" label="立项时间" width="120"/>
                <el-table-column prop="reply_date" label="批复时间" width="120"/>
                <el-table-column prop="progress" label="项目进展" min-width="180" show-overflow-tooltip/>
                <el-table-column label="操作" width="130" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="ts.openDialog(true, row, TS_BLANK)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="ts.remove(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!ts.loading.value && ts.rows.value.length === 0" description="暂无三同时项目"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="ts.total.value" v-model:current-page="ts.pageNo.value"
                           v-model:page-size="ts.pageSize.value" :page-sizes="ts.pager.pageSizes"/>        </div>

        <!-- ==================== 许可的危险化学品 ==================== -->
        <div v-if="activeTab === 'chem'" class="card">
            <div class="toolbar">
                <el-input v-model="chem.keyword.value" placeholder="搜索化学品名称" clearable
                          style="width:220px" :prefix-icon="Search"/>
                <el-button type="primary" @click="chem.openDialog(false, null, CHEM_BLANK)">新增化学品</el-button>
                <el-button type="danger" plain :disabled="chem.selected.value.length === 0"
                           @click="chem.removeBatch()">批量删除</el-button>
            </div>
            <el-table :data="chem.rows.value" v-loading="chem.loading.value" border stripe
                      @selection-change="chem.onSel">
                <el-table-column type="selection" width="46" align="center" fixed="left"/>
                <el-table-column prop="chem_name" label="化学品名称" min-width="150"/>
                <el-table-column label="重点监管" width="90" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.is_key ? 'danger' : 'info'" effect="light" size="small">
                            {{ row.is_key ? '是' : '否' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="剧毒" width="80" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.is_poison ? 'danger' : 'info'" effect="light" size="small">
                            {{ row.is_poison ? '是' : '否' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="重大危险源" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.is_major_hazard ? 'warning' : 'info'" effect="light" size="small">
                            {{ row.is_major_hazard ? '是' : '否' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="storage" label="储存量" width="110"/>
                <el-table-column prop="yearly" label="年使用/产量" width="130"/>
                <el-table-column prop="state" label="物料状态" width="90"/>
                <el-table-column prop="fire_class" label="火灾类别" width="90"/>
                <el-table-column prop="cas_no" label="CAS号" width="120"/>
                <el-table-column prop="storage_place" label="储存场所" min-width="130" show-overflow-tooltip/>
                <el-table-column prop="hazard_class" label="危险性类别" min-width="150" show-overflow-tooltip/>
                <el-table-column label="操作" width="130" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="chem.openDialog(true, row, CHEM_BLANK)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="chem.remove(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!chem.loading.value && chem.rows.value.length === 0" description="暂无数据"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="chem.total.value" v-model:current-page="chem.pageNo.value"
                           v-model:page-size="chem.pageSize.value" :page-sizes="chem.pager.pageSizes"/>        </div>

        <!-- ==================== 重点监管的危险化工工艺 ==================== -->
        <div v-if="activeTab === 'proc'" class="card">
            <div class="toolbar">
                <el-input v-model="proc.keyword.value" placeholder="搜索工艺名称" clearable
                          style="width:220px" :prefix-icon="Search"/>
                <el-button type="primary" @click="proc.openDialog(false, null, PROC_BLANK)">新增工艺</el-button>
                <el-button type="danger" plain :disabled="proc.selected.value.length === 0"
                           @click="proc.removeBatch()">批量删除</el-button>
            </div>
            <el-table :data="proc.rows.value" v-loading="proc.loading.value" border stripe
                      @selection-change="proc.onSel">
                <el-table-column type="selection" width="46" align="center" fixed="left"/>
                <el-table-column prop="proc_name" label="工艺名称" min-width="200"/>
                <el-table-column prop="proc_type" label="反应类型" width="110"/>
                <el-table-column prop="device_name" label="装置/车间" min-width="140" show-overflow-tooltip/>
                <el-table-column prop="chem_involved" label="涉及危险化学品" min-width="170" show-overflow-tooltip/>
                <el-table-column label="自动化" width="90" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.auto_in_use ? 'success' : 'warning'" effect="light" size="small">
                            {{ row.auto_in_use ? '已投用' : '未投用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="紧急停车" width="95" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.esd_in_use ? 'success' : 'warning'" effect="light" size="small">
                            {{ row.esd_in_use ? '已投用' : '未投用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="SIS" width="90" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.sis_in_use ? 'success' : 'warning'" effect="light" size="small">
                            {{ row.sis_in_use ? '已投用' : '未投用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="HAZOP" width="85" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.hazop_done ? 'success' : 'info'" effect="light" size="small">
                            {{ row.hazop_done ? '已开展' : '未开展' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="risk_level" label="危险度" width="85" align="center"/>
                <el-table-column label="操作" width="130" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="proc.openDialog(true, row, PROC_BLANK)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="proc.remove(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!proc.loading.value && proc.rows.value.length === 0" description="暂无数据"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="proc.total.value" v-model:current-page="proc.pageNo.value"
                           v-model:page-size="proc.pageSize.value" :page-sizes="proc.pager.pageSizes"/>        </div>

        <!-- ==================== 证照预警 · 企业证书 ==================== -->
        <div v-if="activeTab === 'cert'" class="card">
            <div class="toolbar">
                <el-input v-model="cert.keyword.value" placeholder="搜索证照名称/编号" clearable
                          style="width:220px" :prefix-icon="Search"/>
                <el-button type="primary" @click="cert.openDialog(false, null, CERT_BLANK)">新增企业证书</el-button>
            </div>
            <el-table :data="cert.rows.value" v-loading="cert.loading.value" border stripe>
                <el-table-column prop="cert_name" label="证照名称" min-width="180"/>
                <el-table-column prop="cert_no" label="证照编号" min-width="150" show-overflow-tooltip/>
                <el-table-column prop="issue_org" label="发证机关" min-width="140"/>
                <el-table-column prop="issue_date" label="发证日期" width="110"/>
                <el-table-column prop="expire_date" label="到期日期" width="110"/>
                <el-table-column prop="warn_days" label="提前预警(天)" width="110" align="center"/>
                <el-table-column label="预警状态" width="130" align="center">
                    <template #default="{row}">
                        <el-tag :type="certStatus(row).type" effect="light">
                            {{ certStatus(row).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="130" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="cert.openDialog(true, row, CERT_BLANK)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="cert.remove(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!cert.loading.value && cert.rows.value.length === 0" description="暂无证照"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="cert.total.value" v-model:current-page="cert.pageNo.value"
                           v-model:page-size="cert.pageSize.value" :page-sizes="cert.pager.pageSizes"/>
        </div>

        <!-- ==================== 证照预警 · 企业人员证书 ==================== -->
        <div v-if="activeTab === 'certemp'" class="card">
            <div class="toolbar">
                <el-input v-model="certEmp.keyword.value" placeholder="搜索证书/持证人/编号" clearable
                          style="width:240px" :prefix-icon="Search"/>
                <el-button type="primary" @click="certEmp.openDialog(false, null, CERT_EMP_BLANK)">新增人员证书
                </el-button>
            </div>
            <el-table :data="certEmp.rows.value" v-loading="certEmp.loading.value" border stripe>
                <el-table-column prop="holder_name" label="持证人" width="95"/>
                <el-table-column prop="cert_name" label="证书名称" min-width="150" show-overflow-tooltip/>
                <el-table-column prop="cert_scope" label="准操项目/作业类别" min-width="140" show-overflow-tooltip/>
                <el-table-column prop="cert_no" label="证书编号" min-width="140" show-overflow-tooltip/>
                <el-table-column prop="issue_org" label="发证机关" min-width="110" show-overflow-tooltip/>
                <el-table-column prop="issue_date" label="发证日期" width="105"/>
                <el-table-column prop="expire_date" label="到期日期" width="105"/>
                <el-table-column prop="warn_days" label="提前预警(天)" width="112" align="center"/>
                <el-table-column label="预警状态" width="120" align="center">
                    <template #default="{row}">
                        <el-tag :type="certStatus(row).type" effect="light">
                            {{ certStatus(row).desc }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="130" align="center" fixed="right">
                    <template #default="{row}">
                        <el-link type="primary" @click="certEmp.openDialog(true, row, CERT_EMP_BLANK)">编辑</el-link>
                        <el-link type="danger" style="margin-left:10px" @click="certEmp.remove(row)">删除</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!certEmp.loading.value && certEmp.rows.value.length === 0" description="暂无人员证书"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="certEmp.total.value" v-model:current-page="certEmp.pageNo.value"
                           v-model:page-size="certEmp.pageSize.value" :page-sizes="certEmp.pager.pageSizes"/>
        </div>

        <!-- 三同时编辑弹窗 -->
        <el-dialog v-model="ts.dialogVisible.value" :title="ts.form.value.ts_id ? '编辑三同时项目' : '新增三同时项目'"
                   width="520px" destroy-on-close>
            <el-form :ref="el => (ts.formRef.value = el)" :model="ts.form.value" :rules="tsRules" label-width="100px">
                <el-form-item label="项目名称" prop="project_name">
                    <el-input v-model="ts.form.value.project_name"/>
                </el-form-item>
                <el-form-item label="项目类型">
                    <el-input v-model="ts.form.value.project_type"/>
                </el-form-item>
                <el-form-item label="立项时间">
                    <el-date-picker v-model="ts.form.value.approval_date" type="date"
                                    value-format="YYYY-MM-DD" style="width:100%"/>
                </el-form-item>
                <el-form-item label="批复时间">
                    <el-date-picker v-model="ts.form.value.reply_date" type="date"
                                    value-format="YYYY-MM-DD" style="width:100%"/>
                </el-form-item>
                <el-form-item label="项目进展">
                    <el-input v-model="ts.form.value.progress" type="textarea" :rows="3"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="ts.dialogVisible.value = false">取消</el-button>
                <el-button type="primary" :loading="ts.saving.value" @click="ts.save">保存</el-button>
            </template>
        </el-dialog>

        <!-- 许可危化品编辑弹窗 -->
        <el-dialog v-model="chem.dialogVisible.value" :title="chem.form.value.chem_id ? '编辑化学品' : '新增化学品'"
                   width="880px" destroy-on-close>
            <el-form :ref="el => (chem.formRef.value = el)" :model="chem.form.value" :rules="chemRules"
                     label-width="170px">
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="化学品名称" prop="chem_name">
                            <el-input v-model="chem.form.value.chem_name"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="CAS号">
                            <el-input v-model="chem.form.value.cas_no"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="危化品目录序号">
                            <el-input v-model="chem.form.value.catalog_no"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="危险性类别">
                            <el-input v-model="chem.form.value.hazard_class" placeholder="如 易燃液体 类别2"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="物料状态">
                            <el-select v-model="chem.form.value.state" clearable placeholder="请选择"
                                       style="width:100%">
                                <el-option v-for="o in gd.chemStates" :key="o" :value="o" :label="o"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="火灾危险类别">
                            <el-select v-model="chem.form.value.fire_class" clearable placeholder="请选择"
                                       style="width:100%">
                                <el-option v-for="o in gd.fireClasses" :key="o" :value="o" :label="o"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="储存量">
                            <el-input v-model="chem.form.value.storage"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="年使用/产量">
                            <el-input v-model="chem.form.value.yearly"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="储存场所">
                            <el-input v-model="chem.form.value.storage_place" placeholder="罐区/仓库名称"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="经纬度">
                            <el-input v-model="chem.form.value.longitude"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="重大危险源临界量(吨)">
                            <el-input v-model="chem.form.value.critical_qty"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="实际存在量(吨)">
                            <el-input v-model="chem.form.value.actual_qty"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="重点监管危化品">
                            <el-switch v-model="chem.form.value.is_key"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="剧毒化学品">
                            <el-switch v-model="chem.form.value.is_poison"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="8">
                        <el-form-item label="构成重大危险源">
                            <el-switch v-model="chem.form.value.is_major_hazard"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="备注">
                            <el-input v-model="chem.form.value.remark" type="textarea" :rows="2"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="chem.dialogVisible.value = false">取消</el-button>
                <el-button type="primary" :loading="chem.saving.value" @click="chem.save">保存</el-button>
            </template>
        </el-dialog>

        <!-- 重点监管工艺编辑弹窗 -->
        <el-dialog v-model="proc.dialogVisible.value" :title="proc.form.value.proc_id ? '编辑工艺' : '新增工艺'"
                   width="760px" destroy-on-close>
            <el-form :ref="el => (proc.formRef.value = el)" :model="proc.form.value" :rules="procRules"
                     label-width="170px">
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="工艺名称" prop="proc_name">
                            <el-select v-model="proc.form.value.proc_name" filterable allow-create clearable
                                       default-first-option placeholder="选择或输入工艺名称" style="width:100%">
                                <el-option v-for="o in gd.hazardProcs" :key="o" :value="o" :label="o"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="反应类型">
                            <el-select v-model="proc.form.value.proc_type" filterable clearable
                                       placeholder="请选择" style="width:100%">
                                <el-option v-for="o in gd.procTypes" :key="o" :value="o" :label="o"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="工艺装置/所在车间">
                            <el-input v-model="proc.form.value.device_name"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="反应工艺危险度等级">
                            <el-select v-model="proc.form.value.risk_level" clearable placeholder="请选择"
                                       style="width:100%">
                                <el-option v-for="o in gd.riskLevels" :key="o" :value="o" :label="o"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="涉及危险化学品">
                            <el-input v-model="proc.form.value.chem_involved" placeholder="多个以、分隔"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="自动化控制系统">
                            <el-switch v-model="proc.form.value.auto_in_use" active-text="已投用"
                                       inactive-text="未投用"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="紧急停车系统">
                            <el-switch v-model="proc.form.value.esd_in_use" active-text="已投用"
                                       inactive-text="未投用"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="安全仪表系统(SIS)">
                            <el-switch v-model="proc.form.value.sis_in_use" active-text="已投用"
                                       inactive-text="未投用"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="已开展HAZOP分析">
                            <el-switch v-model="proc.form.value.hazop_done" active-text="是" inactive-text="否"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="备注">
                            <el-input v-model="proc.form.value.remark" type="textarea" :rows="2"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="proc.dialogVisible.value = false">取消</el-button>
                <el-button type="primary" :loading="proc.saving.value" @click="proc.save">保存</el-button>
            </template>
        </el-dialog>

        <!-- 企业证书编辑弹窗 -->
        <el-dialog v-model="cert.dialogVisible.value" :title="cert.form.value.cert_id ? '编辑企业证书' : '新增企业证书'"
                   width="520px" destroy-on-close>
            <el-form :ref="el => (cert.formRef.value = el)" :model="cert.form.value" :rules="certRules" label-width="110px">
                <el-form-item label="证照名称" prop="cert_name">
                    <el-input v-model="cert.form.value.cert_name"/>
                </el-form-item>
                <el-form-item label="证照编号">
                    <el-input v-model="cert.form.value.cert_no"/>
                </el-form-item>
                <el-form-item label="发证机关">
                    <el-input v-model="cert.form.value.issue_org"/>
                </el-form-item>
                <el-form-item label="发证日期">
                    <el-date-picker v-model="cert.form.value.issue_date" type="date"
                                    value-format="YYYY-MM-DD" style="width:100%"/>
                </el-form-item>
                <el-form-item label="到期日期">
                    <el-date-picker v-model="cert.form.value.expire_date" type="date"
                                    value-format="YYYY-MM-DD" style="width:100%"/>
                </el-form-item>
                <el-form-item label="提前预警(天)" prop="warn_days">
                    <el-input-number v-model="cert.form.value.warn_days" :min="1" :precision="0" :controls="false"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="cert.dialogVisible.value = false">取消</el-button>
                <el-button type="primary" :loading="cert.saving.value" @click="cert.save">保存</el-button>
            </template>
        </el-dialog>

        <!-- 企业人员证书编辑弹窗 -->
        <el-dialog v-model="certEmp.dialogVisible.value"
                   :title="certEmp.form.value.cert_id ? '编辑人员证书' : '新增人员证书'"
                   width="640px" destroy-on-close>
            <el-form :ref="el => (certEmp.formRef.value = el)" :model="certEmp.form.value" :rules="certEmpRules"
                     label-width="130px">
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="持证人" prop="holder_name">
                            <el-input v-model="certEmp.form.value.holder_name"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="身份证号">
                            <el-input v-model="certEmp.form.value.holder_idcard"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="证书名称" prop="cert_name">
                            <el-input v-model="certEmp.form.value.cert_name" placeholder="如 特种作业操作证"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="准操项目/作业类别">
                            <el-input v-model="certEmp.form.value.cert_scope"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="证书编号">
                            <el-input v-model="certEmp.form.value.cert_no"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="发证机关">
                            <el-input v-model="certEmp.form.value.issue_org"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="发证日期">
                            <el-date-picker v-model="certEmp.form.value.issue_date" type="date"
                                            value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="到期日期">
                            <el-date-picker v-model="certEmp.form.value.expire_date" type="date"
                                            value-format="YYYY-MM-DD" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="提前预警(天)" prop="warn_days">
                            <el-input-number v-model="certEmp.form.value.warn_days" :min="1" :precision="0"
                                             :controls="false" style="width:100%"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <template #footer>
                <el-button @click="certEmp.dialogVisible.value = false">取消</el-button>
                <el-button type="primary" :loading="certEmp.saving.value" @click="certEmp.save">保存</el-button>
            </template>
        </el-dialog>

    </div>
</template>

<style lang="scss" scoped>
.baseinfo-page {
    .page-head {
        margin: 4px 0 18px;
        text-align: center;

        h2 {
            margin: 0;
            font-size: 22px;
            font-weight: 700;
            color: #1e293b;
            letter-spacing: 2px;
        }
    }

    .card {
        background: #ffffff;
        border-radius: 10px;
        padding: 16px 20px 20px;
        border: 1px solid #e2e8f0;
    }

    .block-title {
        margin: 6px 0 18px;
        padding-left: 10px;
        border-left: 3px solid #17c0ad;
        font-size: 15px;
        font-weight: 600;
        color: #1e293b;
        line-height: 1.2;

        &:not(:first-child) {
            margin-top: 26px;
        }
    }

    .ent-form {
        :deep(.el-form-item__label) {
            white-space: normal;
            line-height: 1.4;
            word-break: break-all;
        }

        :deep(.el-form-item) {
            margin-bottom: 18px;
        }
    }

    .toolbar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 14px;
    }

    .pager {
        margin-top: 14px;
        justify-content: flex-end;
    }

    .form-footer {
        margin-top: 18px;
        text-align: center;
    }

    .form-tip {
        font-size: 11px;
        color: #94a3b8;
        line-height: 1.5;
        margin-top: 2px;
    }

    .file-name {
        margin-left: 10px;
        font-size: 12px;
        color: #64748b;
    }
}
</style>
