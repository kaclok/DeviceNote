<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {invalidateDict} from "../system/DictX.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* 一个菜单 = 一个字典类型（承包商类型 / 承包商处罚类型 / 项目类型），由路由 :type 决定 */
const TYPES = gd.dictTypes.map(t => t.id)
const props = defineProps({type: String})
const activeType = ref(TYPES.includes(props?.type) ? props.type : TYPES[0])
const pageTitle = computed(() => (gd.dictTypes.find(t => t.id === activeType.value) || {}).desc || '')

/* 三类字典统一支持 在用/回收站：页内右侧水平两个切页（回收站 = 软删行 open_status=false） */
const recycled = ref(false)

/* 项目类型：行内维护该类型下的「开工准备条件 / 资料清单」（仿资质管理弹窗，不是独立页面） */
const isProjectType = computed(() => activeType.value === 'project_type')

/* 承包商类型：行内维护 资质管理 / 审核所需文件 */
const isContractorType = computed(() => activeType.value === 'contractor_type')

/* 操作列宽度按入口数量动态给，避免链接被挤换行；回收站只有 恢复/彻底删除 两个入口 */
const opWidth = computed(() => {
    if (recycled.value) return 170
    if (isContractorType.value) return 330
    if (isProjectType.value) return 300
    return 120
})

// 每页独立分页配置：dict.<类型>
const pager = computed(() => pagerOf('dict.' + activeType.value))

const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const pageNo = ref(1)
const pageSize = ref(pager.value.pageSize)
const total = ref(0)
let kwTimer = null

let AC = new AbortController()

function loadRows() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    const paras = {dictType: activeType.value, pageNo: pageNo.value, pageSize: pageSize.value}
    const kw = keyword.value.trim()
    if (kw) paras.kw = kw
    paras.recycled = recycled.value
    sys.biz('cbs/dict/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            rows.value = data.data?.rows || []
            total.value = data.data?.total || 0
        } else {
            notifyError(data, '字典加载失败')
        }
    })
}

watch(keyword, () => {
    clearTimeout(kwTimer)
    kwTimer = setTimeout(() => {
        pageNo.value = 1
        loadRows()
    }, 300)
})
watch([pageNo, pageSize], loadRows)
watch(recycled, () => {
    pageNo.value = 1
    loadRows()
})

// 切换类型（同一组件实例换路由参数）：重置到第一页并换成该页自己的每页条数
watch(() => props.type, t => {
    if (!TYPES.includes(t) || t === activeType.value) return
    activeType.value = t
    recycled.value = false
    keyword.value = ''
    pageNo.value = 1
    pageSize.value = pager.value.pageSize
    loadRows()
})

/* ============ 新增/编辑 ============ */
const dialogVisible = ref(false)
const saving = ref(false)
const formRef = ref()
const form = ref({})
const rules = {item_name: [{required: true, message: '请输入名称', trigger: 'blur'}]}

function openDialog(edit, row) {
    form.value = edit
        ? {
            item_id: row.item_id, dict_type: row.dict_type, item_name: row.item_name,
            is_using: row.is_using !== false,
        }
        : {item_id: '', dict_type: activeType.value, item_name: '', is_using: true}
    dialogVisible.value = true
}

function saveForm() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        const f = {...form.value, dict_type: activeType.value}
        sys.biz('cbs/dict/save', f, AC.signal, () => {}, (r, data) => {
            saving.value = false
            if (r) {
                ElMessage.success('保存成功')
                dialogVisible.value = false
                invalidateDict(activeType.value)   // 业务页下拉的缓存一并失效
                loadRows()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

/* ============ 删除 / 恢复 / 彻底删除 ============ */
/* 在用页删除 = 移入回收站（软删）；「彻底删除」只在回收站页出现 */
function removeRow(row) {
    ElMessageBox.confirm(`确定将「${row.item_name}」移入回收站吗？`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/dict/delete', {itemId: row.item_id}, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                ElMessage.success('已移入回收站')
                invalidateDict(activeType.value)
                loadRows()
            } else notifyError(data, '操作失败')
        })
    }).catch(() => {})
}

function restoreRow(row) {
    loading.value = true
    sys.biz('cbs/dict/restore', {itemId: row.item_id}, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            ElMessage.success('已恢复')
            invalidateDict(activeType.value)
            loadRows()
        } else notifyError(data, '恢复失败')
    })
}

function purgeRow(row) {
    ElMessageBox.confirm(`确定彻底删除「${row.item_name}」吗？此操作不可恢复。`, '提示', {type: 'warning'}).then(() => {
        loading.value = true
        sys.biz('cbs/dict/purge', {itemId: row.item_id}, AC.signal, () => {}, (r, data) => {
            loading.value = false
            if (r) {
                ElMessage.success('已彻底删除')
                invalidateDict(activeType.value)
                loadRows()
            } else notifyError(data, '删除失败')
        })
    }).catch(() => {})
}

/* ============ 资质管理 / 审核所需文件（仅承包商类型；同一弹窗，qualType 区分） ============
 * 仿目标站 contractorType：两个入口共用一个弹窗、一张表，1=资质管理 2=审核所需文件 */
const QUAL_QUAL = 1
const QUAL_FILE = 2
const qualVisible = ref(false)
const qualLoading = ref(false)
const qualType = ref(QUAL_QUAL)
const qualTypeId = ref('')
const qualTypeName = ref('')
const qualRows = ref([])
const QUAL_MAX = 200
const emptyQualForm = () => ({qual_id: '', qual_name: '', qual_level: '', validity: 0, remind: null, is_using: true})
const qualForm = ref(emptyQualForm())
const qualIsFile = computed(() => qualType.value === QUAL_FILE)
const qualNameLabel = computed(() => (qualIsFile.value ? '文件名称' : '资质名称'))
const qualTitle = computed(() => `${qualIsFile.value ? '审核所需文件' : '资质管理'} — ${qualTypeName.value}`)
const qualEditing = computed(() => !!qualForm.value.qual_id)

function openQual(row, type) {
    qualType.value = type
    qualTypeId.value = row.item_id
    qualTypeName.value = row.item_name
    qualForm.value = emptyQualForm()
    qualVisible.value = true
    loadQual()
}

function loadQual() {
    qualLoading.value = true
    sys.biz('cbs/cqual/list', {typeId: qualTypeId.value, qualType: qualType.value}, AC.signal, () => {}, (r, data) => {
        qualLoading.value = false
        if (r) qualRows.value = data.data || []
        else notifyError(data, '加载失败')
    })
}

function editQual(row) {
    qualForm.value = {
        qual_id: row.qual_id,
        qual_name: row.qual_name || '',
        qual_level: row.qual_level || '',
        validity: row.validity ?? 0,
        remind: row.remind,
        is_using: row.is_using !== false,
    }
}

function resetQualForm() {
    qualForm.value = emptyQualForm()
}

function saveQual() {
    const f = qualForm.value
    const name = (f.qual_name || '').trim()
    if (!name) {
        ElMessage.warning(qualIsFile.value ? '请输入文件名称' : '请输入资质名称')
        return
    }
    if (!f.qual_id && qualRows.value.length >= QUAL_MAX) {
        ElMessage.warning(`单类型最多 ${QUAL_MAX} 条`)
        return
    }
    const payload = {
        type_id: qualTypeId.value,
        qual_type: qualType.value,
        qual_name: name,
        qual_level: qualIsFile.value ? '' : (f.qual_level || '').trim(),
        validity: qualIsFile.value ? (f.validity ?? 0) : 0,
        remind: qualIsFile.value ? f.remind : null,
        is_using: f.is_using !== false,
    }
    if (f.qual_id) payload.qual_id = f.qual_id
    sys.biz('cbs/cqual/save', payload, AC.signal, () => {}, (r, data) => {
        if (r) {
            ElMessage.success(f.qual_id ? '已保存' : '已添加')
            qualForm.value = emptyQualForm()
            loadQual()
        } else notifyError(data, '保存失败')
    })
}

function toggleQual(row) {
    sys.biz('cbs/cqual/toggle', {qualId: row.qual_id, isUsing: !row.is_using}, AC.signal, () => {}, (r, data) => {
        if (r) {
            ElMessage.success(row.is_using ? '已停用' : '已启用')
            loadQual()
        } else notifyError(data, '操作失败')
    })
}

function removeQual(row) {
    ElMessageBox.confirm(`确定删除「${row.qual_name}」吗？`, '提示', {type: 'warning'}).then(() => {
        sys.biz('cbs/cqual/delete', {qualId: row.qual_id}, AC.signal, () => {}, (r, data) => {
            if (r) {
                ElMessage.success('已删除')
                if (qualForm.value.qual_id === row.qual_id) qualForm.value = emptyQualForm()
                loadQual()
            } else notifyError(data, '删除失败')
        })
    }).catch(() => {})
}

/* ============ 开工准备条件 / 资料清单（按项目类型维护） ============ */
const META = {
    prep: {title: '开工准备条件', table: 't_prep_condition'},
    doc: {title: '资料清单', table: 't_doc_list'},
}
const META_MAX = 200
const metaVisible = ref(false)
const metaLoading = ref(false)
const metaKind = ref('prep')
const metaTypeId = ref('')
const metaTypeName = ref('')
const metaRows = ref([])
const metaForm = ref({name: '', remark: ''})
const metaCfg = computed(() => META[metaKind.value] || META.prep)

function openMeta(kind, row) {
    metaKind.value = kind
    metaTypeId.value = row.item_id
    metaTypeName.value = row.item_name
    metaForm.value = {name: '', remark: ''}
    metaVisible.value = true
    loadMeta()
}

function loadMeta() {
    metaLoading.value = true
    sys.biz('cbs/meta/list', {
        table: metaCfg.value.table, typeId: metaTypeId.value, pageNo: 1, pageSize: META_MAX,
    }, AC.signal, () => {}, (r, data) => {
        metaLoading.value = false
        if (r) metaRows.value = data.data?.rows || []
        else notifyError(data, '加载失败')
    })
}

function saveMeta() {
    const name = metaForm.value.name.trim()
    if (!name) {
        ElMessage.warning('请输入名称')
        return
    }
    if (metaRows.value.length >= META_MAX) {
        ElMessage.warning(`单一项目类型最多 ${META_MAX} 条`)
        return
    }
    sys.biz('cbs/meta/save', {
        table: metaCfg.value.table, typeId: metaTypeId.value,
        name: name, remark: metaForm.value.remark.trim(),
    }, AC.signal, () => {}, (r, data) => {
        if (r) {
            ElMessage.success('已添加')
            metaForm.value = {name: '', remark: ''}
            loadMeta()
        } else notifyError(data, '添加失败')
    })
}

function removeMeta(row) {
    ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '提示', {type: 'warning'}).then(() => {
        sys.biz('cbs/meta/delete', {table: metaCfg.value.table, id: row.id}, AC.signal, () => {}, (r, data) => {
            if (r) {
                ElMessage.success('已删除')
                loadMeta()
            } else notifyError(data, '删除失败')
        })
    }).catch(() => {})
}

onMounted(loadRows)
onUnmounted(() => {
    AC.abort()
    clearTimeout(kwTimer)
})
</script>

<template>
    <div class="dict-page">
        <div class="page-head">
            <h2>{{ pageTitle }}</h2>
        </div>

        <div class="card">
            <div class="toolbar">
                <el-input v-model="keyword" placeholder="搜索名称" clearable
                          style="width:220px" :prefix-icon="Search"/>
                <!-- 回收站页不给新增：新行必然 open_status=true，落在「在用」里，点了会以为没生效 -->
                <el-button v-if="!recycled" type="primary" plain class="add-btn"
                           @click="openDialog(false)">新增</el-button>
                <!-- 右侧水平 在用/回收站 切页（仿目标站 contractorType，三类字典通用） -->
                <el-radio-group v-model="recycled" class="recycle-tabs">
                    <el-radio-button :value="false">在用</el-radio-button>
                    <el-radio-button :value="true">回收站</el-radio-button>
                </el-radio-group>
            </div>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column type="index" label="序号" width="70" align="center"
                                 :index="i => (pageNo - 1) * pageSize + i + 1"/>
                <el-table-column prop="item_name" label="名称" min-width="240"/>
                <el-table-column v-if="!recycled" label="状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.is_using ? 'success' : 'info'" effect="light" size="small">
                            {{ row.is_using ? '启用' : '停用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" :width="opWidth" align="center" fixed="right">
                    <template #default="{row}">
                        <template v-if="recycled">
                            <el-link type="primary" @click="restoreRow(row)">恢复</el-link>
                            <el-link type="danger" style="margin-left:10px" @click="purgeRow(row)">彻底删除</el-link>
                        </template>
                        <template v-else>
                            <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                            <el-link v-if="isContractorType" type="success" style="margin-left:10px"
                                     @click="openQual(row, 1)">资质管理</el-link>
                            <el-link v-if="isContractorType" type="success" style="margin-left:10px"
                                     @click="openQual(row, 2)">审核所需文件</el-link>
                            <template v-if="isProjectType">
                                <el-link type="success" style="margin-left:10px"
                                         @click="openMeta('prep', row)">开工准备条件</el-link>
                                <el-link type="success" style="margin-left:10px"
                                         @click="openMeta('doc', row)">资料清单</el-link>
                            </template>
                            <el-link type="danger" style="margin-left:10px" @click="removeRow(row)">删除</el-link>
                        </template>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0"
                      :description="recycled ? '回收站为空' : `暂无${pageTitle}`"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="pager.pageSizes"/>
        </div>

        <!-- 新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="form.item_id ? `编辑${pageTitle}` : `新增${pageTitle}`"
                   width="460px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-form-item label="名称" prop="item_name">
                    <el-input v-model="form.item_name" placeholder="字典项名称"/>
                </el-form-item>
                <el-form-item label="状态">
                    <el-switch v-model="form.is_using" active-text="启用"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveForm">保存</el-button>
            </template>
        </el-dialog>

        <!-- 资质管理 / 审核所需文件弹窗（同一套数据，qualType 区分；由承包商类型行内入口打开） -->
        <el-dialog v-model="qualVisible" :title="qualTitle" width="760px" top="6vh" destroy-on-close>
            <div v-loading="qualLoading">
                <el-form :inline="true" class="qual-form">
                    <el-form-item :label="qualNameLabel" required>
                        <el-input v-model="qualForm.qual_name" :placeholder="`请输入${qualNameLabel}`"
                                  style="width:190px"/>
                    </el-form-item>
                    <el-form-item v-if="!qualIsFile" label="等级/类别">
                        <el-input v-model="qualForm.qual_level" placeholder="如：一级 / 甲级" style="width:140px"/>
                    </el-form-item>
                    <el-form-item v-if="qualIsFile" label="有效期(月)">
                        <el-input-number v-model="qualForm.validity" :min="0" :controls="false"
                                         placeholder="0=长期有效" style="width:120px"/>
                    </el-form-item>
                    <el-form-item v-if="qualIsFile" label="临期提醒(天)">
                        <el-input-number v-model="qualForm.remind" :min="0" :controls="false"
                                         placeholder="如 30" style="width:110px"/>
                    </el-form-item>
                    <el-form-item label="状态">
                        <el-switch v-model="qualForm.is_using" active-text="启用"/>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="saveQual">{{ qualEditing ? '保存修改' : '添加' }}</el-button>
                        <el-button v-if="qualEditing" @click="resetQualForm">取消</el-button>
                    </el-form-item>
                </el-form>
                <el-table :data="qualRows" border stripe size="small" max-height="360">
                    <el-table-column type="index" label="序号" width="60" align="center"/>
                    <el-table-column prop="qual_name" :label="qualNameLabel" min-width="190"/>
                    <el-table-column v-if="!qualIsFile" prop="qual_level" label="等级/类别" width="130" align="center">
                        <template #default="{row}">{{ row.qual_level || '—' }}</template>
                    </el-table-column>
                    <el-table-column v-if="qualIsFile" label="有效期(月)" width="110" align="center">
                        <template #default="{row}">{{ row.validity ? `${row.validity} 个月` : '长期有效' }}</template>
                    </el-table-column>
                    <el-table-column v-if="qualIsFile" label="临期提醒(天)" width="120" align="center">
                        <template #default="{row}">{{ row.remind ?? '—' }}</template>
                    </el-table-column>
                    <el-table-column label="状态" width="90" align="center">
                        <template #default="{row}">
                            <el-tag :type="row.is_using ? 'success' : 'info'" effect="light" size="small">
                                {{ row.is_using ? '启用' : '停用' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" width="150" align="center">
                        <template #default="{row}">
                            <el-link type="primary" @click="editQual(row)">编辑</el-link>
                            <el-link :type="row.is_using ? 'warning' : 'success'" style="margin-left:10px"
                                     @click="toggleQual(row)">{{ row.is_using ? '停用' : '启用' }}</el-link>
                            <el-link type="danger" style="margin-left:10px" @click="removeQual(row)">删除</el-link>
                        </template>
                    </el-table-column>
                </el-table>
                <el-empty v-if="!qualLoading && qualRows.length === 0"
                          :description="`暂无${qualIsFile ? '审核所需文件' : '资质'}，请在上方添加`"/>
            </div>
        </el-dialog>
        <!-- 开工准备条件 / 资料清单弹窗（按项目类型维护，行内入口打开） -->
        <el-dialog v-model="metaVisible" :title="`${metaCfg.title} — ${metaTypeName}`" width="640px" top="6vh" destroy-on-close>
            <div v-loading="metaLoading">
                <el-form :inline="true" class="meta-form">
                    <el-form-item label="名称" required>
                        <el-input v-model="metaForm.name" placeholder="请输入名称" style="width:240px"/>
                    </el-form-item>
                    <el-form-item label="说明">
                        <el-input v-model="metaForm.remark" placeholder="补充说明（可选）" style="width:180px"/>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="saveMeta">添加</el-button>
                    </el-form-item>
                </el-form>
                <el-table :data="metaRows" border stripe size="small" max-height="360">
                    <el-table-column type="index" label="序号" width="60" align="center"/>
                    <el-table-column prop="name" label="名称" min-width="200"/>
                    <el-table-column prop="remark" label="说明" min-width="220" show-overflow-tooltip>
                        <template #default="{row}">{{ row.remark || '—' }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="90" align="center">
                        <template #default="{row}">
                            <el-link type="danger" @click="removeMeta(row)">删除</el-link>
                        </template>
                    </el-table-column>
                </el-table>
                <el-empty v-if="!metaLoading && metaRows.length === 0" :description="`暂无${metaCfg.title}，请在上方添加`"/>
            </div>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.dict-page {
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
        padding: 16px;
        border: 1px solid #e2e8f0;
    }

    .toolbar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 14px;

        .add-btn {
            margin-right: auto;
        }

        /* 右侧水平切页 */
        .recycle-tabs {
            margin-left: auto;
        }
    }

    .pager {
        margin-top: 14px;
        justify-content: flex-end;
    }

    .qual-form {
        margin-bottom: 12px;
    }

    .meta-form {
        margin-bottom: 12px;
    }
}
</style>
