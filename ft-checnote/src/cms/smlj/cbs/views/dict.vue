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

/* 承包商类型：仿目标站 contractorType，页内右侧水平放置 在用/回收站 两个切页（回收站=软删行） */
const recycle = computed(() => activeType.value === 'contractor_type')
const recycled = ref(false)

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
    if (recycle.value) paras.recycled = recycled.value
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
function removeRow(row) {
    if (recycle.value && !recycled.value) {
        // 承包商类型：删除 = 进回收站（软删），可在回收站恢复
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
    } else {
        // 非回收站类型：直接彻底删除
        ElMessageBox.confirm(`确定删除「${row.item_name}」吗？删除后不可恢复。`, '提示', {type: 'warning'}).then(() => {
            loading.value = true
            sys.biz('cbs/dict/purge', {itemId: row.item_id}, AC.signal, () => {}, (r, data) => {
                loading.value = false
                if (r) {
                    ElMessage.success('已删除')
                    invalidateDict(activeType.value)
                    loadRows()
                } else notifyError(data, '删除失败')
            })
        }).catch(() => {})
    }
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

/* ============ 资质管理（仅承包商类型，按类型维护所需资质） ============ */
const qualVisible = ref(false)
const qualLoading = ref(false)
const qualTypeId = ref('')
const qualTypeName = ref('')
const qualRows = ref([])
const qualForm = ref({qual_name: '', qual_level: ''})
const QUAL_MAX = 200

function openQual(row) {
    qualTypeId.value = row.item_id
    qualTypeName.value = row.item_name
    qualForm.value = {qual_name: '', qual_level: ''}
    qualVisible.value = true
    loadQual()
}

function loadQual() {
    qualLoading.value = true
    sys.biz('cbs/cqual/list', {typeId: qualTypeId.value}, AC.signal, () => {}, (r, data) => {
        qualLoading.value = false
        if (r) qualRows.value = data.data || []
        else notifyError(data, '资质加载失败')
    })
}

function saveQual() {
    const name = qualForm.value.qual_name.trim()
    if (!name) {
        ElMessage.warning('请输入资质名称')
        return
    }
    if (qualRows.value.length >= QUAL_MAX) {
        ElMessage.warning(`单类型资质最多 ${QUAL_MAX} 条`)
        return
    }
    sys.biz('cbs/cqual/save', {
        type_id: qualTypeId.value,
        qual_name: name,
        qual_level: qualForm.value.qual_level.trim(),
    }, AC.signal, () => {}, (r, data) => {
        if (r) {
            ElMessage.success('已添加')
            qualForm.value = {qual_name: '', qual_level: ''}
            loadQual()
        } else notifyError(data, '添加失败')
    })
}

function removeQual(row) {
    sys.biz('cbs/cqual/delete', {qualId: row.qual_id}, AC.signal, () => {}, (r, data) => {
        if (r) {
            ElMessage.success('已删除')
            loadQual()
        } else notifyError(data, '删除失败')
    })
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
                <el-button type="primary" plain class="add-btn" @click="openDialog(false)">新增</el-button>
                <!-- 承包商类型：右侧水平 在用/回收站 切页（仿目标站 contractorType） -->
                <el-radio-group v-if="recycle" v-model="recycled" class="recycle-tabs">
                    <el-radio-button :value="false">在用</el-radio-button>
                    <el-radio-button :value="true">回收站</el-radio-button>
                </el-radio-group>
            </div>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column type="index" label="序号" width="70" align="center"
                                 :index="i => (pageNo - 1) * pageSize + i + 1"/>
                <el-table-column prop="item_name" label="名称" min-width="240"/>
                <el-table-column label="状态" width="100" align="center">
                    <template #default="{row}">
                        <el-tag :type="row.is_using ? 'success' : 'info'" effect="light" size="small">
                            {{ row.is_using ? '启用' : '停用' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="220" align="center" fixed="right">
                    <template #default="{row}">
                        <template v-if="recycle && recycled">
                            <el-link type="primary" @click="restoreRow(row)">恢复</el-link>
                            <el-link type="danger" style="margin-left:10px" @click="purgeRow(row)">彻底删除</el-link>
                        </template>
                        <template v-else>
                            <el-link type="primary" @click="openDialog(true, row)">编辑</el-link>
                            <el-link v-if="recycle" type="success" style="margin-left:10px"
                                     @click="openQual(row)">资质管理</el-link>
                            <el-link type="danger" style="margin-left:10px" @click="removeRow(row)">删除</el-link>
                        </template>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0"
                      :description="recycle && recycled ? '回收站为空' : `暂无${pageTitle}`"/>

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

        <!-- 资质管理弹窗（按承包商类型维护所需资质） -->
        <el-dialog v-model="qualVisible" :title="`资质管理 — ${qualTypeName}`" width="640px" top="6vh" destroy-on-close>
            <div v-loading="qualLoading">
                <el-form :inline="true" class="qual-form">
                    <el-form-item label="资质名称" required>
                        <el-input v-model="qualForm.qual_name" placeholder="如：建筑施工总承包资质" style="width:240px"/>
                    </el-form-item>
                    <el-form-item label="等级/类别">
                        <el-input v-model="qualForm.qual_level" placeholder="如：一级 / 甲级" style="width:160px"/>
                    </el-form-item>
                    <el-form-item>
                        <el-button type="primary" @click="saveQual">添加</el-button>
                    </el-form-item>
                </el-form>
                <el-table :data="qualRows" border stripe size="small" max-height="360">
                    <el-table-column type="index" label="序号" width="60" align="center"/>
                    <el-table-column prop="qual_name" label="资质名称" min-width="220"/>
                    <el-table-column prop="qual_level" label="等级/类别" width="140"/>
                    <el-table-column label="操作" width="90" align="center">
                        <template #default="{row}">
                            <el-link type="danger" @click="removeQual(row)">删除</el-link>
                        </template>
                    </el-table-column>
                </el-table>
                <el-empty v-if="!qualLoading && qualRows.length === 0" description="暂无资质，请在上方添加"/>
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
}
</style>
