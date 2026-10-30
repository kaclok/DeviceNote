<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";
import {ElMessage, ElMessageBox} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)
const {wsCache} = useSessionCache()

const activeTab = ref('dept')

// 菜单直达 tab：/home/org/:tab
const ORG_TABS = ['dept', 'role', 'user', 'post']
const ORG_TITLES = {dept: '部门管理', role: '角色管理', user: '人员管理', post: '岗位管理'}
const pageTitle = computed(() => ORG_TITLES[activeTab.value] || '')
const props = defineProps({tab: String})
watch(() => props.tab, t => {
    if (ORG_TABS.includes(t) && t !== activeTab.value) activeTab.value = t
})
if (ORG_TABS.includes(props?.tab)) activeTab.value = props.tab

/* ============ 公共：部门平铺数据 → 树 ============ */
const deptRows = ref([])
// 子节点按 dept_code 排序，树形稳定
const deptTree = computed(() => {
    const nodes = deptRows.value.map(d => ({
        dept_code: d.dept_code, dept_name: d.dept_name,
        parent_dept_code: d.parent_dept_code, is_company: d.is_company,
        is_using: d.is_using, children: [],
    }))
    const byCode = new Map(nodes.map(n => [n.dept_code, n]))
    const roots = []
    for (const n of nodes) {
        const p = n.parent_dept_code && byCode.get(n.parent_dept_code)
        // 父缺失（含脏父值指向表外）时降级为根，保证不丢节点
        if (p && p !== n) p.children.push(n)
        else roots.push(n)
    }
    const sortRec = ls => {
        ls.sort((a, b) => a.dept_code.localeCompare(b.dept_code))
        ls.forEach(n => sortRec(n.children))
    }
    sortRec(roots)
    return roots
})

/* 部门是树：不能按行切，按「根部门」分页，整棵子树跟随（数据量小，全量在内存） */
const deptPager = pagerOf('org.dept')
const deptPageNo = ref(1)
const deptPageSize = ref(deptPager.pageSize)
const deptTotal = computed(() => deptTree.value.length)
const pagedDeptTree = computed(() => {
    const start = (deptPageNo.value - 1) * deptPageSize.value
    return deptTree.value.slice(start, start + deptPageSize.value)
})
const deptMap = computed(() => new Map(deptRows.value.map(d => [d.dept_code, d])))
function deptName(code) {
    return deptMap.value.get(code)?.dept_name || code
}

function loadDepts(signal, onAfter) {
    sys.getDeptList(null, signal, () => {}, (r, data) => {
        if (r) deptRows.value = data.data || []
        else notifyError(data, '部门列表加载失败')
        onAfter?.(r)
    })
}

/* ============ Tab1 部门管理 ============ */
const treeRef = ref()
const deptKeyword = ref('')
const deptLoading = ref(false)

let AC_dept = new AbortController()

function filterDeptNode(value, data) {
    if (!value) return true
    const kw = String(value).toLowerCase()
    return data.dept_name.toLowerCase().includes(kw) || data.dept_code.toLowerCase().includes(kw)
}

watch(deptKeyword, v => {
    treeRef.value?.filter(v)
})

const deptDialogVisible = ref(false)
const deptSaving = ref(false)
const deptFormRef = ref()
// isEdit 由 deptForm.deptCodeEditable 反推：编辑态主键锁定
const deptForm = ref({dept_code: '', dept_name: '', parent_dept_code: '', is_company: false, _edit: false})
const deptRules = {
    dept_code: [{required: true, message: '请输入部门编码', trigger: 'blur'}],
    dept_name: [{required: true, message: '请输入部门名称', trigger: 'blur'}],
}

function openDeptDialog(edit, node) {
    if (edit) {
        deptForm.value = {
            dept_code: node.dept_code, dept_name: node.dept_name,
            parent_dept_code: node.parent_dept_code || '',
            is_company: !!node.is_company, _edit: true,
        }
    } else {
        deptForm.value = {
            dept_code: '', dept_name: '',
            parent_dept_code: node ? node.dept_code : '',
            is_company: false, _edit: false,
        }
    }
    deptDialogVisible.value = true
}

function saveDept() {
    deptFormRef.value.validate(valid => {
        if (!valid) return
        deptSaving.value = true
        const f = {...deptForm.value}
        delete f._edit
        if (!f.parent_dept_code) f.parent_dept_code = null
        sys.saveDept(f, AC_dept.signal, () => {}, (r, data) => {
            deptSaving.value = false
            if (r) {
                ElMessage.success('保存成功')
                deptDialogVisible.value = false
                reloadOrg()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

function removeDept(node) {
    ElMessageBox.confirm(`确定删除部门「${node.dept_name}」吗？`, '提示', {type: 'warning'}).then(() => {
        deptLoading.value = true
        sys.deleteDept({deptCode: node.dept_code}, AC_dept.signal, () => {}, (r, data) => {
            deptLoading.value = false
            if (r) {
                ElMessage.success('删除成功')
                reloadOrg()
            } else {
                notifyError(data, '删除失败')
            }
        })
    }).catch(() => {})
}

/**
 * 部门增删改后：重算树辅助列 → 强制字典回源 → 重拉列表。
 * warpOrgs 失败不阻塞刷新（辅助列滞后一次可由下次 warpOrgs 修复），但要提示。
 */
function reloadOrg() {
    sys.warpOrgs(AC_dept.signal, () => {}, (r, data) => {
        if (!r) console.error('warpOrgs 失败', data)
    })
    sys.refreshDeptCache()
    loadDepts(AC_dept.signal)
}

/* ============ Tab2 角色管理 ============ */
const roleLoading = ref(false)
const roleList = ref([])
let AC_role = new AbortController()

/* 角色是小字典（下拉也要用全量），页面按页切，不再额外开分页接口 */
const rolePager = pagerOf('org.role')
const rolePageNo = ref(1)
const rolePageSize = ref(rolePager.pageSize)
const roleTotal = computed(() => roleList.value.length)
const pagedRoleList = computed(() => {
    const start = (rolePageNo.value - 1) * rolePageSize.value
    return roleList.value.slice(start, start + rolePageSize.value)
})

const roleDialogVisible = ref(false)
const roleSaving = ref(false)
const roleFormRef = ref()
const roleForm = ref({role_code: '', role_name: '', data_scope: '', perms: [], _edit: false})
const roleRules = {
    role_code: [{required: true, message: '请输入角色编码', trigger: 'blur'}],
    role_name: [{required: true, message: '请输入角色名称', trigger: 'blur'}],
    data_scope: [{required: true, message: '请选择数据范围', trigger: 'change'}],
}
const SCOPE_OPTIONS = gd.dataScope.levels.map(l => ({value: String(l.id), label: `${l.id} ${l.desc}`}))

function loadRoles() {
    roleLoading.value = true
    sys.getRoleList(null, AC_role.signal, () => {}, (r, data) => {
        roleLoading.value = false
        if (r) roleList.value = data.data || []
        else notifyError(data, '角色列表加载失败')
    })
}

function openRoleDialog(edit, row) {
    roleForm.value = edit
        ? {role_code: row.role_code, role_name: row.role_name, data_scope: String(row.data_scope), perms: [...(row.perms || [])], _edit: true}
        : {role_code: '', role_name: '', data_scope: '', perms: [], _edit: false}
    roleDialogVisible.value = true
}

function saveRole() {
    roleFormRef.value.validate(valid => {
        if (!valid) return
        roleSaving.value = true
        const f = {...roleForm.value}
        delete f._edit
        sys.saveRole(f, AC_role.signal, () => {}, (r, data) => {
            roleSaving.value = false
            if (r) {
                ElMessage.success('保存成功')
                roleDialogVisible.value = false
                sys.refreshRoleCache()
                loadRoles()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

function removeRole(row) {
    ElMessageBox.confirm(`确定停用角色「${row.role_name}」吗？（有账号使用时会被拒绝）`, '提示', {type: 'warning'}).then(() => {
        roleLoading.value = true
        sys.deleteRole({roleCode: row.role_code}, AC_role.signal, () => {}, (r, data) => {
            roleLoading.value = false
            if (r) {
                ElMessage.success('已停用')
                sys.refreshRoleCache()
                loadRoles()
            } else {
                notifyError(data, '停用失败')
            }
        })
    }).catch(() => {})
}

/* ============ Tab3 人员管理 ============ */
const userLoading = ref(false)
const userList = ref([])
const userKeyword = ref('')
const userTypeFilter = ref('')
const userPager = pagerOf('org.user')
const userPageNo = ref(1)
const userPageSize = ref(userPager.pageSize)
const userTotal = ref(0)
let kwTimer = null

let AC_user = new AbortController()

function loadUsers() {
    AC_user.abort()
    AC_user = new AbortController()
    userLoading.value = true
    const paras = {pageNo: userPageNo.value, pageSize: userPageSize.value}
    const kw = userKeyword.value.trim()
    if (kw) paras.kw = kw
    if (userTypeFilter.value !== '') paras.userType = userTypeFilter.value
    sys.getUserList(paras, AC_user.signal, () => {}, (r, data) => {
        userLoading.value = false
        if (r) {
            userList.value = data.data?.rows || []
            userTotal.value = data.data?.total || 0
        } else {
            notifyError(data, '人员列表加载失败')
        }
    })
}

// 输入防抖：停顿 300ms 才发请求
watch(userKeyword, () => {
    clearTimeout(kwTimer)
    kwTimer = setTimeout(() => {
        userPageNo.value = 1
        loadUsers()
    }, 300)
})
watch(userTypeFilter, () => {
    userPageNo.value = 1
    loadUsers()
})
watch([userPageNo, userPageSize], loadUsers)

const userDialogVisible = ref(false)
const userSaving = ref(false)
const userFormRef = ref()
const userForm = ref({
    account: '', username: '', pwd: '', role_code: '', dept_code: '',
    user_type: 1, contractor_id: '', _edit: false,
})
const userRules = computed(() => ({
    account: [{required: true, message: '请输入登录账号', trigger: 'blur'}],
    username: [{required: true, message: '请输入姓名', trigger: 'blur'}],
    pwd: userForm.value._edit ? [] : [{required: true, message: `请输入初始密码（留空则默认 ${gd.defaultPwd}）`, trigger: 'blur'}],
    role_code: [{required: true, message: '请选择角色', trigger: 'change'}],
    dept_code: [{required: true, message: '请选择归属部门', trigger: 'change'}],
    contractor_id: [{
        validator: (rule, value, callback) => {
            if (userForm.value.user_type === 2 && !value) callback(new Error('承包商人员必须关联承包商单位'))
            else callback()
        }, trigger: 'blur',
    }],
}))

const roleOptions = computed(() => (roleList.value || []).map(r => ({
    value: r.role_code, label: `${r.role_name}（${r.role_code}）`, disabled: !r.is_using,
})))

function userTypeLabel(t) {
    return (gd.userTypes.find(x => x.id === t) || {}).desc || t
}

function openUserDialog(edit, row) {
    userForm.value = edit
        ? {
            account: row.account, username: row.username, pwd: '',
            role_code: row.role_code, dept_code: row.dept_code,
            user_type: row.user_type || 1, contractor_id: row.contractor_id || '', _edit: true,
        }
        : {
            account: '', username: '', pwd: '', role_code: '', dept_code: '',
            user_type: 1, contractor_id: '', _edit: false,
        }
    userDialogVisible.value = true
}

function saveUser() {
    userFormRef.value.validate(valid => {
        if (!valid) return
        userSaving.value = true
        const f = {...userForm.value}
        delete f._edit
        // 编辑态不提交 pwd 字段：后端 update 分支不处理口令，置空串防误改
        if (f._edit) f.pwd = undefined
        if (!f.contractor_id) f.contractor_id = null
        sys.saveUser(f, AC_user.signal, () => {}, (r, data) => {
            userSaving.value = false
            if (r) {
                ElMessage.success('保存成功')
                userDialogVisible.value = false
                loadUsers()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

function resetUserPwd(row) {
    ElMessageBox.confirm(
        `确定将「${row.username}（${row.account}）」的密码重置为 ${gd.defaultPwd} 吗？`, '提示', {type: 'warning'}
    ).then(() => {
        sys.resetUserPwd({account: row.account}, AC_user.signal, () => {}, (r, data) => {
            if (r) ElMessage.success('已重置')
            else notifyError(data, '重置失败')
        })
    }).catch(() => {})
}

function toggleUserRow(row) {
    const next = !row.is_using
    sys.toggleUser({account: row.account, isUsing: next}, AC_user.signal, () => {}, (r, data) => {
        if (r) {
            row.is_using = next
            ElMessage.success(next ? '已启用' : '已停用')
        } else {
            notifyError(data, '操作失败')
        }
    })
}

/* 承包商下拉（人员类型=2 时选所属单位）：
 * best-effort 加载（需 cbs:contractor 权限），加载失败不阻塞——allow-create 兜底手输 */
const contractorOptions = ref([])

function loadContractorOptions() {
    sys.biz('cbs/contractor/options', {}, AC_user.signal, () => {}, (r, data) => {
        if (r) contractorOptions.value = (data.data || []).map(c => ({value: c.contractor_id, label: c.name}))
    })
}

/* ============ Tab4 岗位管理 ============ */
const postLoading = ref(false)
const postList = ref([])
const postKeyword = ref('')
const postPager = pagerOf('org.post')
const postPageNo = ref(1)
const postPageSize = ref(postPager.pageSize)
const postTotal = ref(0)
let kwPostTimer = null

let AC_post = new AbortController()

function loadPosts() {
    AC_post.abort()
    AC_post = new AbortController()
    postLoading.value = true
    const paras = {pageNo: postPageNo.value, pageSize: postPageSize.value}
    const kw = postKeyword.value.trim()
    if (kw) paras.kw = kw
    sys.biz('cbs/post/list', paras, AC_post.signal, () => {}, (r, data) => {
        postLoading.value = false
        if (r) {
            postList.value = data.data?.rows || []
            postTotal.value = data.data?.total || 0
        } else {
            notifyError(data, '岗位列表加载失败')
        }
    })
}

watch(postKeyword, () => {
    clearTimeout(kwPostTimer)
    kwPostTimer = setTimeout(() => {
        postPageNo.value = 1
        loadPosts()
    }, 300)
})
watch([postPageNo, postPageSize], loadPosts)

const postDialogVisible = ref(false)
const postSaving = ref(false)
const postFormRef = ref()
const postForm = ref({post_code: '', post_name: '', post_status: 1, remark: '', _edit: false})
const postRules = {
    post_code: [{required: true, message: '请输入岗位编码', trigger: 'blur'}],
    post_name: [{required: true, message: '请输入岗位名称', trigger: 'blur'}],
}

function openPostDialog(edit, row) {
    postForm.value = edit
        ? {post_code: row.post_code, post_name: row.post_name, post_status: row.post_status ?? 1, remark: row.remark || '', _edit: true}
        : {post_code: '', post_name: '', post_status: 1, remark: '', _edit: false}
    postDialogVisible.value = true
}

function savePost() {
    postFormRef.value.validate(valid => {
        if (!valid) return
        postSaving.value = true
        const f = {...postForm.value}
        delete f._edit
        sys.biz('cbs/post/save', f, AC_post.signal, () => {}, (r, data) => {
            postSaving.value = false
            if (r) {
                ElMessage.success('保存成功')
                postDialogVisible.value = false
                loadPosts()
            } else {
                notifyError(data, '保存失败')
            }
        })
    })
}

function removePost(row) {
    ElMessageBox.confirm(`确定删除岗位「${row.post_name}」吗？`, '提示', {type: 'warning'}).then(() => {
        postLoading.value = true
        sys.biz('cbs/post/delete', {postId: row.post_id}, AC_post.signal, () => {}, (r, data) => {
            postLoading.value = false
            if (r) {
                ElMessage.success('删除成功')
                loadPosts()
            } else {
                notifyError(data, '删除失败')
            }
        })
    }).catch(() => {})
}

/* ============ 生命周期 ============ */
onMounted(() => {
    loadDepts(AC_dept.signal)
    loadRoles()
    loadUsers()
    loadContractorOptions()
    loadPosts()
})

onUnmounted(() => {
    AC_dept.abort()
    AC_role.abort()
    AC_user.abort()
    AC_post.abort()
    clearTimeout(kwTimer)
    clearTimeout(kwPostTimer)
})
</script>

<template>
    <div class="org-page">
        <div class="page-head">
            <h2>{{ pageTitle }}</h2>
        </div>

            <!-- ==================== 部门管理 ==================== -->
            <div v-if="activeTab === 'dept'" class="pane">
                <div class="card tree-card" v-loading="deptLoading">
                    <div class="toolbar">
                        <el-input v-model="deptKeyword" placeholder="搜索部门名称/编码" clearable
                                  style="width:220px" :prefix-icon="Search"/>
                        <el-button type="primary" @click="openDeptDialog(false, null)">新增根部门</el-button>
                        <el-button :disabled="!treeRef?.getCurrentNode()" @click="openDeptDialog(false, treeRef?.getCurrentNode())">
                            新增子部门
                        </el-button>
                    </div>
                    <el-tree ref="treeRef" :data="pagedDeptTree" node-key="dept_code" default-expand-all
                             :filter-node-method="filterDeptNode" :expand-on-click-node="false"
                             :props="{label: 'dept_name'}" highlight-current>
                        <template #default="{data}">
                            <div class="tree-node">
                                <span class="node-name">{{ data.dept_name }}</span>
                                <span class="node-code">{{ data.dept_code }}</span>
                                <el-tag v-if="data.is_company" size="small" type="warning" effect="light">公司</el-tag>
                                <el-tag v-if="!data.is_using" size="small" type="info" effect="light">停用</el-tag>
                                <span class="node-actions">
                                    <el-link type="primary" @click.stop="openDeptDialog(true, data)">编辑</el-link>
                                    <el-link type="danger" @click.stop="removeDept(data)">删除</el-link>
                                </span>
                            </div>
                        </template>
                    </el-tree>
                    <el-empty v-if="!deptLoading && deptRows.length === 0" description="暂无部门，先新增根部门"/>
                    <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                                   :total="deptTotal" v-model:current-page="deptPageNo"
                                   v-model:page-size="deptPageSize" :page-sizes="deptPager.pageSizes"/>
                </div>
            </div>

            <!-- ==================== 角色管理 ==================== -->
            <div v-if="activeTab === 'role'" class="pane">
                <div class="card">
                    <div class="toolbar">
                        <el-button type="primary" @click="openRoleDialog(false)">新增角色</el-button>
                    </div>
                    <el-table :data="pagedRoleList" v-loading="roleLoading" border stripe>
                        <el-table-column prop="role_code" label="角色编码" width="140"/>
                        <el-table-column prop="role_name" label="角色名称" min-width="140"/>
                        <el-table-column label="数据范围" width="110" align="center">
                            <template #default="{row}">
                                <el-tag :type="{'4':'danger','3':'warning','2':'primary','1':'info'}[String(row.data_scope)] || 'info'"
                                        effect="light">
                                    {{ (gd.dataScope.levels.find(l => l.id === Number(row.data_scope)) || {}).desc || row.data_scope }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="权限点" min-width="260">
                            <template #default="{row}">
                                <template v-if="row.perms && row.perms.length">
                                    <el-tag v-for="p in row.perms" :key="p" size="small" effect="plain" class="perm-tag">{{ p }}</el-tag>
                                </template>
                                <span v-else class="dim">—</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="状态" width="80" align="center">
                            <template #default="{row}">
                                <el-tag :type="row.is_using ? 'success' : 'info'" effect="light" size="small">
                                    {{ row.is_using ? '启用' : '停用' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="openRoleDialog(true, row)">编辑</el-link>
                                <el-link v-if="row.is_using" type="danger" style="margin-left:10px"
                                         @click="removeRole(row)">停用</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                                   :total="roleTotal" v-model:current-page="rolePageNo"
                                   v-model:page-size="rolePageSize" :page-sizes="rolePager.pageSizes"/>
                </div>
            </div>

            <!-- ==================== 人员管理 ==================== -->
            <div v-if="activeTab === 'user'" class="pane">
                <div class="card">
                    <div class="toolbar">
                        <el-input v-model="userKeyword" placeholder="搜索账号/姓名" clearable
                                  style="width:220px" :prefix-icon="Search"/>
                        <el-select v-model="userTypeFilter" placeholder="人员类型" clearable style="width:140px">
                            <el-option v-for="t in gd.userTypes" :key="t.id" :value="t.id" :label="t.desc"/>
                        </el-select>
                        <el-button type="primary" @click="openUserDialog(false)">新增人员</el-button>
                    </div>
                    <el-table :data="userList" v-loading="userLoading" border stripe>
                        <el-table-column prop="account" label="登录账号" width="130"/>
                        <el-table-column prop="username" label="姓名" min-width="110"/>
                        <el-table-column label="角色" min-width="130">
                            <template #default="{row}">{{ row.role?.role_name || row.role_code }}</template>
                        </el-table-column>
                        <el-table-column label="归属部门" min-width="130">
                            <template #default="{row}">{{ deptName(row.dept_code) }}</template>
                        </el-table-column>
                        <el-table-column label="类型" width="100" align="center">
                            <template #default="{row}">
                                <el-tag :type="row.user_type === 2 ? 'warning' : 'primary'" effect="light" size="small">
                                    {{ userTypeLabel(row.user_type) }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column label="状态" width="90" align="center">
                            <template #default="{row}">
                                <el-switch :model-value="row.is_using" @change="toggleUserRow(row)"/>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="180" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="openUserDialog(true, row)">编辑</el-link>
                                <el-link type="warning" style="margin-left:10px" @click="resetUserPwd(row)">重置密码</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!userLoading && userList.length === 0" description="暂无人员"/>
                    <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                                   :total="userTotal" v-model:current-page="userPageNo"
                                   v-model:page-size="userPageSize" :page-sizes="userPager.pageSizes"/>
                </div>
            </div>
            <!-- ==================== 岗位管理 ==================== -->
            <div v-if="activeTab === 'post'" class="pane">
                <div class="card">
                    <div class="toolbar">
                        <el-input v-model="postKeyword" placeholder="搜索岗位名称/编码" clearable
                                  style="width:220px" :prefix-icon="Search"/>
                        <el-button type="primary" @click="openPostDialog(false)">新增岗位</el-button>
                    </div>
                    <el-table :data="postList" v-loading="postLoading" border stripe>
                        <el-table-column prop="post_code" label="岗位编码" width="160"/>
                        <el-table-column prop="post_name" label="岗位名称" min-width="160"/>
                        <el-table-column label="状态" width="100" align="center">
                            <template #default="{row}">
                                <el-tag :type="row.post_status === 1 ? 'success' : 'info'" effect="light" size="small">
                                    {{ row.post_status === 1 ? '启用' : '停用' }}
                                </el-tag>
                            </template>
                        </el-table-column>
                        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip>
                            <template #default="{row}">
                                <span v-if="row.remark">{{ row.remark }}</span>
                                <span v-else class="dim">—</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="操作" width="130" align="center" fixed="right">
                            <template #default="{row}">
                                <el-link type="primary" @click="openPostDialog(true, row)">编辑</el-link>
                                <el-link type="danger" style="margin-left:10px" @click="removePost(row)">删除</el-link>
                            </template>
                        </el-table-column>
                    </el-table>
                    <el-empty v-if="!postLoading && postList.length === 0" description="暂无岗位"/>
                    <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                                   :total="postTotal" v-model:current-page="postPageNo"
                                   v-model:page-size="postPageSize" :page-sizes="postPager.pageSizes"/>
                </div>
            </div>

        <!-- 部门编辑弹窗 -->
        <el-dialog v-model="deptDialogVisible" :title="deptForm._edit ? '编辑部门' : (deptForm.parent_dept_code ? `新增子部门（上级：${deptName(deptForm.parent_dept_code)}）` : '新增根部门')"
                   width="460px" destroy-on-close>
            <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="90px">
                <el-form-item label="部门编码" prop="dept_code">
                    <el-input v-model="deptForm.dept_code" :disabled="deptForm._edit" placeholder="唯一编码，创建后不可改"/>
                </el-form-item>
                <el-form-item label="部门名称" prop="dept_name">
                    <el-input v-model="deptForm.dept_name" placeholder="请输入部门名称"/>
                </el-form-item>
                <el-form-item label="上级部门">
                    <el-input :model-value="deptForm.parent_dept_code ? deptName(deptForm.parent_dept_code) : '（无，作为根部门）'" disabled/>
                </el-form-item>
                <el-form-item label="公司节点">
                    <el-switch v-model="deptForm.is_company" active-text="是公司节点"/>
                    <div class="form-tip">公司节点用于数据范围「本公司」档的定位，人工维护</div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="deptDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="deptSaving" @click="saveDept">保存</el-button>
            </template>
        </el-dialog>

        <!-- 角色编辑弹窗 -->
        <el-dialog v-model="roleDialogVisible" :title="roleForm._edit ? '编辑角色' : '新增角色'" width="520px" destroy-on-close>
            <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="90px">
                <el-form-item label="角色编码" prop="role_code">
                    <el-input v-model="roleForm.role_code" :disabled="roleForm._edit" placeholder="唯一编码，创建后不可改"/>
                </el-form-item>
                <el-form-item label="角色名称" prop="role_name">
                    <el-input v-model="roleForm.role_name" placeholder="请输入角色名称"/>
                </el-form-item>
                <el-form-item label="数据范围" prop="data_scope">
                    <el-select v-model="roleForm.data_scope" placeholder="请选择数据范围" style="width:100%">
                        <el-option v-for="o in SCOPE_OPTIONS" :key="o.value" :value="o.value" :label="o.label"/>
                    </el-select>
                    <div class="form-tip">决定该角色账号可见的数据面：1 本人 / 2 本部门 / 3 本公司 / 4 全集团</div>
                </el-form-item>
                <el-form-item label="权限点">
                    <el-checkbox-group v-model="roleForm.perms">
                        <el-checkbox v-for="p in gd.permDefs" :key="p" :value="p">{{ p }}</el-checkbox>
                    </el-checkbox-group>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="roleDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="roleSaving" @click="saveRole">保存</el-button>
            </template>
        </el-dialog>

        <!-- 人员编辑弹窗 -->
        <el-dialog v-model="userDialogVisible" :title="userForm._edit ? '编辑人员' : '新增人员'" width="520px" destroy-on-close>
            <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="100px">
                <el-form-item label="登录账号" prop="account">
                    <el-input v-model="userForm.account" :disabled="userForm._edit" placeholder="唯一账号，创建后不可改"/>
                </el-form-item>
                <el-form-item label="姓名" prop="username">
                    <el-input v-model="userForm.username" placeholder="请输入姓名"/>
                </el-form-item>
                <el-form-item v-if="!userForm._edit" label="初始密码" prop="pwd">
                    <el-input v-model="userForm.pwd" type="password" show-password
                              :placeholder="`留空则默认 ${gd.defaultPwd}`" autocomplete="new-password"/>
                </el-form-item>
                <el-form-item label="人员类型">
                    <el-radio-group v-model="userForm.user_type">
                        <el-radio v-for="t in gd.userTypes" :key="t.id" :value="t.id">{{ t.desc }}</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item v-if="userForm.user_type === 2" label="所属单位" prop="contractor_id">
                    <el-select v-model="userForm.contractor_id" filterable allow-create default-first-option
                               placeholder="选择或输入承包商单位" style="width:100%">
                        <el-option v-for="c in contractorOptions" :key="c.value" :value="c.value" :label="c.label"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="角色" prop="role_code">
                    <el-select v-model="userForm.role_code" placeholder="请选择角色" style="width:100%">
                        <el-option v-for="o in roleOptions" :key="o.value" :value="o.value" :label="o.label"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="归属部门" prop="dept_code">
                    <el-tree-select v-model="userForm.dept_code" :data="deptTree" node-key="dept_code"
                                    :props="{label: 'dept_name'}" check-strictly
                                    placeholder="请选择归属部门" style="width:100%"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="userDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="userSaving" @click="saveUser">保存</el-button>
            </template>
        </el-dialog>

        <!-- 岗位编辑弹窗 -->
        <el-dialog v-model="postDialogVisible" :title="postForm._edit ? '编辑岗位' : '新增岗位'" width="460px" destroy-on-close>
            <el-form ref="postFormRef" :model="postForm" :rules="postRules" label-width="90px">
                <el-form-item label="岗位编码" prop="post_code">
                    <el-input v-model="postForm.post_code" placeholder="唯一编码，启用中不允许重复"/>
                </el-form-item>
                <el-form-item label="岗位名称" prop="post_name">
                    <el-input v-model="postForm.post_name" placeholder="请输入岗位名称"/>
                </el-form-item>
                <el-form-item label="状态">
                    <el-radio-group v-model="postForm.post_status">
                        <el-radio :value="1">启用</el-radio>
                        <el-radio :value="0">停用</el-radio>
                    </el-radio-group>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input v-model="postForm.remark" type="textarea" :rows="2" placeholder="选填"/>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button @click="postDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="postSaving" @click="savePost">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.org-page {
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
    }

    .pager {
        margin-top: 14px;
        justify-content: flex-end;
    }

    .tree-card {
        max-width: 720px;
    }

    .tree-node {
        display: flex;
        align-items: center;
        gap: 8px;
        flex: 1;
        padding-right: 8px;
        min-width: 0;

        .node-name {
            font-size: 14px;
        }

        .node-code {
            font-size: 11px;
            color: #94a3b8;
        }

        .node-actions {
            margin-left: auto;
            display: none;
            gap: 8px;
        }

        &:hover .node-actions {
            display: inline-flex;
        }
    }

    .perm-tag {
        margin: 2px 4px 2px 0;
    }

    .dim {
        color: #cbd5e1;
    }

    .form-tip {
        font-size: 11px;
        color: #94a3b8;
        line-height: 1.5;
        margin-top: 4px;
    }
}
</style>
