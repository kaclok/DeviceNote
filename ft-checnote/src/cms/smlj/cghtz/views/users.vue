<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {PERM_DEFS} from "../system/MockX.js"

const loading = ref(false)
const list = ref([])

// 权限分组
const permGroups = computed(() => {
    const groups = {}
    PERM_DEFS.forEach(p => {
        if (!groups[p.group]) groups[p.group] = []
        groups[p.group].push(p)
    })
    return Object.entries(groups).map(([name, items]) => ({name, items}))
})

const AC_list = new AbortController()

onMounted(() => {
    loadList()
})

onUnmounted(() => {
    AC_list.abort()
})

function loadList() {
    loading.value = true
    Singleton.getInstance(SysX).getAccountList({}, AC_list.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) list.value = data.data || []
    })
}

function statusTag(status) {
    return status === 1 ? {type: 'success', text: '启用'} : {type: 'info', text: '停用'}
}

function roleTag(role) {
    const map = {
        '超级管理员': 'danger',
        '录入员': 'primary',
        '查看员': 'warning',
    }
    return map[role] || 'info'
}

/* ---------------- 新建账号 ---------------- */
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()
const saving = ref(false)
const form = ref({account: '', realName: '', role: 'EDITOR', password: '', auth: []})

const rules = {
    account: [{required: true, message: '请输入账号', trigger: 'blur'}],
    realName: [{required: true, message: '请输入姓名', trigger: 'blur'}],
    dept: [{required: true, message: '请输入部门', trigger: 'blur'}],
}

function openCreate() {
    isEdit.value = false
    form.value = {account: '', realName: '', role: 'EDITOR', password: '', auth: ['contract:view', 'contract:create', 'contract:update', 'contract:import']}
    dialogVisible.value = true
}

function openEdit(row) {
    isEdit.value = true
    form.value = {...row, password: ''}
    dialogVisible.value = true
}

function togglePerm(code) {
    const idx = form.value.auth.indexOf(code)
    if (idx >= 0) form.value.auth.splice(idx, 1)
    else form.value.auth.push(code)
}

function hasPermInForm(code) {
    return form.value.auth.includes(code)
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
            <div class="head-desc">为每个账号分配独立权限：登录 / 查看 / 新增 / 导入 / 导出 / 账号管理等</div>
        </div>

        <el-card shadow="never" class="table-card">
            <div class="toolbar">
                <el-button type="primary" @click="openCreate">＋ 新建账号</el-button>
                <div class="spacer"></div>
                <el-input placeholder="搜索账号 / 姓名" clearable style="width:220px">
                    <template #prefix><span style="color:#94a3b8">🔍</span></template>
                </el-input>
            </div>

            <el-table :data="list" v-loading="loading" border stripe>
                <el-table-column prop="account" label="账号" width="120">
                    <template #default="{row}"><b style="color:#2563eb">{{ row.account }}</b></template>
                </el-table-column>
                <el-table-column prop="realName" label="姓名" width="110"/>
                <el-table-column prop="dept" label="部门" width="120"/>
                <el-table-column prop="role" label="角色" width="120" align="center">
                    <template #default="{row}">
                        <el-tag :type="roleTag(row.role)" size="small" effect="light">{{ row.role }}</el-tag>
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
                        <el-button link type="primary" size="small" @click="openEdit(row)">编辑/授权</el-button>
                        <el-button link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
                        <el-button link :type="row.status === 1 ? 'danger' : 'success'" size="small" @click="toggleStatus(row)">
                            {{ row.status === 1 ? '停用' : '启用' }}
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 新建/编辑账号 + 权限分配弹窗 -->
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
                        <el-form-item label="部门" prop="dept">
                            <el-input v-model="form.dept" placeholder="如：采购部"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="角色">
                            <el-select v-model="form.role" style="width:100%">
                                <el-option label="录入员" value="录入员"/>
                                <el-option label="查看员" value="查看员"/>
                                <el-option label="超级管理员" value="超级管理员"/>
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

            <div class="perm-title">权限分配（勾选即授予）</div>
            <div class="perm-tree">
                <div v-for="g in permGroups" :key="g.name" class="perm-group">
                    <div class="perm-group-name">{{ g.name }}</div>
                    <div class="perm-items">
                        <div v-for="p in g.items" :key="p.code" class="perm-item"
                             :class="{checked: hasPermInForm(p.code)}" @click="togglePerm(p.code)">
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

        .spacer {flex: 1}
    }

    .perm-title {
        font-size: 14px;
        font-weight: 600;
        margin: 8px 0 10px;
    }

    .perm-tree {
        border: 1px solid #e2e8f0;
        border-radius: 10px;
        overflow: hidden;
        max-height: 360px;
        overflow-y: auto;

        .perm-group {
            border-bottom: 1px solid #e2e8f0;

            &:last-child {border-bottom: none}

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
                    color: #475569;
                    padding: 8px 10px;
                    border: 1px solid #e2e8f0;
                    border-radius: 8px;
                    cursor: pointer;
                    transition: all .15s;

                    &:hover {
                        border-color: #2563eb;
                        color: #2563eb;
                    }

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
