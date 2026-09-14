<script setup lang="js">
import {nextTick} from 'vue'
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {buildDeptTree, buildDeptPathMap, matchDept} from "../utils/DeptX.js"

/**
 * 部门选择器（两种选法，同一个值）
 *
 * 1) 下拉框：可直接在框里输入文字，按「部门名 / 公司·部门全路径 / 部门编码」模糊匹配，
 *    下拉中列出所有符合条件的部门，点击即选中；不点选则值不变。
 * 2) 右侧「?」按钮：打开「组织架构」树弹窗，树同样支持搜索，
 *    适合不知道部门全名、需要顺着 集团 → 公司 → 部门 逐层找的场景。
 *
 * 数据来源：/cghtz/dept/list（后端数据源为 train.t_org 的集团组织树），
 * 登录后由 SysX.preloadDictCache 预加载并全局缓存，这里只读缓存。
 *
 * 用法：<DeptPicker v-model="form.dept_code"/>
 *
 * 说明：Element Plus 的 el-select 提供 filter-method 时会接管筛选（不再按 label 自动过滤），
 *      因此这里由 shownDepts 自行完成匹配，避免"只能按部门名匹配"的限制。
 */
const props = defineProps({
    modelValue: {type: String, default: ''},
    /** 可选：父级已加载部门字典时传入，避免重复取值 */
    depts: {type: Array, default: null},
    placeholder: {type: String, default: '输入部门名称，或点右侧按钮选择'},
    disabled: {type: Boolean, default: false},
    /**
     * 透传给 el-select 的 teleported。
     * 默认 true（面板挂到 body）。
     * ⚠️ **放在 el-dialog 里时必须传 false**：teleport 到 body 后面板脱离了弹窗的滚动上下文，
     * 弹窗内容一滚动，面板就停在原地、与输入框脱开（Element Plus 的既有表现）。
     * 传 false 后面板作为弹窗 DOM 的子节点渲染，天然随内容一起滚。
     */
    teleported: {type: Boolean, default: true},
})
const emit = defineEmits(['update:modelValue', 'change'])

/* ===================== 1. 部门字典 ===================== */
const innerDepts = ref([])

const deptList = computed(() => (props.depts && props.depts.length ? props.depts : innerDepts.value))

/**
 * 取值：优先用父级传入的 depts；否则回落到 SysX 的全局缓存。
 * SysX 内部已做「只拉一次 + 在途请求去重 + 失败可重试」，这里不再自行维护 Promise。
 */
function ensureDepts() {
    if (props.depts && props.depts.length) return
    if (innerDepts.value.length) return
    Singleton.getInstance(SysX).getDeptList(null, null, () => {
    }, (r, data) => {
        if (r) innerDepts.value = (data && data.data) || []
    })
}

/** dept_code → 部门全路径（用于下拉项副标题、树节点提示与回显兜底） */
const pathMap = computed(() => buildDeptPathMap(deptList.value))

function deptOf(code) {
    if (!code) return null
    return deptList.value.find(d => d.dept_code === code) || null
}

function deptPath(d) {
    if (!d) return ''
    return pathMap.value[d.dept_code] || d.dept_all_name || d.dept_code || ''
}

function cleanDept(d) {
    if (!d) return null
    return {
        dept_code: d.dept_code,
        dept_name: d.dept_name,
        dept_all_name: deptPath(d),
        parent_dept_code: d.parent_dept_code,
    }
}

/* ===================== 2. 下拉选择 ===================== */
const query = ref('')

const shownDepts = computed(() => {
    const all = deptList.value
    const base = query.value.trim() ? all.filter(d => matchDept(d, query.value, pathMap.value)) : all
    // 已选部门始终保留在选项里，否则下拉框回显不出部门名（会退化成显示编码）
    const cur = deptOf(props.modelValue)
    if (cur && !base.includes(cur)) return [cur, ...base]
    return base
})

/** 提供 filter-method 后由本组件负责筛选（Element Plus 不再按 label 过滤） */
function filterMethod(q) {
    query.value = q
}

function onVisibleChange(visible) {
    // 关闭下拉时清空查询词，保证下次打开看到的是完整部门列表
    if (!visible) query.value = ''
}

function onSelect(code) {
    const val = code || ''
    emit('update:modelValue', val)
    emit('change', deptOf(val))
}

/* ===================== 3. 组织架构树弹窗 ===================== */
const dialogVisible = ref(false)
const treeKeyword = ref('')
const treeRef = ref()
const checkedDept = ref(null)

const treeData = computed(() => buildDeptTree(deptList.value))

function filterNode(value, data) {
    return matchDept(data, value, pathMap.value)
}

watch(treeKeyword, v => {
    treeRef.value?.filter(String(v || ''))
})

watch(dialogVisible, async v => {
    if (!v) return
    ensureDepts()
    treeKeyword.value = ''
    checkedDept.value = cleanDept(deptOf(props.modelValue))
    await nextTick()
    // 打开时高亮当前已选部门（树已全展开，无需再定位层级）
    const code = checkedDept.value ? checkedDept.value.dept_code : null
    if (code) treeRef.value?.setCurrentKey(code)
})

function onNodeClick(data) {
    checkedDept.value = cleanDept(data)
}

function confirmTree() {
    const d = checkedDept.value
    emit('update:modelValue', d ? d.dept_code : '')
    emit('change', d)
    dialogVisible.value = false
}

function clearDept() {
    checkedDept.value = null
    emit('update:modelValue', '')
    emit('change', null)
    dialogVisible.value = false
}

onMounted(ensureDepts)
</script>

<template>
    <div class="dept-picker">
        <el-select
            class="dept-select"
            :model-value="modelValue || ''"
            filterable
            clearable
            :disabled="disabled"
            :teleported="teleported"
            :placeholder="placeholder"
            :filter-method="filterMethod"
            @update:model-value="onSelect"
            @visible-change="onVisibleChange"
        >
            <el-option v-for="d in shownDepts" :key="d.dept_code" :label="d.dept_name" :value="d.dept_code">
                <span class="opt-name">{{ d.dept_name }}</span>
                <span class="opt-path">{{ deptPath(d) }}</span>
            </el-option>
        </el-select>

        <el-tooltip content="从组织架构选择部门" placement="top">
            <el-button class="dept-tree-btn" :disabled="disabled" @click="dialogVisible = true">?</el-button>
        </el-tooltip>

        <el-dialog v-model="dialogVisible" title="选择部门（组织架构）" width="600px" append-to-body destroy-on-close>
            <el-input v-model="treeKeyword" placeholder="搜索部门名称 / 公司·部门全路径 / 编码" clearable>
                <template #prefix><span style="color:#94a3b8">🔍</span></template>
            </el-input>

            <div class="tree-box">
                <el-tree
                    ref="treeRef"
                    :data="treeData"
                    node-key="dept_code"
                    :props="{label: 'dept_name', children: 'children'}"
                    :filter-node-method="filterNode"
                    highlight-current
                    default-expand-all
                    :expand-on-click-node="false"
                    @node-click="onNodeClick"
                >
                    <template #default="{ data }">
                        <span class="tree-node">
                            <span class="tree-node-name">{{ data.dept_name }}</span>
                            <span class="tree-node-code">{{ data.dept_code }}</span>
                        </span>
                    </template>
                </el-tree>
                <div v-if="!treeData.length" class="tree-empty">暂无部门数据</div>
            </div>

            <div class="tree-selected">
                已选部门：<b>{{ checkedDept ? checkedDept.dept_name : '未选择' }}</b>
                <span v-if="checkedDept" class="tree-selected-path">{{ checkedDept.dept_all_name }}</span>
            </div>

            <template #footer>
                <el-button @click="clearDept">清空选择</el-button>
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="confirmTree">确定</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.dept-picker {
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;

    .dept-select {
        flex: 1;
        min-width: 0;
    }

    .dept-tree-btn {
        flex-shrink: 0;
        width: 30px;
        padding: 0;
        font-weight: 700;
        color: #2563eb;
    }
}

/* 下拉选项：部门名 + 全路径小字，便于区分同名部门 */
.opt-name {
    margin-right: 8px;
}

.opt-path {
    font-size: 11px;
    color: #94a3b8;
}

.tree-box {
    margin-top: 10px;
    height: 340px;
    overflow: auto;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 6px 4px;

    .tree-empty {
        text-align: center;
        color: #94a3b8;
        font-size: 13px;
        padding: 24px 0;
    }

    .tree-node {
        display: flex;
        align-items: center;
        gap: 8px;

        .tree-node-code {
            font-size: 11px;
            color: #cbd5e1;
            font-family: monospace;
        }
    }
}

.tree-selected {
    margin-top: 10px;
    font-size: 12px;
    color: #64748b;

    b {
        color: #2563eb;
    }

    .tree-selected-path {
        margin-left: 8px;
        color: #94a3b8;
    }
}
</style>
