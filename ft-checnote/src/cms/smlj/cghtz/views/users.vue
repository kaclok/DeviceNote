<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import DeptPicker from "../components/DeptPicker.vue"
import {SCOPE, buildDeptPathMap, buildScopedDeptTree, matchDept, deptDisplay, deptShort, isUnknownDept, deptScopeDepts, effectiveScope, scopeText} from "../utils/DeptX.js"
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts"
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

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

/* ---------------- 数据范围（account.data_scope） ---------------- */
// 范围的唯一来源是账号行自己的 data_scope（后端 NOT NULL、新建必填），角色侧没有该字段。
// 任何有 perm:assign 的人都能给账号配范围，但只能配「不高于自己」的档位
// （编号 = 包含序）。真正的拦截在后端的集合包含校验，这里只是不让他选到明显越权的项。
const {wsCache} = useSessionCache()
const _acc = wsCache.get(ECacheType.ACCOUNT) || {}
const myScope = effectiveScope(_acc)
// 展开起点：账号自己的归属部门（后端 expandScope 的入参）
const myDeptCode = _acc.dept_code || ''
// 「本人」档的可管理面只有自己 —— 判据是账号本身（后端 canManageAccount 同口径）
const myAccount = _acc.account || ''
// 「本人」档（1）的账号可见面只有自己（后端按 account 下推），按部门筛选此时必然 0 行 —— 隐藏。
// 与 canManage 的本人档分支同口径，真正的收窄在后端 userDao.queryAll 的 scopeOwner 条件。
const onlySelf = myScope === SCOPE.SELF

// 档位下拉：编号与文案的唯一来源 = gd.json（与 DeptX.SCOPE / scopeText 同源）
const SCOPE_OPTIONS = gd.dataScope.levels.map(l => ({value: String(l.id), label: `${l.id} ${l.desc}`}))
/** 该档位能否分配给账号：全集团账号不受限，其余只能选不高于自己的档位 */
function canGrantScope(v) {
    return myScope === SCOPE.ALL || Number(v) <= myScope
}
/**
 * 本账号「可见的部门」—— 账号页两个部门选择器共用这一份，与后端 inScope 同口径：
 *   · 归属部门   ：只能把人挂到自己范围内的部门（后端 accountSave 第 (2) 条会校验）
 *   · 按部门筛选 ：筛到范围外的部门必然 0 行，不如不给选
 * deptScopeDepts 返回 null 表示不限制（全集团账号），此时退回全量字典。
 * ⚠️ 只是收窄候选，不是安全边界 —— 改前端参数绕不过后端的集合包含校验。
 */
const scopedDeptOptions = computed(
    () => deptScopeDepts(deptOptions.value, myScope, myDeptCode) ?? deptOptions.value
)

/**
 * 本账号「可见部门」的原样三态（null = 不限制）。
 * 与 scopedDeptOptions 的区别是**不做全量 fallback** —— 判定可管理范围时必须区分
 * 「不限制」和「全量恰好等于我的可见」，退回全量会把受限账号误判成不限。
 */
const myVisibleDepts = computed(() => deptScopeDepts(deptOptions.value, myScope, myDeptCode))

/**
 * 该行账号是否在我的「可管理范围」内 —— 与后端 canManageAccount 同口径：
 * 目标账号**现有**范围的展开 ⊆ 我的可见部门。
 * 只看归属部门是不够的：同一个公司里可能存在范围比操作者更大的账号
 * （典型：本公司管理员 vs 同公司挂「全集团」档的账号），归属部门在范围内、整个人的范围却更大。
 * ⚠️ 置灰只是体验，**不是安全边界** —— 后端 accountSave / resetPwd / toggle 三处各有这道闸。
 */
function canManage(row) {
    if (myScope === SCOPE.ALL) return true
    // 「本人」档（1）：可见面 = 本部门子树 ∩ 归属=我，可管理面因此只有自己。
    // 该档的授权边界与「本部门」档相同，只比部门会把同部门同事判成可管理 —— 后端的
    // resetPwd / toggle 已按「仅本人」拦住，这里置灰是为了不给出误导性的可点按钮。
    if (myScope === SCOPE.SELF) return row.account === myAccount
    const target = deptScopeDepts(deptOptions.value, effectiveScope(row), row.dept_code)
    if (target === null) return false                 // 目标是「全集团」，超出任何受限操作者
    const mine = myVisibleDepts.value || []
    return target.every(c => mine.includes(c))
}

/** 置灰原因（tooltip 文案）；可管理时返回空串，tooltip 自动不显示 */
function manageBlockReason(row) {
    return canManage(row) ? '' : `该账号的数据范围（${scopeLabel(row)}）超出你的可管理范围`
}


/** 列表行的范围文案 */
function scopeLabel(row) {
    return scopeText(effectiveScope(row))
}

/** 范围标签配色：范围越宽警示度越高 */
function scopeTagType(row) {
    return {'4': 'danger', '3': 'warning', '2': 'primary', '1': 'info'}[String(effectiveScope(row))] || 'info'
}

/* ---------------- 左侧常驻组织架构树 ---------------- */
const treeRef = ref()
const treeKeyword = ref('')

/**
 * 可见部门编码集，三态与 deptScopeDepts 一致：
 *   null = 不限制（全集团，直接用全量字典）；空集 = 一个都不可见（数据范围 fail-closed）。
 * ⚠️ 与 myVisibleDepts 一样不做全量 fallback —— 受限账号不能因为"恰好等于全量"被当成不限。
 */
const visibleDeptCodes = computed(() => {
    const vis = myVisibleDepts.value
    return vis === null ? null : new Set(vis.map(d => d.dept_code))
})

/** 虚拟根节点 key：点它 = 清空部门筛选（不带 dept_code 条件查全量） */
const ALL_DEPT_KEY = '__all__'

/**
 * 默认展开的节点：只展开虚拟根，其余全部折叠（116 个部门一次铺开会把左栏撑成长条）。
 * ⚠️ 刻意不用 defaultExpandAll：那是"永远全展开"，还会让搜索失去意义。
 * 搜索不需要额外处理 —— Element Plus 的 tree-store.filter 会对每个**可见的非叶节点**
 * 调 node.expand()（tree-store.mjs `if (node.visible && !node.isLeaf) node.expand()`），
 * 自顶向下遍历，命中项的整条祖先路径会自动展开。
 */
const DEFAULT_EXPANDED_KEYS = [ALL_DEPT_KEY]

/**
 * 树数据：全量字典按可见集剪枝 + 补回祖先链。
 * 直接拿可见集建树是不行的 —— 父节点不在集合里，每个部门都会变成根节点，看不出层级
 * （所以必须传全量字典，只把祖先节点标成 selectable=false 作层级路径）。
 * 顶部挂一个虚拟根「全部部门」，作为"取消部门筛选"的入口。
 */
const treeData = computed(() => {
    const nodes = buildScopedDeptTree(deptOptions.value, visibleDeptCodes.value)
    return [{dept_code: ALL_DEPT_KEY, dept_name: '全部部门', isAll: true, selectable: true, children: nodes}]
})

/** 树搜索：部门名 / 公司·部门全路径 / 部门编码 任一命中（父节点因有命中子节点而保留） */
function filterNode(value, data) {
    return data.isAll ? true : matchDept(data, value, deptPathMap.value)
}

watch(treeKeyword, v => {
    treeRef.value?.filter(String(v || ''))
})

function nodeTitle(data) {
    if (data.isAll) return '显示全部部门的账号'
    return data.selectable ? deptPath(data.dept_code) : '该部门不在你的数据范围内，仅作为层级路径展示'
}

/**
 * 点树节点 → 按该部门重新查询（服务端过滤，回到第一页）。
 * 祖先节点只作层级路径、不在数据范围内，筛出来必然 0 行 → 提示而不筛选。
 */
function onTreeClick(data) {
    if (!data.selectable) {
        ElMessage.warning('该部门不在你的数据范围内，仅作为层级路径展示')
        return
    }
    deptFilter.value = data.isAll ? '' : data.dept_code
    applySearch()
}

/** 清空部门筛选：与点「全部部门」等价（current-node-key 会跟着 deptFilter 回到虚拟根） */
function clearDeptFilter() {
    deptFilter.value = ''
    applySearch()
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
const form = ref({
    account: '', username: '', role_code: 'EDITOR', dept_code: '', password: '',
    data_scope: '2',   // 数据范围必填，默认 2 本部门（含下级）
})

const rules = {
    account: [{required: true, message: '请输入账号', trigger: 'blur'}],
    username: [{required: true, message: '请输入姓名', trigger: 'blur'}],
    role_code: [{required: true, message: '请选择角色', trigger: 'change'}],
    // 归属部门必填（后端强校验）。trigger 用 change：下拉选择与组织树确认都会 emit change
    dept_code: [{required: true, message: '请选择归属部门', trigger: 'change'}],
    // 数据范围必填：它是范围的唯一来源，后端没有兜底（新建时留空会被直接拒绝）
    data_scope: [{required: true, message: '请选择数据范围', trigger: 'change'}],
}

function openCreate() {
    isEdit.value = false
    form.value = {
        account: '', username: '', role_code: 'EDITOR', dept_code: '', password: '',
        data_scope: '2',
    }
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
        // 账号行上一定有值（后端 NOT NULL）；老数据若为空则留空，用户必须补一个才能保存
        data_scope: row.data_scope == null ? '' : String(row.data_scope),
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
        // 数据范围必填，传了就是"整体设置"。
        // 是否越权（超出操作者可管理范围）由后端的集合包含校验判定，前端不做安全边界的判断。
        paras.data_scope = form.value.data_scope || ''

        Singleton.getInstance(SysX).saveAccount(paras, new AbortController().signal, () => {
        }, (r, data) => {
            saving.value = false
            if (r) {
                ElMessage.success(isEdit.value ? '保存成功' : `账号${paras.account}创建成功，初始密码：${paras.password || gd.defaultPwd}`)
                dialogVisible.value = false
                loadList()
            } else {
                notifyError(data, '保存失败')
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
                notifyError(data, '密码重置失败')
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
            } else {
                notifyError(data, `${tip}失败`)
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
            <div class="head-desc">为每个账号分配角色与归属部门；功能权限由角色决定，数据范围可按账号单独指定（仅集团管理员可调）</div>
        </div>

        <div class="users-body">
            <!-- 左侧：常驻组织架构。点部门 = 按该部门筛选右侧列表（服务端过滤，回到第一页）。
                 「本人」档的可见面只有自己，筛部门必然 0 行 —— 与顶栏搜索一样直接不显示这栏。 -->
            <el-card v-if="!onlySelf" shadow="never" class="dept-aside">
                <div class="aside-head">
                    <span class="aside-title">组织架构</span>
                    <span v-if="deptFilter" class="aside-clear" @click="clearDeptFilter">清空</span>
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
                        :current-node-key="deptFilter || ALL_DEPT_KEY"
                        :filter-node-method="filterNode"
                        highlight-current
                        :default-expanded-keys="DEFAULT_EXPANDED_KEYS"
                        :expand-on-click-node="false"
                        @node-click="onTreeClick"
                    >
                        <template #default="{ data }">
                            <span class="tree-node" :class="{'node-plain': !data.selectable}" :title="nodeTitle(data)">
                                {{ data.dept_name }}
                            </span>
                        </template>
                    </el-tree>
                </div>
            </el-card>

            <el-card shadow="never" class="table-card">
                <div class="toolbar">
                    <el-button v-hasPermission="['perm:assign']" type="primary" @click="openCreate">＋ 新建账号</el-button>
                    <div class="spacer"></div>
                    <!-- 部门维度由左侧组织架构树承担（同一维度不设第二个入口）；
                         本人档：后端已把结果收窄成「只有自己」（scopeOwner 条件），搜索只是再筛一遍 -->
                    <el-input v-model="keyword" :placeholder="onlySelf ? '本人档位仅显示你自己' : '搜索账号 / 姓名'"
                              clearable style="width:220px">
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
                    <el-table-column label="数据范围" width="150" align="center">
                        <template #default="{row}">
                            <el-tag :type="scopeTagType(row)" size="small" effect="plain">{{ scopeLabel(row) }}</el-tag>
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
                            <!-- 禁用态按钮不派发鼠标事件，必须由 span 承载 tooltip（Element Plus 的既有做法） -->
                            <el-tooltip :disabled="canManage(row)" :content="manageBlockReason(row)" placement="top">
                                <span>
                                    <el-button v-notSelf.readonly="row.account" :disabled="!canManage(row)" link type="primary" size="small" @click="openEdit(row)">编辑/授权</el-button>
                                    <el-button v-notSelf.readonly="row.account" :disabled="!canManage(row)" link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
                                    <el-button v-notSelf.readonly="row.account" :disabled="!canManage(row)" link :type="row.open_status === 1 ? 'danger' : 'success'" size="small" @click="toggleStatus(row)">
                                        {{ row.open_status ? '停用' : '启用' }}
                                    </el-button>
                                </span>
                            </el-tooltip>
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
        </div>

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
                            <DeptPicker v-model="form.dept_code" :depts="scopedDeptOptions" :teleported="false" @change="onDeptChange"/>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="数据范围" prop="data_scope">
                            <el-select v-model="form.data_scope" style="width:180px">
                                <el-option v-for="o in SCOPE_OPTIONS" :key="o.value" :label="o.label"
                                           :value="o.value" :disabled="!canGrantScope(o.value)"/>
                            </el-select>
                            <span style="color:#cbd5e1;font-size:12px;margin-left:8px">
                                必填；只能分配不高于你自己（{{ scopeText(myScope) }}）的档位
                            </span>
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
    /* 与 ledger-page 保持同一字号基准：正文级 12px，下拉类控件走全局统一变量 */
    font-size: 10px;

    /* el-table 单元格、表头 */
    :deep(.el-table) {
        font-size: 12px;

        .el-table__header th {
            font-size: 12px;
        }

        .el-table__cell {
            font-size: 12px;
        }
    }

    /* el-form 标签（列表筛选区 + 编辑弹窗） */
    :deep(.el-form-item__label) {
        font-size: 12px;
    }

    /* el-checkbox：EP 默认 14px，压回 12px 与筛选区/表单其它文字一致 */
    :deep(.el-checkbox),
    :deep(.el-checkbox__label) {
        font-size: 12px;
    }

    /* 纯文本输入框 */
    :deep(.el-input__inner) {
        font-size: 12px;
    }

    /* 下拉类控件统一走全局变量（见 styles/cghtz.css）。
       日期选择器内部也是 .el-input__inner，会命中上面那条 12px，
       这里用更高特异性显式压回，保证下拉类控件字号与全局一致。 */
    :deep(.el-select__wrapper),
    :deep(.el-date-editor .el-input__inner) {
        font-size: var(--cghtz-dd-font-size);
    }

    /* el-button 按钮 */
    :deep(.el-button) {
        font-size: 12px;
    }

    /* el-pagination 分页 */
    :deep(.el-pagination) {
        font-size: 12px;

        .el-pagination__total {
            font-size: 12px;
        }
    }

    /* el-dialog 内容区：EP 默认 --el-dialog-content-font-size = 14px，压回 12px 与页面正文一致。
       弹窗默认不 teleport（appendTo='body' + appendToBody=false → Teleport disabled），
       留在 .users-page DOM 内，所以 scoped 规则能命中。 */
    :deep(.el-dialog__body) {
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

    .toolbar {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 14px;

        .spacer {
            flex: 1
        }
    }

    /* 左树 + 右表：两栏等高，左侧定宽、右侧吃掉剩余宽度。
       min-height 保证表格加载中（行数为 0）时左树不会被挤扁。 */
    .users-body {
        display: flex;
        align-items: stretch;
        gap: 12px;
        min-height: 460px;

        .dept-aside {
            width: 236px;
            flex-shrink: 0;
            /* 高度固定：卡片不随右侧表格的长短伸缩（原 min-height 只兜底、不封顶，
               表格一长整栏就跟着变高）。树内容超高时在 .tree-box 内部滚动。 */
            height: 620px;
            display: flex;
            flex-direction: column;

            /* 卡片内部纵向 flex：把固定高度内的剩余空间全部让给树区（超高即在树区内滚动） */
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

                /* 节点内容行是 flex 容器：让文本项 flex:1 + min-width:0 才能真正触发省略号 */
                :deep(.el-tree-node__content) {
                    overflow: hidden;
                }
            }
        }

        .table-card {
            flex: 1;
            min-width: 0;
        }
    }

    /* 树节点：窄栏里超长部门名省略，完整路径走 title */
    .tree-node {
        flex: 1;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        /* 祖先路径节点：不在数据范围内，不可点，灰显 */
        &.node-plain {
            color: #c0c4cc;
            cursor: not-allowed;
        }
    }

    .pager {
        margin-top: 14px;
        display: flex;
        justify-content: flex-end;
    }

    .perm-title {
        font-size: 12px;
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
                font-size: 12px;
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
                    font-size: 12px;
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
