<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {useCache} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useCache()

const loading = ref(false)
const list = ref([])

// 搜索关键字（按账号 / 姓名模糊匹配）
const keyword = ref('')
const filteredList = computed(() => {
    const kw = keyword.value.trim().toLowerCase()
    if (!kw) return list.value
    return list.value.filter(row =>
        (row.account || '').toLowerCase().includes(kw) ||
        (row.realName || '').toLowerCase().includes(kw)
    )
})

// 角色列表（动态数据，由后端下发；现走 MockX.getRoleList）
const roles = ref([])
// 权限码字典（动态数据，由后端下发；现走 MockX.getPermDefs）
const permDefs = ref([])

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

const AC_list = new AbortController()
const AC_roles = new AbortController()
const AC_perms = new AbortController()

onMounted(() => {
    loadList()
    loadRoles()
    loadPermDefs()
})

onUnmounted(() => {
    AC_list.abort()
    AC_roles.abort()
    AC_perms.abort()
})

function loadList() {
    loading.value = true
    Singleton.getInstance(SysX).getAccountList({}, AC_list.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) list.value = data.data || []
    })
}

function loadRoles() {
    Singleton.getInstance(SysX).getRoleList({}, AC_roles.signal, () => {
    }, (r, data) => {
        if (r) roles.value = data.data || []
    })
}

function loadPermDefs() {
    Singleton.getInstance(SysX).getPermDefs({}, AC_perms.signal, () => {
    }, (r, data) => {
        if (r) permDefs.value = data.data || []
    })
}

function statusTag(status) {
    return status === 1 ? {type: 'success', text: '启用'} : {type: 'info', text: '停用'}
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
const form = ref({account: '', realName: '', role_code: 'EDITOR', password: ''})

const rules = {
    account: [{required: true, message: '请输入账号', trigger: 'blur'}],
    realName: [{required: true, message: '请输入姓名', trigger: 'blur'}],
    role_code: [{required: true, message: '请选择角色', trigger: 'change'}],
}

function openCreate() {
    isEdit.value = false
    form.value = {account: '', realName: '', role_code: 'EDITOR', password: ''}
    dialogVisible.value = true
}

function openEdit(row) {
    isEdit.value = true
    form.value = {
        account: row.account,
        realName: row.realName,
        role_code: row.role?.role_code || '',
        password: '',
    }
    dialogVisible.value = true
}

// 权限是否属于当前所选角色（只读展示，不可手动操作）
function hasPermInForm(code) {
    return currentRolePerms.value.has(code)
}

function saveAccount() {
    formRef.value.validate(valid => {
        if (!valid) return
        saving.value = true
        Singleton.getInstance(SysX).saveAccount({...form.value}, new AbortController().signal, () => {
        }, (r, data) => {
            saving.value = false
            if (r) {
                ElMessage.success(isEdit.value ? '保存成功' : '账号创建成功，初始密码 123456')
                dialogVisible.value = false
                loadList()
            } else {
                ElMessage.error(data?.msg || '保存失败')
            }
        })
    })
}

/* ---------------- 其他操作 ---------------- */
function resetPwd(row) {
    ElMessageBox.confirm(`确定将 ${row.account}（${row.realName}）的密码重置为 123456 吗？`, '重置密码', {type: 'warning'}).then(() => {
        Singleton.getInstance(SysX).resetPassword({account: row.account}, new AbortController().signal, () => {
        }, (r, data) => {
            if (r) ElMessage.success('已重置为 123456')
        })
    }).catch(() => {
    })
}

function toggleStatus(row) {
    const tip = row.status === 1 ? '停用' : '启用'
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
            <div class="head-desc">为每个账号分配角色，权限由角色决定，不可手动调整</div>
        </div>

        <el-card shadow="never" class="table-card">
            <div class="toolbar">
                <el-button type="primary" @click="openCreate">＋ 新建账号</el-button>
                <div class="spacer"></div>
                <el-input v-model="keyword" placeholder="搜索账号 / 姓名" clearable style="width:220px">
                    <template #prefix><span style="color:#94a3b8">🔍</span></template>
                </el-input>
            </div>

            <el-table :data="filteredList" v-loading="loading" border stripe>
                <el-table-column prop="account" label="账号" width="120">
                    <template #default="{row}"><b style="color:#2563eb">{{ row.account }}</b></template>
                </el-table-column>
                <el-table-column prop="realName" label="姓名" width="110"/>
                <el-table-column prop="role" label="角色" width="120" align="center">
                    <template #default="{row}">
                        <el-tag :type="roleTag(row.role.role_code)" size="small" effect="light">{{ row.role.role_name }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="90" align="center">
                    <template #default="{row}">
                        <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).text }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="lastLogin" label="最近登录" width="160"/>
                <el-table-column label="操作" min-width="180" fixed="right" align="center">
                    <template #default="{row}">
                        <el-button v-notSelf.readonly="row.account" link type="primary" size="small" @click="openEdit(row)">编辑/授权</el-button>
                        <el-button link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
                        <el-button v-notSelf.readonly="row.account" link :type="row.status === 1 ? 'danger' : 'success'" size="small" @click="toggleStatus(row)">
                            {{ row.status === 1 ? '停用' : '启用' }}
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
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
                        <el-form-item label="姓名" prop="realName">
                            <el-input v-model="form.realName" placeholder="真实姓名"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="角色" prop="role_code">
                            <el-select v-model="form.role_code" placeholder="请选择角色" style="width:100%">
                                <el-option v-for="r in roles" :key="r.role_code" :label="r.role_name" :value="r.role_code"/>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col v-if="!isEdit" :span="24">
                        <el-form-item label="初始密码">
                            <el-input v-model="form.password" placeholder="留空则默认 123456"/>
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
                        <div v-for="p in g.items" :key="p.code" class="perm-item"
                             :class="{checked: hasPermInForm(p.code)}">
                            <span class="checkbox" :class="{checked: hasPermInForm(p.code)}"></span>
                            <span>{{ p.name }}</span>
                            <span class="perm-code">{{ p.code }}</span>
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
        margin-bottom: 14px;

        .spacer {
            flex: 1
        }
    }

    .self-tip {
        font-size: 12px;
        color: #94a3b8;
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
