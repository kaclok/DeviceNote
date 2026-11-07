<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {invalidateDict} from "../system/DictX.js"
import {ElMessage, ElMessageBox} from "element-plus";
import {Plus, Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* 一个菜单 = 一个字典类型（承包商类型 / 承包商处罚类型 / 项目类型），由路由 :type 决定 */
const TYPES = gd.dictTypes.map(t => t.id)
const props = defineProps({type: String})
const activeType = ref(TYPES.includes(props?.type) ? props.type : TYPES[0])
const pageTitle = computed(() => (gd.dictTypes.find(t => t.id === activeType.value) || {}).desc || '')

/* 三类字典统一支持 在用/回收站：页内右侧水平两个切页（回收站 = 软删行 open_status=false） */
const recycled = ref(false)

/* 项目类型：行内维护该类型下的「开工准备文件 / 资料清单」（仿参考站 prepareFileModal / meansListModal） */
const isProjectType = computed(() => activeType.value === 'project_type')

/* 承包商类型：行内维护 资质管理 / 审核所需文件（仿参考站 typeFlairManageModal） */
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

/* ============ 资质管理 / 审核所需文件（仅承包商类型；同一张表，qualType 区分） ============
 * 布局仿参考站 typeFlairManageModal：主弹窗 = 「新增」+ 列表（只有关闭按钮），新增/编辑走二级弹窗 */
const QUAL_QUAL = 1
const QUAL_FILE = 2
const QUAL_MAX = 200
const qualVisible = ref(false)
const qualLoading = ref(false)
const qualType = ref(QUAL_QUAL)
const qualTypeId = ref('')
const qualTypeName = ref('')
const qualRows = ref([])
const qualIsFile = computed(() => qualType.value === QUAL_FILE)
const qualTitle = computed(() => (qualIsFile.value ? '审核所需文件' : '资质管理'))
const qualNameLabel = computed(() => (qualIsFile.value ? '文件名称' : '资质名称'))

const qualFormVisible = ref(false)
const qualSaving = ref(false)
const qualFormRef = ref()
const emptyQualForm = () => ({qual_id: '', qual_name: '', validity: null, remind: null, is_using: true})
const qualForm = ref(emptyQualForm())
const qualFormTitle = computed(() => `${qualForm.value.qual_id ? '编辑' : '新增'}${qualNameLabel.value}`)
const qualRules = computed(() => ({
    qual_name: [{required: true, message: `请输入${qualNameLabel.value}`, trigger: 'blur'}],
    validity: [{required: true, message: '请输入有效期(月)', trigger: 'blur'}],
    remind: [{required: true, message: '请输入临期提醒(天)', trigger: 'blur'}],
}))

function openQual(row, type) {
    qualType.value = type
    qualTypeId.value = row.item_id
    qualTypeName.value = row.item_name
    qualRows.value = []
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

function addQual() {
    qualForm.value = emptyQualForm()
    qualFormVisible.value = true
}

function editQual(row) {
    qualForm.value = {
        qual_id: row.qual_id,
        qual_name: row.qual_name || '',
        validity: row.validity ?? 0,
        remind: row.remind,
        is_using: row.is_using !== false,
    }
    qualFormVisible.value = true
}

function saveQual() {
    qualFormRef.value.validate(valid => {
        if (!valid) return
        const f = qualForm.value
        if (!f.qual_id && qualRows.value.length >= QUAL_MAX) {
            ElMessage.warning(`单类型最多 ${QUAL_MAX} 条`)
            return
        }
        qualSaving.value = true
        const payload = {
            type_id: qualTypeId.value,
            qual_type: qualType.value,
            qual_name: (f.qual_name || '').trim(),
            validity: f.validity ?? 0,
            remind: f.remind,
            is_using: f.is_using !== false,
        }
        if (f.qual_id) payload.qual_id = f.qual_id
        sys.biz('cbs/cqual/save', payload, AC.signal, () => {}, (r, data) => {
            qualSaving.value = false
            if (r) {
                ElMessage.success(f.qual_id ? '已保存' : '已添加')
                qualFormVisible.value = false
                loadQual()
            } else notifyError(data, '保存失败')
        })
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
                loadQual()
            } else notifyError(data, '删除失败')
        })
    }).catch(() => {})
}

/* ============ 开工准备文件 / 资料清单（按项目类型维护） ============
 * 布局仿参考站 prepareFileModal / meansListModal：主弹窗 = 「新增」+ 列表，新增/编辑走二级弹窗 */
const META = {
    prep: {title: '开工准备文件', table: 't_prep_condition', bordered: true},
    doc: {title: '资料清单', table: 't_doc_list', bordered: false},
}
const META_MAX = 200
const metaVisible = ref(false)
const metaLoading = ref(false)
const metaKind = ref('prep')
const metaTypeId = ref('')
const metaTypeName = ref('')
const metaRows = ref([])
const metaCfg = computed(() => META[metaKind.value] || META.prep)

const metaFormVisible = ref(false)
const metaSaving = ref(false)
const metaFormRef = ref()
const emptyMetaForm = () => ({id: '', name: '', remark: ''})
const metaForm = ref(emptyMetaForm())
const metaFormTitle = computed(() => (metaForm.value.id ? '编辑' : '新增'))
const metaRules = {name: [{required: true, message: '请输入名称', trigger: 'blur'}]}

function openMeta(kind, row) {
    metaKind.value = kind
    metaTypeId.value = row.item_id
    metaTypeName.value = row.item_name
    metaRows.value = []
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

function addMeta() {
    metaForm.value = emptyMetaForm()
    metaFormVisible.value = true
}

function editMeta(row) {
    metaForm.value = {id: row.id, name: row.name || '', remark: row.remark || ''}
    metaFormVisible.value = true
}

function saveMeta() {
    metaFormRef.value.validate(valid => {
        if (!valid) return
        const f = metaForm.value
        if (!f.id && metaRows.value.length >= META_MAX) {
            ElMessage.warning(`单一项目类型最多 ${META_MAX} 条`)
            return
        }
        metaSaving.value = true
        const payload = {
            table: metaCfg.value.table, typeId: metaTypeId.value,
            name: f.name.trim(), remark: (f.remark || '').trim(),
        }
        if (f.id) payload.id = f.id
        sys.biz('cbs/meta/save', payload, AC.signal, () => {}, (r, data) => {
            metaSaving.value = false
            if (r) {
                ElMessage.success(f.id ? '已保存' : '已添加')
                metaFormVisible.value = false
                loadMeta()
            } else notifyError(data, f.id ? '保存失败' : '添加失败')
        })
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
                                         @click="openMeta('prep', row)">开工准备文件</el-link>
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

        <!-- 资质管理 / 审核所需文件（仅承包商类型行内入口；主弹窗只放「新增」+ 列表，仿参考站） -->
        <el-dialog v-model="qualVisible" :title="qualTitle" width="80%" top="6vh" destroy-on-close>
            <div class="table-operator">
                <el-button type="primary" :icon="Plus" @click="addQual">新增</el-button>
                <span class="op-ctx">{{ qualTypeName }}</span>
            </div>
            <el-table :data="qualRows" v-loading="qualLoading" max-height="480">
                <el-table-column type="index" label="序号" width="80" align="center"/>
                <el-table-column prop="qual_name" :label="qualNameLabel" min-width="200" align="center"/>
                <el-table-column label="有效期(月)" width="130" align="center">
                    <template #default="{row}">{{ row.validity ? row.validity : '长期有效' }}</template>
                </el-table-column>
                <el-table-column label="临期提醒(天)" width="140" align="center">
                    <template #default="{row}">{{ row.remind ?? '—' }}</template>
                </el-table-column>
                <el-table-column prop="update_time" label="更新时间" width="180" align="center">
                    <template #default="{row}">{{ row.update_time || '—' }}</template>
                </el-table-column>
                <el-table-column label="状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.is_using ? 'success' : 'info'" effect="light" size="small">
                            {{ row.is_using ? '启用' : '停用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="180" align="center">
                    <template #default="{row}">
                        <el-link type="primary" @click="editQual(row)">编辑</el-link>
                        <el-divider direction="vertical"/>
                        <el-link :type="row.is_using ? 'warning' : 'success'" @click="toggleQual(row)">
                            {{ row.is_using ? '停用' : '启用' }}
                        </el-link>
                        <el-divider direction="vertical"/>
                        <el-link type="danger" @click="removeQual(row)">删除</el-link>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty :description="`暂无${qualTitle}`"/>
                </template>
            </el-table>
            <template #footer>
                <el-button @click="qualVisible = false">关闭</el-button>
            </template>

            <!-- 二级弹窗：新增/编辑资质（仿参考站 addTypeFlairModal） -->
            <el-dialog v-model="qualFormVisible" :title="qualFormTitle" width="600px"
                       append-to-body destroy-on-close class="sub-dialog">
                <el-form ref="qualFormRef" :model="qualForm" :rules="qualRules" label-width="120px">
                    <el-form-item :label="qualNameLabel" prop="qual_name">
                        <el-input v-model="qualForm.qual_name" :placeholder="`请输入${qualNameLabel}`"/>
                    </el-form-item>
                    <el-form-item label="状态">
                        <el-radio-group v-model="qualForm.is_using">
                            <el-radio :value="false">停用</el-radio>
                            <el-radio :value="true">启用</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="有效期(月)" prop="validity">
                        <el-input-number v-model="qualForm.validity" :min="0" :controls="false"
                                         placeholder='输入"0"表示长期有效'/>
                    </el-form-item>
                    <el-form-item label="临期提醒(天)" prop="remind">
                        <el-input-number v-model="qualForm.remind" :min="0" :controls="false"
                                         placeholder="请输入临期提醒(天)"/>
                    </el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="qualFormVisible = false">关闭</el-button>
                    <el-button type="primary" :loading="qualSaving" @click="saveQual">确定</el-button>
                </template>
            </el-dialog>
        </el-dialog>

        <!-- 开工准备文件 / 资料清单（项目类型行内入口；主弹窗只放「新增」+ 列表，仿参考站） -->
        <el-dialog v-model="metaVisible" :title="metaCfg.title" width="800px" top="6vh" destroy-on-close>
            <div class="table-operator">
                <el-button type="primary" :icon="Plus" @click="addMeta">新增</el-button>
                <span class="op-ctx">{{ metaTypeName }}</span>
            </div>
            <el-table :data="metaRows" v-loading="metaLoading" :border="metaCfg.bordered" max-height="480">
                <el-table-column type="index" label="序号" width="80" align="center"/>
                <el-table-column prop="name" label="名称" min-width="200" align="center"/>
                <el-table-column prop="remark" label="描述" min-width="300" align="center">
                    <template #default="{row}">{{ row.remark || '—' }}</template>
                </el-table-column>
                <el-table-column label="操作" width="140" align="center">
                    <template #default="{row}">
                        <el-link type="primary" @click="editMeta(row)">编辑</el-link>
                        <el-divider direction="vertical"/>
                        <el-link type="danger" @click="removeMeta(row)">删除</el-link>
                    </template>
                </el-table-column>
                <template #empty>
                    <el-empty :description="`暂无${metaCfg.title}`"/>
                </template>
            </el-table>
            <template #footer>
                <el-button @click="metaVisible = false">关闭</el-button>
            </template>

            <!-- 二级弹窗：新增/编辑（仿参考站 j-modal form 600px：名称 + 描述） -->
            <el-dialog v-model="metaFormVisible" :title="metaFormTitle" width="600px"
                       append-to-body destroy-on-close class="sub-dialog">
                <el-form ref="metaFormRef" :model="metaForm" :rules="metaRules" label-width="120px">
                    <el-form-item label="名称" prop="name">
                        <el-input v-model="metaForm.name" placeholder="请输入名称"/>
                    </el-form-item>
                    <el-form-item label="描述">
                        <el-input v-model="metaForm.remark" type="textarea" :rows="3"
                                  placeholder="请输入描述"/>
                    </el-form-item>
                </el-form>
                <template #footer>
                    <el-button @click="metaFormVisible = false">关闭</el-button>
                    <el-button type="primary" :loading="metaSaving" @click="saveMeta">确定</el-button>
                </template>
            </el-dialog>
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

    /* 弹窗内工具条：左「新增」，右当前上下文（仿参考站 table-operator） */
    .table-operator {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 12px;

        .op-ctx {
            margin-left: auto;
            color: #8a94a6;
            font-size: 13px;
        }
    }

    /* 二级弹窗表单控件占满整行（与参考站 span:24 一致） */
    .sub-dialog :deep(.el-input-number) {
        width: 100%;
    }
}
</style>
