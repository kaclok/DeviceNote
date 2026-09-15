<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import DeptPicker from "../components/DeptPicker.vue"
import {buildDeptPathMap, deptDisplay, deptShort, isUnknownDept} from "../utils/DeptX.js"

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(15)

// 搜索条件：关键字（账号/姓名模糊）与归属部门 —— 均由后端过滤，配合服务端分页
const keyword = ref('')
const deptFilter = ref('')

// 表头排序状态：sortProp 为 null 时用原始排序。
// 服务端分页下排序只对当前页生效（与 ledger.vue 保持一致）。
const sortProp = ref(null)
const sortOrder = ref(null) // 'ascending' | 'descending' | null
const sortedList = computed(() => {
    if (!sortProp.value || !sortOrder.value) return list.value
    const sorted = [...list.value]
    sorted.sort((a, b) => {
        let cmp = 0
        if (sortProp.value === 'role_code') {
            cmp = (a.role?.role_code || '').localeCompare(b.role?.role_code || '')
        } else if (sortProp.value === 'open_status') {
            cmp = (a.open_status ? 1 : 0) - (b.open_status ? 1 : 0)
        }
        return sortOrder.value === 'ascending' ? cmp : -cmp
    })
    return sorted
})

// 角色列表（动态数据，由后端下发
const roles = ref([])
// 权限码字典（动态数据，由后端下发
const permDefs = ref([])

/* ---------------- 组织架构（归属部门字典） ---------------- */
// 数据源 /cghtz/dept/list（train.t_org），登录后已由 SysX 预加载缓存，这里只读缓存
const deptOptions = ref([])
const deptPathMap = computed(() => buildDeptPathMap(deptOptions.value))
/** 归属部门展示文本：命中字典 → 「公司/部门」；未命中 → 「未知部门(code)」 */
function deptPath(code) {
    return deptDisplay(deptPathMap.value, code)
}

/** 该编码未命中字典（用于把「未知部门(code)」标灰） */
function deptUnknown(code) {
    return isUnknownDept(deptPathMap.value, code)
}

/** 列表列位窄：只显示末级部门名，完整「公司/部门」路径放 tooltip */
function deptShortName(code) {
    return deptShort(deptPathMap.value, code)
}

// 权限分组（基于动态 permDefs 计算）
const permGroups = computed(() => {
    const groups = {}
    permDefs.value.forEach(p => {
        if (!groups[p.group]) groups[p.group] = []
        groups[p.group].push(p)
    })
    return Object.entries(groups).map(([name, items]) => ({name, items}))
})

// 当前所选角色的权限码集合（只读展示，由角色决定，不可手动勾选）
const currentRolePerms = computed(() => {
    const r = roles.value.find(x => x.role_code === form.value.role_code)
    return new Set(r?.perms || [])
})

// 列表请求在翻页/搜索时会重发，需取消上一次未完成的请求，避免旧响应覆盖新响应
let AC_list = new AbortController()
const AC_roles = new AbortController()
const AC_perms = new AbortController()
const AC_dept = new AbortController()

onMounted(() => {
    loadList()
    loadRoles()
    loadPermDefs()
    loadDepts()
})

onUnmounted(() => {
    AC_list.abort()
    AC_roles.abort()
    AC_perms.abort()
    AC_dept.abort()
    clearTimeout(kwTimer)
})

function loadList() {
    AC_list.abort()
    AC_list = new AbortController()

    loading.value = true
    // 只传非空条件，避免后端把空串当成"筛选空值"
    const paras = {pageNum: page.value, pageSize: pageSize.value}
    const kw = keyword.value.trim()
    if (kw) paras.kw = kw
    if (deptFilter.value) paras.dept_code = deptFilter.value

    Singleton.getInstance(SysX).getAccountList(paras, AC_list.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) {
            list.value = data.data.list || []
            total.value = data.data.total || 0
        }
    })
}

// 关键字变化防抖 300ms 后重新查询（回到第一页）
let kwTimer = null
watch(keyword, () => {
    clearTimeout(kwTimer)
    kwTimer = setTimeout(() => {
        page.value = 1
        loadList()
    }, 300)
})

function applySearch() {
    page.value = 1
    loadList()
}

// 服务端分页：页码/每页条数变化需重新发请求
function onPageChange(p) {
    page.value = p
    loadList()
}

function onSizeChange(s) {
    pageSize.value = s
    page.value = 1
    loadList()
}

function loadRoles() {
    Singleton.getInstance(SysX).getRoleList(null, AC_roles.signal, () => {
    }, (r, data) => {
        if (r) {
            roles.value = data.data || []
        }
    })
}

function loadPermDefs() {
    Singleton.getInstance(SysX).getPermDefs(null, AC_perms.signal, () => {
    }, (r, data) => {
        if (r) {
            permDefs.value = data.data || []
        }
    })
}

function loadDepts() {
    Singleton.getInstance(SysX).getDeptList(null, AC_dept.signal, () => {
    }, (r, data) => {
        if (r) {
            deptOptions.value = data.data || []
        }
    })
}

/** el-table 表头排序：三态切换 ascending → descending → null(原始排序) */
function onSortChange({prop, order}) {
    sortProp.value = prop || null
    sortOrder.value = order || null
}

function statusTag(status) {
    return status ? {type: 'success', text: '启用'} : {type: 'info', text: '停用'}
}

function roleTag(roleCode) {
    const map = {
        'ADMIN': 'danger',
        'EDITOR': 'primary',
        'VIEWER': 'warning',
    }
    return map[roleCode] || 'info'
}

/* ---------------- 新建账号 ---------------- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const saving = ref(false)
const form = ref({account: '', username: '', role_code: 'EDITOR', dept_code: '', password: ''})

const rules = {
    account: [{required: true, message: '请输入账号', trigger: 'blur'}],
    username: [{required: true, message: '请输入姓名', trigger: 'blur'}],
    role_code: [{required: true, message: '请选择角色', trigger: 'change'}],
    // 归属部门必填（后端强校验）。trigger 用 change：下拉选择与组织树确认都会 emit change
    dept_code: [{required: true, message: '请选择归属部门', trigger: 'change'}],
}

function openCreate() {
    isEdit.value = false
    form.value = {account: '', username: '', role_code: 'EDITOR', dept_code: '', password: ''}
    dialogVisible.value = true
}

function openEdit(row) {
    isEdit.value = true
    form.value = {
        account: row.account,
        username: row.username,
        role_code: row.role?.role_code || '',
        dept_code: row.dept_code || '',
        password: '',
    }
    dialogVisible.value = true
}

// 权限是否属于当前所选角色（只读展示，不可手动操作）
function hasPermInForm(code) {
    return currentRolePerms.value.has(code)
}

/**
 * DeptPicker 选完部门后立刻消掉必填的红字。
 * 下拉路径由 el-select 触发 change 能自然带出校验，但"组织架构树弹窗"是程序化 emit，
 * 不在表单元素的事件链上，不显式 validateField 会残留红色提示。
 */
function onDeptChange() {
    formRef.value?.validateField('dept_code').catch(() => {
    })
}

function saveAccount() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        // 提交体：与后端 accountSave @RequestParam 一致：account/username/role_code/dept_code/password(新增用)
        const paras = {
            account: String(form.value.account || '').trim(),
            username: String(form.value.username || '').trim(),
            role_code: form.value.role_code,
            dept_code: form.value.dept_code || '',
        }
        if (!isEdit.value) {
            paras.password = String(form.value.password || '').trim()
        }
        Singleton.getInstance(SysX).saveAccount(paras, new AbortController().signal, () => {
        }, (r, data) => {
            saving.value = false
            if (r) {
                ElMessage.success(isEdit.value ? '保存成功' : `账号${paras.account}创建成功，初始密码：${paras.password || gd.defaultPwd}`)
                dialogVisible.value = false
                loadList()
            } else {
                ElMessage.error(data?.data?.message || '保存失败')
            }
        })
    })
}

/* ---------------- 其他操作 ---------------- */
function resetPwd(row) {
    ElMessageBox.confirm(`确定将 ${row.account}（${row.username || ''}）的密码重置为 ${gd.defaultPwd} 吗？`, '重置密码', {type: 'warning'}).then(() => {
        Singleton.getInstance(SysX).resetPassword({account: row.account}, new AbortController().signal, () => {
        }, (r, data) => {
            if (r) {
                ElMessage.success(`密码已重置`)
            } else {
                ElMessage.error('密码重置失败')
            }
        })
    }).catch(() => {
    })
}

function toggleStatus(row) {
    const tip = row.open_status ? '停用' : '启用'
    ElMessageBox.confirm(`确定${tip}账号 ${row.account} 吗？`, '提示', {type: 'warning'}).then(() => {
        Singleton.getInstance(SysX).toggleAccountStatus({account: row.account}, new AbortController().signal, () => {
        }, (r, data) => {
            if (r) {
                ElMessage.success(`已${tip}`)
                loadList()
            }
        })
    }).catch(() => {
    })
}
</script>

<template>
    <div class="users-page">
        <div class="page-head">
            <div class="head-title">账号与权限管理</div>
            <div class="head-desc">为每个账号分配角色与归属部门，权限由角色决定，不可手动调整</div>
        </div>

        <el-card shadow="never" class="table-card">
            <div class="toolbar">
                <el-button v-hasPermission="['perm:assign']" type="primary" @click="openCreate">＋ 新建账号</el-button>
                <div class="spacer"></div>
                <div class="dept-filter">
                    <DeptPicker v-model="deptFilter" :depts="deptOptions" placeholder="按部门筛选" @change="applySearch"/>
                </div>
                <el-input v-model="keyword" placeholder="搜索账号 / 姓名" clearable style="width:220px">
                    <template #prefix><span style="color:#94a3b8">🔍</span></template>
                </el-input>
            </div>

            <el-table :data="sortedList" v-loading="loading" border stripe style="width:100%" @sort-change="onSortChange">
                <el-table-column type="index" label="序号" width="64" align="center"/>
                <el-table-column prop="account" label="账号" width="120">
                    <template #default="{row}"><b style="color:#2563eb">{{ row.account }}</b></template>
                </el-table-column>
                <el-table-column prop="username" label="姓名" width="110"/>
                <el-table-column prop="dept_code" label="归属部门" min-width="140">
                    <template #default="{row}">
                        <span v-if="!row.dept_code" style="color:#cbd5e1">-</span>
                        <el-tooltip v-else :content="deptPath(row.dept_code)" placement="top">
                            <span :class="{'dept-unknown': deptUnknown(row.dept_code)}">{{ deptShortName(row.dept_code) }}</span>
                        </el-tooltip>
                    </template>
                </el-table-column>
                <el-table-column prop="role_code" label="角色" width="110" align="center" sortable="custom">
                    <template #default="{row}">
                        <el-tag :type="roleTag(row.role.role_code)" size="small" effect="light">{{ row.role.role_name }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="open_status" label="状态" width="90" align="center" sortable="custom">
                    <template #default="{row}">
                        <el-tag :type="statusTag(row.open_status).type" size="small">{{ statusTag(row.open_status).text }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="250" fixed="right" align="center">
                    <template #default="{row}">
                        <el-button v-notSelf.readonly="row.account" link type="primary" size="small" @click="openEdit(row)">编辑/授权</el-button>
                        <el-button v-notSelf.readonly="row.account" link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
                        <el-button v-notSelf.readonly="row.account" link :type="row.open_status === 1 ? 'danger' : 'success'" size="small" @click="toggleStatus(row)">
                            {{ row.open_status ? '停用' : '启用' }}
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>

            <div class="pager">
                <el-pagination
                    :current-page="page"
                    :page-size="pageSize"
                    :page-sizes="[15, 30, 60, 100]"
                    :total="total"
                    layout="total, sizes, prev, pager, next, jumper"
                    background
                    @current-change="onPageChange"
                    @size-change="onSizeChange"
                />
            </div>
        </el-card>

        <!-- 新建/编辑账号 + 权限预览弹窗 -->
        <el-dialog v-model="dialogVisible" :title="isEdit ? `编辑账号与权限：${form.account}` : '新建账号'" width="640px" destroy-on-close>
            <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
                <el-row :gutter="16">
                    <el-col :span="12">
                        <el-form-item label="账号" prop="account">
                            <el-input v-model="form.account" placeholder="登录账号" :disabled="isEdit"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="姓名" prop="username">
                            <el-input v-model="form.username" placeholder="真实姓名"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="角色" prop="role_code">
                            <el-select v-model="form.role_code" placeholder="请选择角色" style="width:100%">
                                <el-option v-for="r in roles" :key="r.role_code" :label="r.role_name" :value="r.role_code"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="归属部门" prop="dept_code">
                            <DeptPicker v-model="form.dept_code" :depts="deptOptions" :teleported="false" @change="onDeptChange"/>
                        </el-form-item>
                    </el-col>
                    <el-col v-if="!isEdit" :span="24">
                        <el-form-item label="初始密码">
                            <el-input v-model="form.password" :placeholder="`留空则默认 ${gd.defaultPwd}`"/>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>

            <div class="perm-title">
                权限预览（由所选角色决定，不可手动调整）
                <span v-if="form.role_code" class="perm-title-role">
                    当前角色：<b>{{ roles.find(r => r.role_code === form.role_code)?.role_name }}</b>
                </span>
            </div>
            <div class="perm-tree">
                <div v-for="g in permGroups" :key="g.name" class="perm-group">
                    <div class="perm-group-name">{{ g.name }}</div>
                    <div class="perm-items">
                        <div v-for="p in g.items" :key="p.perm_code" class="perm-item"
                             :class="{checked: hasPermInForm(p.perm_code)}">
                            <span class="checkbox" :class="{checked: hasPermInForm(p.perm_code)}"></span>
                            <span>{{ p.perm_name }}</span>
                            <span class="perm-code">{{ p.perm_code }}</span>
                        </div>
                    </div>
                </div>
            </div>

            <template #footer>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="saving" @click="saveAccount">{{ isEdit ? '保存' : '创建账号' }}</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
/* 归属部门字典未命中：灰色标出「未知部门(code)」，避免与正常部门名混淆 */
.dept-unknown {
    color: #94a3b8;
}

.users-page {
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

    .toolbar {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 14px;

        .spacer {
            flex: 1
        }

        /* DeptPicker 根节点是 100% 宽，工具栏里需要固定宽度 */
        .dept-filter {
            width: 240px;
        }
    }

    .pager {
        margin-top: 14px;
        display: flex;
        justify-content: flex-end;
    }

    .perm-title {
        font-size: 14px;
        font-weight: 600;
        margin: 8px 0 10px;
        display: flex;
        align-items: center;
        justify-content: space-between;

        .perm-title-role {
            font-size: 12px;
            font-weight: 400;
            color: #64748b;
        }
    }

    .perm-tree {
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        overflow: hidden;
        max-height: 360px;
        overflow-y: auto;

        .perm-group {
            border-bottom: 1px solid #e2e8f0;

            &:last-child {
                border-bottom: none
            }

            .perm-group-name {
                padding: 10px 14px;
                background: #f8fafc;
                font-weight: 600;
                font-size: 13px;
            }

            .perm-items {
                display: grid;
                grid-template-columns: repeat(2, 1fr);
                gap: 8px;
                padding: 12px 14px;

                .perm-item {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    font-size: 13px;
                    color: #94a3b8;
                    padding: 8px 10px;
                    border: 1px solid #e2e8f0;
                    border-radius: 8px;
                    cursor: not-allowed;
                    transition: all .15s;

                    &.checked {
                        background: #eff6ff;
                        border-color: #2563eb;
                        color: #2563eb;
                    }

                    .checkbox {
                        width: 16px;
                        height: 16px;
                        border-radius: 4px;
                        border: 2px solid #cbd5e1;
                        flex-shrink: 0;
                        transition: all .15s;
                        position: relative;

                        &.checked {
                            background: #2563eb;
                            border-color: #2563eb;

                            &::after {
                                content: '✓';
                                position: absolute;
                                inset: 0;
                                display: flex;
                                align-items: center;
                                justify-content: center;
                                color: #fff;
                                font-size: 11px;
                                font-weight: 700;
                            }
                        }
                    }

                    .perm-code {
                        margin-left: auto;
                        font-size: 11px;
                        color: #94a3b8;
                        font-family: monospace;
                    }
                }
            }
        }
    }
}
</style>
