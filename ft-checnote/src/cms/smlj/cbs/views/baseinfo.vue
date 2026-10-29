<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

const activeTab = ref('ent')
let AC = new AbortController()

/* ============ Tab1 企业信息 ============ */
const entLoading = ref(false)
const entSaving = ref(false)
const entFormRef = ref()
const entForm = ref({})

// 企业信息表单分区（字段名 = cbs.t_enterprise 列）
const ENT_GROUPS = [
    {title: '基本信息', items: [
        {key: 'ent_name', label: '企业名称'},
        {key: 'uscc', label: '统一社会信用代码'},
        {key: 'legal_person', label: '法定代表人'},
        {key: 'legal_phone', label: '法人电话'},
        {key: 'legal_idcard', label: '法人身份证号'},
        {key: 'chem_reg_code', label: '危化品登记系统编码'},
        {key: 'found_date', label: '成立时间', type: 'date'},
        {key: 'industry_type', label: '行业分类'},
        {key: 'supervisor', label: '主管部门'},
        {key: 'staff_count', label: '职工人数', type: 'number'},
        {key: 'region_code', label: '行政区划'},
        {key: 'county', label: '所属县(市、区)'},
        {key: 'address', label: '单位地址', wide: true},
        {key: 'register_address', label: '注册地址', wide: true},
        {key: 'product_address', label: '生产地址', wide: true},
        {key: 'park_name', label: '化工园区名称'},
        {key: 'fixed_assets', label: '固定资产(万元)', type: 'number'},
        {key: 'total_area', label: '总建筑面积(㎡)', type: 'number'},
        {key: 'prod_start_date', label: '投产时间', type: 'date'},
    ]},
    {title: '经营与规模', items: [
        {key: 'industry_detail', label: '所属行业及类型', wide: true},
        {key: 'sub_units', label: '下级单位', wide: true},
        {key: 'economy_type', label: '经济类型'},
        {key: 'run_status', label: '企业经营状态'},
        {key: 'supervise_level', label: '监管层级'},
        {key: 'capacity', label: '企业生产能力'},
        {key: 'ent_scale', label: '企业规模'},
        {key: 'ent_position', label: '企业定位'},
        {key: 'two_key_one_major', label: '两重点一重大作业', type: 'select'},
        {key: 'license_no', label: '许可证编号'},
        {key: 'license_begin', label: '许可开始日期', type: 'date'},
        {key: 'license_end', label: '许可截止日期', type: 'date'},
    ]},
    {title: '安全管理', items: [
        {key: 'main_person', label: '企业主要负责人'},
        {key: 'main_person_idcard', label: '负责人身份证号'},
        {key: 'safety_org', label: '安全管理机构'},
        {key: 'safety_org_phone', label: '安全机构电话'},
        {key: 'safety_org_leader', label: '安全机构负责人'},
        {key: 'safety_org_leader_phone', label: '负责人电话'},
        {key: 'duty_phone', label: '值班电话'},
        {key: 'full_safety_count', label: '专职安全员数', type: 'number'},
        {key: 'part_safety_count', label: '兼职安全员数', type: 'number'},
        {key: 'special_work_count', label: '特种作业人员数', type: 'number'},
        {key: 'safety_std_level', label: '安全标准级别'},
        {key: 'std_cert_expire', label: '标准化证书到期', type: 'date'},
    ]},
    {title: '危化与风险', items: [
        {key: 'has_major_hazard', label: '有危化品重大危险源', type: 'switch'},
        {key: 'risk_identified', label: '已进行风险辨识', type: 'switch'},
        {key: 'hazard_filed', label: '重大危险源备案', type: 'select'},
        {key: 'has_danger_process', label: '涉及危险工艺', type: 'select'},
        {key: 'monitor_system', label: '接入监测预警系统', type: 'switch'},
        {key: 'has_supervision_process', label: '有重点监管化工工艺', type: 'switch'},
    ]},
    {title: '系统接入', items: [
        {key: 'server_addr', label: '企业端服务器地址', wide: true},
        {key: 'mqtt_topic', label: 'MQTT Topic'},
        {key: 'plant_map_id', label: '厂区四色图(file引用)'},
    ]},
]

function loadEnt() {
    entLoading.value = true
    sys.biz('cbs/ent/get', {}, AC.signal, () => {}, (r, data) => {
        entLoading.value = false
        if (r) entForm.value = data.data || {}
        else notifyError(data, '企业信息加载失败')
    })
}

function saveEnt() {
    entSaving.value = true
    sys.biz('cbs/ent/save', entForm.value, AC.signal, () => {}, (r, data) => {
        entSaving.value = false
        if (r) ElMessage.success('保存成功')
        else notifyError(data, '保存失败')
    })
}

/* ============ Tab2 企业简介 ============ */
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
function makeCrud({url, key, idField, nameField}) {
    const loading = ref(false)
    const rows = ref([])
    const keyword = ref('')
    const dialogVisible = ref(false)
    const saving = ref(false)
    const formRef = ref()
    const form = ref({})

    function load() {
        AC.abort()
        AC = new AbortController()
        loading.value = true
        const paras = {}
        const kw = keyword.value.trim()
        if (kw) paras.kw = kw
        sys.biz(`${url}/list`, paras, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) rows.value = data.data || []
            else notifyError(data, '列表加载失败')
        })
    }

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

    return {loading, rows, keyword, dialogVisible, saving, formRef, form, load, openDialog, save, remove}
}

/* 三同时 */
const ts = makeCrud({url: 'cbs/ts', key: 'tsId', idField: 'ts_id', nameField: 'project_name'})
const TS_BLANK = {project_name: '', project_type: '', approval_date: '', reply_date: '', progress: ''}
const tsRules = {project_name: [{required: true, message: '请输入项目名称', trigger: 'blur'}]}

/* 许可危化品 */
const chem = makeCrud({url: 'cbs/chem', key: 'chemId', idField: 'chem_id', nameField: 'chem_name'})
const CHEM_BLANK = {
    chem_name: '', is_key: false, storage: '', yearly: '', state: '',
    fire_class: '', catalog_no: '', cas_no: '', longitude: '',
}
const chemRules = {chem_name: [{required: true, message: '请输入化学品名称', trigger: 'blur'}]}

/* 重点监管工艺 */
const proc = makeCrud({url: 'cbs/proc', key: 'procId', idField: 'proc_id', nameField: 'proc_name'})
const PROC_BLANK = {proc_name: '', auto_in_use: false}
const procRules = {proc_name: [{required: true, message: '请输入工艺名称', trigger: 'blur'}]}

/* 证照预警 */
const cert = makeCrud({url: 'cbs/cert', key: 'certId', idField: 'cert_id', nameField: 'cert_name'})
const CERT_BLANK = {cert_name: '', cert_no: '', issue_org: '', issue_date: '', expire_date: '', warn_days: 30}
const certRules = {
    cert_name: [{required: true, message: '请输入证照名称', trigger: 'blur'}],
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

/* ============ Tab7 类型字典（承包商类型/处罚类型/项目类型） ============ */
const dictLoading = ref(false)
const dictRows = ref([])
const dictType = ref('contractor_type')
const dictDialogVisible = ref(false)
const dictSaving = ref(false)
const dictFormRef = ref()
const dictForm = ref({})
const DICT_BLANK = {item_id: '', dict_type: 'contractor_type', item_name: '', sort_no: 0, is_using: true}

function loadDicts() {
    AC.abort()
    AC = new AbortController()
    dictLoading.value = true
    sys.biz('cbs/dict/list', {dictType: dictType.value}, AC.signal, () => {}, (r, data) => {
        dictLoading.value = false
        if (r) dictRows.value = data.data || []
        else notifyError(data, '字典加载失败')
    })
}

watch(dictType, loadDicts)

function openDictDialog(edit, row) {
    dictForm.value = edit
        ? {
            item_id: row.item_id, dict_type: row.dict_type, item_name: row.item_name,
            sort_no: row.sort_no ?? 0, is_using: row.is_using !== false,
        }
        : {...DICT_BLANK, dict_type: dictType.value}
    dictDialogVisible.value = true
}

function saveDict() {
    dictFormRef.value.validate(valid => {
        if (!valid) return
        dictSaving.value = true
        sys.biz('cbs/dict/save', dictForm.value, AC.signal, () => {}, (r, data) => {
            dictSaving.value = false
            if (r) {
                ElMessage.success('保存成功')
                dictDialogVisible.value = false
                loadDicts()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

function removeDict(row) {
    ElMessageBox.confirm(`确定删除字典项「${row.item_name}」吗？`, '提示', {type: 'warning'}).then(() => {
        dictLoading.value = true
        sys.biz('cbs/dict/delete', {itemId: row.item_id}, AC.signal, () => {}, (r, data) => {
            dictLoading.value = false
            if (r) {
                ElMessage.success('删除成功')
                loadDicts()
            } else {
                notifyError(data, '删除失败')
            }
        })
    }).catch(() => {})
}

/* ============ Tab 懒加载 + 生命周期 ============ */
const tabLoaded = ref({ent: false})

watch(activeTab, t => {
    if (tabLoaded.value[t]) return
    tabLoaded.value[t] = true
    if (t === 'intro') loadIntro()
    if (t === 'ts') ts.load()
    if (t === 'chem') chem.load()
    if (t === 'proc') proc.load()
    if (t === 'cert') cert.load()
    if (t === 'dict') loadDicts()
})

onMounted(loadEnt)
onUnmounted(() => AC.abort())
</script>

<template>
    <div class="baseinfo-page">
        <el-tabs v-model="activeTab">
            <!-- ==================== 企业信息 ==================== -->
            <el-tab-pane label="企业信息" name="ent">
                <div class="card" v-loading="entLoading">
                    <template v-for="g in ENT_GROUPS" :key="g.title">
                        <el-divider content-position="left">{{ g.title }}</el-divider>
                        <el-form ref="entFormRef" :model="entForm" label-width="160px">
                            <el-row :gutter="16">
                                <el-col v-for="it in g.items" :key="it.key" :span="it.wide ? 24 : 8">
                                    <el-form-item :label="it.label">
                                        <el-input-number v-if="it.type === 'number'"
                                                         v-model="entForm[it.key]" :controls="false"
                                                         style="width:100%"/>
                                        <el-date-picker v-else-if="it.type === 'date'"
                                                        v-model="entForm[it.key]" type="date"
                                                        value-format="YYYY-MM-DD" style="width:100%"/>
                                        <el-select v-else-if="it.type === 'select'"
                                                   v-model="entForm[it.key]" clearable style="width:100%">
                                            <el-option v-for="o in gd.yesNoNa" :key="o" :value="o" :label="o"/>
                                        </el-select>
                                        <el-switch v-else-if="it.type === 'switch'" v-model="entForm[it.key]"/>
                                        <el-input v-else v-model="entForm[it.key]"/>
                                    </el-form-item>
                                </el-col>
                            </el-row>
                        </el-form>
                    </template>
                    <div class="form-footer">
                        <el-button type="primary" :loading="entSaving" @click="saveEnt">保存企业信息</el-button>
                    </div>
                </div>
            </el-tab-pane>

            <!-- ==================== 企业简介 ==================== -->
            <el-tab-pane label="企业简介" name="intro">
                <div class="card" v-loading="introLoading">
                    <el-input v-model="introText" type="textarea" :rows="14"
                              placeholder="填写企业简介内容"/>
                    <div class="form-footer">
                        <el-button type="primary" :loading="introSaving" @click="saveIntro">保存简介</el-button>
                    </div>
                </div>
            </el-tab-pane>

            <!-- ==================== 安全三同时 ==================== -->
            <el-tab-pane label="安全三同时" name="ts">
                <div class="card">
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
                </div>
            </el-tab-pane>

            <!-- ==================== 许可的危险化学品 ==================== -->
            <el-tab-pane label="许可危化品" name="chem">
                <div class="card">
                    <div class="toolbar">
                        <el-input v-model="chem.keyword.value" placeholder="搜索化学品名称" clearable
                                  style="width:220px" :prefix-icon="Search"/>
                        <el-button type="primary" @click="chem.openDialog(false, null, CHEM_BLANK)">新增化学品
                        </el-button>
                    </div>
                    <el-table :data="chem.rows.value" v-loading="chem.loading.value" border stripe>
                        <el-table-column prop="chem_name" label="化学品名称" min-width="150"/>
                        <el-table-column label="重点监管" width="90" align="center">
                            <template #default="{row}">
                                <el-tag :type="row.is_key ? 'danger' : 'info'" effect="light" size="small">
                                    {{ row.is_key ? '是' : '否' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="storage" label="储存量" width="120"/>
                        <el-table-column prop="yearly" label="年使用/产量" width="130"/>
                        <el-table-column prop="state" label="物料状态" width="90"/>
                        <el-table-column prop="fire_class" label="火灾类别" width="90"/>
                        <el-table-column prop="catalog_no" label="目录序号" width="90"/>
                        <el-table-column prop="cas_no" label="CAS号" width="130"/>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="chem.openDialog(true, row, CHEM_BLANK)">编辑</el-link>
                                <el-link type="danger" style="margin-left:10px" @click="chem.remove(row)">删除</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!chem.loading.value && chem.rows.value.length === 0" description="暂无数据"/>
                </div>
            </el-tab-pane>

            <!-- ==================== 重点监管的危险化工工艺 ==================== -->
            <el-tab-pane label="重点监管工艺" name="proc">
                <div class="card">
                    <div class="toolbar">
                        <el-input v-model="proc.keyword.value" placeholder="搜索工艺名称" clearable
                                  style="width:220px" :prefix-icon="Search"/>
                        <el-button type="primary" @click="proc.openDialog(false, null, PROC_BLANK)">新增工艺</el-button>
                    </div>
                    <el-table :data="proc.rows.value" v-loading="proc.loading.value" border stripe>
                        <el-table-column prop="proc_name" label="工艺名称" min-width="220"/>
                        <el-table-column label="自动化/紧急停车系统投用" width="200" align="center">
                            <template #default="{row}">
                                <el-tag :type="row.auto_in_use ? 'success' : 'warning'" effect="light" size="small">
                                    {{ row.auto_in_use ? '已投用' : '未投用' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="proc.openDialog(true, row, PROC_BLANK)">编辑</el-link>
                                <el-link type="danger" style="margin-left:10px" @click="proc.remove(row)">删除</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!proc.loading.value && proc.rows.value.length === 0" description="暂无数据"/>
                </div>
            </el-tab-pane>

            <!-- ==================== 证照预警 ==================== -->
            <el-tab-pane label="证照预警" name="cert">
                <div class="card">
                    <div class="toolbar">
                        <el-input v-model="cert.keyword.value" placeholder="搜索证照名称/编号" clearable
                                  style="width:220px" :prefix-icon="Search"/>
                        <el-button type="primary" @click="cert.openDialog(false, null, CERT_BLANK)">新增证照</el-button>
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
                </div>
            </el-tab-pane>

            <!-- ==================== 类型字典 ==================== -->
            <el-tab-pane label="类型字典" name="dict">
                <div class="card">
                    <div class="toolbar">
                        <el-radio-group v-model="dictType">
                            <el-radio-button v-for="t in gd.dictTypes" :key="t.id" :value="t.id">
                                {{ t.desc }}
                            </el-radio-button>
                        </el-radio-group>
                        <el-button type="primary" @click="openDictDialog(false)">新增字典项</el-button>
                    </div>
                    <el-table :data="dictRows" v-loading="dictLoading" border stripe>
                        <el-table-column prop="item_name" label="名称" min-width="200"/>
                        <el-table-column prop="sort_no" label="排序" width="90" align="center"/>
                        <el-table-column label="状态" width="100" align="center">
                            <template #default="{row}">
                                <el-tag :type="row.is_using ? 'success' : 'info'" effect="light" size="small">
                                    {{ row.is_using ? '启用' : '停用' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="openDictDialog(true, row)">编辑</el-link>
                                <el-link type="danger" style="margin-left:10px" @click="removeDict(row)">删除</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!dictLoading && dictRows.length === 0" description="暂无字典项"/>
                </div>
            </el-tab-pane>
        </el-tabs>

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
                   width="600px" destroy-on-close>
            <el-form :ref="el => (chem.formRef.value = el)" :model="chem.form.value" :rules="chemRules" label-width="140px">
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="化学品名称" prop="chem_name">
                            <el-input v-model="chem.form.value.chem_name"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="重点监管危化品">
                            <el-switch v-model="chem.form.value.is_key"/>
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
                        <el-form-item label="物料状态">
                            <el-input v-model="chem.form.value.state"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="火灾危险类别">
                            <el-input v-model="chem.form.value.fire_class"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="危化品目录序号">
                            <el-input v-model="chem.form.value.catalog_no"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="CAS号">
                            <el-input v-model="chem.form.value.cas_no"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="经纬度">
                            <el-input v-model="chem.form.value.longitude"/>
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
                   width="460px" destroy-on-close>
            <el-form :ref="el => (proc.formRef.value = el)" :model="proc.form.value" :rules="procRules" label-width="120px">
                <el-form-item label="工艺名称" prop="proc_name">
                    <el-input v-model="proc.form.value.proc_name"/>
                </el-form-item>
                <el-form-item label="自动化投用">
                    <el-switch v-model="proc.form.value.auto_in_use"/>
                    <div class="form-tip">自动化控制系统、紧急停车系统是否投入使用</div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="proc.dialogVisible.value = false">取消</el-button>
                <el-button type="primary" :loading="proc.saving.value" @click="proc.save">保存</el-button>
            </template>
        </el-dialog>

        <!-- 证照编辑弹窗 -->
        <el-dialog v-model="cert.dialogVisible.value" :title="cert.form.value.cert_id ? '编辑证照' : '新增证照'"
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

        <!-- 类型字典编辑弹窗 -->
        <el-dialog v-model="dictDialogVisible" :title="dictForm.item_id ? '编辑字典项' : '新增字典项'"
                   width="460px" destroy-on-close>
            <el-form ref="dictFormRef" :model="dictForm" label-width="90px"
                     :rules="{item_name: [{required: true, message: '请输入名称', trigger: 'blur'}]}">
                <el-form-item label="所属类型">
                    <el-select v-model="dictForm.dict_type" style="width:100%">
                        <el-option v-for="t in gd.dictTypes" :key="t.id" :value="t.id" :label="t.desc"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="名称" prop="item_name">
                    <el-input v-model="dictForm.item_name" placeholder="字典项名称"/>
                </el-form-item>
                <el-form-item label="排序">
                    <el-input-number v-model="dictForm.sort_no" :min="0" :precision="0" :controls="false"/>
                    <span class="form-tip" style="margin-left:8px">数值越小越靠前</span>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="dictForm.is_using" active-text="启用"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dictDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="dictSaving" @click="saveDict">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.baseinfo-page {
    .card {
        background: #ffffff;
        border-radius: 10px;
        padding: 16px 20px 20px;
        border: 1px solid #e2e8f0;
    }

    .toolbar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 14px;
    }

    .form-footer {
        margin-top: 8px;
        text-align: right;
    }

    .form-tip {
        font-size: 11px;
        color: #94a3b8;
        line-height: 1.5;
        margin-top: 4px;
    }
}
</style>
