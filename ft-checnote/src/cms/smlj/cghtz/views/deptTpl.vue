<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {buildDeptPathMap, buildScopedDeptTree, matchDept, deptDisplay, deptScopeDepts, effectiveScope, tplHolderCodes} from "../utils/DeptX.js"
import {downloadTemplate, templateColumns, templateExamples} from "../utils/ExcelX.js"
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts"
import {notifyError} from "@/framework/services/net/NwCodeMap.js"
import {ElMessage, ElMessageBox} from "element-plus"

/* ---------------- 组织架构字典（左树 + 部门名回显） ---------------- */
const deptOptions = ref([])
const deptPathMap = computed(() => buildDeptPathMap(deptOptions.value))

/** 部门全路径回显：命中字典 → 「公司/部门」；未命中 → 「未知部门(code)」 */
function deptPath(code) {
    return deptDisplay(deptPathMap.value, code)
}

/* ---------------- 合同模版字典 ---------------- */
// 模版是系统配置（由建表脚本/运维登记），页面只选不造；id 与 tb_name 一一对应
const templates = ref([])
const tplByKey = computed(() => {
    const m = {}
    templates.value.forEach(t => {
        if (t && t.id != null) m[String(t.id)] = t
    })
    return m
})

/* ---------------- 模板预览与下载（与"有没有选中部门"完全无关） ----------------
 * 为什么放在这一页的上半区：给部门定模板之前，管理员常常并不清楚某套模板到底有哪些列、
 * 哪些必填。原来这两个入口在「批量导入」页 —— 而那一步先得选部门才能用，
 * 等于"想看一眼模板长什么样"要先挑个部门。这里把它挪到配置动作的上方，随手可看。
 * 本区块不读 deptCode，也不写任何绑定关系；选中/清空部门都不会影响按钮的可用性。 */
const previewTplId = ref('')          // 配合 el-select 用字符串值，'' = 未选择
const previewTpl = computed(() => tplByKey.value[previewTplId.value] || null)
const previewReady = computed(() => !!previewTpl.value)
/**
 * 该模板指向的物理表是否可用（后端 template/list 下发的 importable = 表名合法、库里真有这张表）。
 * 台账读写与批量导入都已按 tpl.tb_name 路由，所以这个提示只覆盖"表被删 / 表名写错"这类运维事故。
 * 注意：这里只用来在卡片上出红字提示，**不拦下载** —— 模板本身是可离线填报、可转发的表格，
 * 拦住下载既解决不了线上的问题，又断掉唯一可用的线下途径（下载页 onDownload 处有详述）。
 * 判据写成 "!== false"：后端没下发该字段（例如命中旧缓存）时不拦，由后端 fail-closed 兜底。
 */
const previewImportable = computed(() => !previewTpl.value || previewTpl.value.importable !== false)
/**
 * 表头顺序与三份"顺序"的关系：
 *   previewAll    gd.json 里登记的顺序 —— 「恢复默认顺序」的基准
 *   savedOrder    库里已保存的覆盖顺序（t_contract_template.col_order）—— 判断有没有未保存的改动
 *   colsOrder     当前页面上展示的顺序（拖拽后即变），点保存才写回库里
 * 列清单本身仍唯一来自 gd.json；这一页改的只是**顺序**，不改列、不改表头文案。
 */
const previewAll = computed(() => (previewReady.value ? templateColumns(previewTpl.value.tb_name) : []))
const savedOrder = computed(() => (previewReady.value
    ? templateColumns(previewTpl.value.tb_name, previewTpl.value.col_order).map(c => c.field)
    : []))
const colsOrder = ref([])
/** 表格里展示的行 = 按 colsOrder 排好的列（填写说明挂在列上，重排后不会与"必填 / 示范值"错位） */
const previewColumns = computed(() => {
    const byField = new Map(previewAll.value.map(c => [c.field, c]))
    return colsOrder.value.map(f => byField.get(f)).filter(Boolean)
})
const previewRequired = computed(() => previewColumns.value.filter(c => c.required).map(c => c.header))
const previewHints = computed(() => previewColumns.value.map(c => c.hint))
// 示范行同样来自 gd.json，与下载下来的模板逐格同源 —— 页面上看到什么，下载下来的就是什么
const previewExamples = computed(() => (previewReady.value ? templateExamples(previewTpl.value.tb_name) : []))
/** 顺序是否有未保存的改动 —— 与"库里已保存的那一份"比，而不是与登记顺序比 */
const dirty = computed(() => colsOrder.value.join(',') !== savedOrder.value.join(','))
/** 该列在第 1 条示范行里的取值：一眼看到"这格该长什么样" */
function demoOf(field) {
    const ex = previewExamples.value[0]
    const v = ex ? ex[field] : ''
    return v === undefined || v === null || v === '' ? '—' : v
}
const previewOpen = ref(false)
const savingOrder = ref(false)

/** 选中模版（或模版列表刷新）后，把页面顺序同步成"库里已保存的那一份" */
function syncOrder() {
    colsOrder.value = [...savedOrder.value]
}

// immediate：模版列表是异步到的，首屏没有模版时先落一个空顺序，等列表回来再同步
watch(previewTpl, syncOrder, {immediate: true})

// 只有一套模板时不至于让上半区看起来"没东西可点"：这里给个初值。
// 与导入页的"不替用户默认选中"不同 —— 预览没有任何落库语义，默认值不会造成误解。
watch(templates, ls => {
    if (!previewTplId.value && ls && ls.length) previewTplId.value = String(ls[0].id)
})

/**
 * 下载导入模板：列按该模板引用的物理表取、顺序按该模板已保存的顺序（与「导出 Excel」同一份）；
 * 文件名带上模板名，多模板下载下来的文件不会互相覆盖。
 * 物理表不可用时**照样允许下载**：模板本身就是一张可离线填报、可发给业务同事的表格，
 * 拦住下载既解决不了线上的问题，又断掉了唯一可用的线下途径。风险改为卡片上的红字提示；
 * 真到线上导入那一步，后端 fail-closed 会整批拒绝，不会写坏数据。
 */
function onDownload() {
    if (!previewReady.value) {
        ElMessage.warning('请先选择要下载的合同模板')
        return
    }
    downloadTemplate(previewTpl.value.name, previewTpl.value.tb_name, previewTpl.value.col_order)
}

/** 在页面上先对一眼表头（并给出列数/必填清单），省一次"下完才发现拿错模板" */
function onPreview() {
    if (!previewReady.value) {
        ElMessage.warning('请先选择要预览的合同模板')
        return
    }
    previewOpen.value = true
}

/* ---------------- 表头拖拽排序 ----------------
 * 为什么用原生 HTML5 拖拽而不引第三方库：整页只需要"把一个数组元素挪到另一个位置"，
 * 为它装一个 sortable 依赖不划算（本项目至今零拖拽依赖），而原生 dragover / drop 足够表达。
 * 拖拽只改本地顺序，点「保存顺序」才落库 —— 误拖一下不至于改变所有人的导出。
 */
const dragFrom = ref(-1)
const dragOver = ref(-1)

function onDragStart(i, e) {
    dragFrom.value = i
    // 必须 setData：Firefox 下不设置数据就不会真正进入拖拽
    if (e && e.dataTransfer) {
        e.dataTransfer.effectAllowed = 'move'
        e.dataTransfer.setData('text/plain', String(i))
    }
}

function onDragOver(i) {
    dragOver.value = i
}

function endDrag() {
    dragFrom.value = -1
    dragOver.value = -1
}

/** 把第 from 行挪到第 to 行（其余顺移），只改本地顺序 */
function onDrop(i) {
    const from = dragFrom.value
    if (from < 0 || from === i) {
        endDrag()
        return
    }
    const arr = [...colsOrder.value]
    const moved = arr.splice(from, 1)[0]
    arr.splice(i, 0, moved)
    colsOrder.value = arr
    endDrag()
}

/** 回到 gd.json 的登记顺序（同样要点「保存顺序」才落库） */
function resetOrder() {
    colsOrder.value = previewAll.value.map(c => c.field)
}

/**
 * 保存顺序：保存后**对所有人生效** —— 导出 Excel 与下载导入模板都按它排。
 * 顺序与登记顺序一致时传空串，后端把覆盖值清掉、回到登记顺序（少一份无意义的覆盖数据）。
 */
function saveOrder() {
    if (!previewReady.value || !dirty.value) return
    const order = colsOrder.value.join(',')
    savingOrder.value = true
    Singleton.getInstance(SysX).saveTplColOrder({
        tpl_id: previewTpl.value.id,
        col_order: order,
    }, null, () => {
    }, (r, data) => {
        savingOrder.value = false
        if (r) {
            // 后端返回的就是落库后的值（空串会被归一成 null）；本地列表也要跟着换，
            // 否则切走再切回来又变回旧顺序 —— 缓存里那一项由 SysX 同步，这里补齐页面这一份。
            const hit = templates.value.find(x => String(x.id) === String(previewTpl.value.id))
            if (hit) hit.col_order = (data?.data?.col_order ?? null)
            ElMessage.success('列顺序已保存：导出 Excel 与下载导入模板都按这个顺序出')
        } else {
            notifyError(data, '保存列顺序失败')
        }
    })
}

/* ---------------- 部门 → 模版 绑定 ---------------- */
// 后端已按操作者数据范围收窄，前端只负责建索引，不再过滤（过滤口径只有一处）
const bindList = ref([])
const bindMap = computed(() => {
    const m = {}
    bindList.value.forEach(b => {
        if (b && b.dept_code) m[b.dept_code] = b
    })
    return m
})
/** 当前选中部门的「自身绑定」记录；null = 该部门未绑定 */
const bind = computed(() => bindMap.value[deptCode.value] || null)

/**
 * 该部门「当前生效」的模版 = 部门自己绑定的那套。
 * ⚠️ 系统**不存在**部门间继承模板的机制：上级绑定的模板不会顺延给下级，
 * 所以未绑定就是"该部门没有模板可用"，也没有"上级模板顺延下来"这一档。
 * 返回 null 表示该部门未配置。
 */
const effective = computed(() => {
    const b = bind.value
    if (!b) return null
    return {
        tpl: tplByKey.value[String(b.tpl_id)] || null,
        tplId: String(b.tpl_id),
    }
})

/** 树上节点的模版徽标文案：已绑定才显示（未绑定保持树面干净） */
function tplBadge(code) {
    const b = code && bindMap.value[code]
    if (!b) return ''
    const t = tplByKey.value[String(b.tpl_id)]
    return t ? t.name : `#${b.tpl_id}`
}

/* ---------------- 数据范围（与账号页同口径） ---------------- */
// 前端只用来收窄左树候选：受限账号看不到范围外的部门，因此也不该给它们配上模版。
// ⚠️ 不是安全边界 —— 后端 deptTpl/save|delete 各自有 perm:assign + minScope + inScope 三道闸。
const {wsCache} = useSessionCache()
const _acc = wsCache.get(ECacheType.ACCOUNT) || {}
const myScope = effectiveScope(_acc)
const myDeptCode = _acc.dept_code || ''
const myVisibleDepts = computed(() => deptScopeDepts(deptOptions.value, myScope, myDeptCode))
/** 三态：null = 不限制（全集团）/ 空集 = 一个都不可见（fail-closed），刻意不做全量 fallback */
const visibleDeptCodes = computed(() => {
    const vis = myVisibleDepts.value
    return vis === null ? null : new Set(vis.map(d => d.dept_code))
})
const treeData = computed(() => buildScopedDeptTree(deptOptions.value, treeCodes.value))

/* ---------------- 「仅看有模板」：组织树右上角的过滤开关 ----------------
 * 勾选：树上只留**持有合同模板**的部门；不勾选：退回权限与数据范围内的全部部门。
 * 默认**不勾** —— 本页的职责是给部门配模板，先看到全貌才知道哪些部门还没配
 * （导入页默认勾选是因为那边只需要"能导入的部门"，两边是有意的差异）。
 * ⚠️ 两态都落在数据范围内 —— 过滤只会更窄，不会因为勾选而看到范围外的部门；
 *    这只是体验优化，不构成安全边界。三态口径与导入页共用 DeptX.tplHolderCodes 一份实现。 */
const onlyTplDept = ref(false)
const tplLoaded = ref(false)   // 成功拿到清单才有结论；失败时三态链退回可见集
const tplFail = ref(false)
/** 全部「持有合同模板」的部门（三态：null = 清单未取到，给不出结论 → 不收窄） */
const anyHolderCodes = computed(() => (tplLoaded.value ? tplHolderCodes(templates.value) : null))

/**
 * 树的可选集。三态与 visibleDeptCodes 保持一致（null = 不限 / 空集 = 全不可选）——
 * buildScopedDeptTree 正是按这个约定解的。
 *   · 不勾选           → 可见集原样（本页原有表现形式）
 *   · 勾选但清单没取到 → 同样退回可见集（收窄失效好过把树清空）
 *   · 勾选且拿到清单   → 持有集 ∩ 可见集（取交：只会更窄，不会越权）
 */
const treeCodes = computed(() => {
    if (!onlyTplDept.value) return visibleDeptCodes.value
    const hold = anyHolderCodes.value
    if (hold === null) return visibleDeptCodes.value
    const vis = visibleDeptCodes.value
    // 可见集 null = 不限（全集团档）：持有集本身已按范围收窄，直接用
    return vis === null ? hold : new Set([...hold].filter(c => vis.has(c)))
})

/** 勾选态收窄失效（清单没取到，实际仍是全部部门）—— 静默失效会让人以为"勾了没用" */
const tplFilterOff = computed(() => onlyTplDept.value && tplFail.value)

/** 树为空时的原因：勾选态与未勾选态的成因不同，别只用一句"没有数据"打发 */
const treeEmptyText = computed(() => (onlyTplDept.value && anyHolderCodes.value !== null
    ? '你的数据范围内没有已配置合同模板的部门'
    : '你的数据范围内没有可选部门'))

/* ---------------- 左树交互 ---------------- */
const treeRef = ref()
const treeKeyword = ref('')
const deptCode = ref('')

/** 表单里的模版下拉值（字符串，配合 el-select；空串 = 未选择） */
const pickedTplId = ref('')

function filterNode(value, data) {
    return matchDept(data, value, deptPathMap.value)
}

watch(treeKeyword, v => {
    treeRef.value?.filter(String(v || ''))
})

function nodeTitle(data) {
    return data.selectable ? deptPath(data.dept_code) : '该部门不在你的数据范围内，仅作为层级路径展示'
}

function onTreeClick(data) {
    if (!data.selectable) {
        ElMessage.warning('该部门不在你的数据范围内，仅作为层级路径展示')
        return
    }
    deptCode.value = data.dept_code
    syncForm()
}

function clearDept() {
    deptCode.value = ''
    pickedTplId.value = ''
    treeRef.value?.setCurrentKey(null)
}

/** 把「当前选中部门的自身绑定」同步到表单 —— 未绑定则留空，不让用户误以为已配 */
function syncForm() {
    const b = bindMap.value[deptCode.value]
    pickedTplId.value = (b && b.tpl_id != null) ? String(b.tpl_id) : ''
}

/* ---------------- 数据加载 ---------------- */
const loading = ref(false)
const saving = ref(false)
const unbinding = ref(false)

const AC_dept = new AbortController()
const AC_tpl = new AbortController()
const AC_bind = new AbortController()

onMounted(() => {
    loadDepts()
    loadTemplates()
    loadBinds()
})

onUnmounted(() => {
    AC_dept.abort()
    AC_tpl.abort()
    AC_bind.abort()
})

function loadDepts() {
    Singleton.getInstance(SysX).getDeptList(null, AC_dept.signal, () => {
    }, (r, data) => {
        if (r) deptOptions.value = data.data || []
    })
}

function loadTemplates() {
    Singleton.getInstance(SysX).getTemplateList(null, AC_tpl.signal, () => {
    }, (r, data) => {
        if (r) {
            templates.value = data.data || []
            tplLoaded.value = true
        } else {
            // 失败不给结论：treeCodes 退回可见集，由 tplFilterOff 如实提示"勾选暂未生效"
            tplFail.value = true
        }
    })
}

function loadBinds() {
    loading.value = true
    Singleton.getInstance(SysX).getDeptTplList(null, AC_bind.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) {
            bindList.value = data.data || []
            syncForm()
        } else {
            notifyError(data, '加载部门模板绑定失败')
        }
    })
}

/* ---------------- 保存 / 解除绑定 ---------------- */
function save() {
    if (!deptCode.value) {
        ElMessage.warning('请先从左侧选择部门')
        return
    }
    if (!pickedTplId.value) {
        // 空值不等于"解除"：清空下拉只是没选，贸然提交会被后端判成非法入参
        ElMessage.warning('请选择合同模板；若要取消该部门的绑定，请点「解除绑定」')
        return
    }
    saving.value = true
    Singleton.getInstance(SysX).saveDeptTpl({
        dept_code: deptCode.value,
        tpl_id: pickedTplId.value,
    }, null, () => {
    }, (r, data) => {
        saving.value = false
        if (r) {
            const name = tplByKey.value[pickedTplId.value]?.name || ''
            ElMessage.success(`已将「${deptPath(deptCode.value)}」的模板设为 ${name}`)
            loadBinds()
        } else {
            notifyError(data, '保存失败')
        }
    })
}

/**
 * 解除绑定前的二次确认：这一步不可逆 —— 解除后该部门回到"未绑定"，
 * 导入页会直接拦下这个部门（要求先来本页配置），等于把已经定好的口径清掉了。
 */
function unbind() {
    ElMessageBox.confirm(
        `确定解除「${deptPath(deptCode.value)}」的合同模板绑定吗？解除后该部门将没有可用的模板，也无法导入数据。`,
        '解除绑定',
        {type: 'warning', confirmButtonText: '确定解除', cancelButtonText: '取消'}
    ).then(unbindConfirmed).catch(() => {
    })
}

function unbindConfirmed() {
    unbinding.value = true
    Singleton.getInstance(SysX).deleteDeptTpl({dept_code: deptCode.value}, null, () => {
    }, (r, data) => {
        unbinding.value = false
        if (r) {
            ElMessage.success('已解除绑定')
            loadBinds()
        } else {
            notifyError(data, '解除绑定失败')
        }
    })
}

/** 后端下发的是 ISO/时间戳，统一按「YYYY-MM-DD HH:mm」展示 */
function fmtTime(v) {
    if (!v) return '-'
    const d = new Date(v)
    if (Number.isNaN(d.getTime())) return String(v)
    const p = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
</script>

<template>
    <div class="dept-tpl-page">
        <div class="page-head">
            <div class="head-title">部门合同模板</div>
            <div class="head-desc">先在上方预览模板长什么样，再为部门指定默认合同模板；一个部门只能有一套模板，重复设置即覆盖</div>
        </div>

        <div class="dept-tpl-body">
            <!-- 左侧：常驻组织架构。已绑定的部门在节点右侧带模版徽标，一眼看出哪些还没配 -->
            <el-card shadow="never" class="dept-aside">
                <div class="aside-head">
                    <span class="aside-title">组织架构</span>
                    <!-- 组织架构右上角：勾选只留"持有合同模板"的部门（默认不勾，先看全貌）；不勾选为范围内全部部门 -->
                    <div class="aside-tools">
                        <el-checkbox v-model="onlyTplDept" class="aside-only-tpl"
                                     title="只显示已配置合同模板的部门（仍限制在你的权限与数据范围内）">仅看有模板</el-checkbox>
                        <span v-if="deptCode" class="aside-clear" @click="clearDept">清空</span>
                    </div>
                </div>
                <el-input v-model="treeKeyword" placeholder="搜索部门" clearable size="small" class="aside-search">
                    <template #prefix><span style="color:#94a3b8">🔍</span></template>
                </el-input>
                <!-- 勾了但清单没取到 → 收窄失效，状态与"不勾选"一致：如实说一句，别让人以为勾了没用 -->
                <div v-if="tplFilterOff" class="tree-hint">合同模板清单未取到，暂展示全部部门</div>
                <div class="tree-box">
                    <el-tree v-if="treeData.length"
                        ref="treeRef"
                        :data="treeData"
                        node-key="dept_code"
                        :props="{label: 'dept_name', children: 'children'}"
                        :current-node-key="deptCode || null"
                        :filter-node-method="filterNode"
                        highlight-current
                        default-expand-all
                        :expand-on-click-node="false"
                        @node-click="onTreeClick"
                    >
                        <template #default="{ data }">
                            <span class="tree-node" :class="{'node-plain': !data.selectable}" :title="nodeTitle(data)">
                                <span class="node-name">{{ data.dept_name }}</span>
                                <span v-if="tplBadge(data.dept_code)" class="node-tpl">{{ tplBadge(data.dept_code) }}</span>
                            </span>
                        </template>
                    </el-tree>
                    <div v-else class="tree-none">{{ treeEmptyText }}</div>
                </div>
            </el-card>

            <div class="tpl-col">
                <!-- 上半：模板预览与下载。与左树是否选中部门无关 —— 先看清模板，再决定给谁配 -->
                <el-card shadow="never" class="preview-card">
                    <div class="pv-head">
                        <span class="pv-title">模板预览与下载</span>
                        <span class="pv-sub">与部门选择无关，随时可看</span>
                    </div>
                    <div class="pv-body">
                        <el-select v-model="previewTplId" placeholder="选择要查看的合同模板" clearable style="width:270px">
                            <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="String(t.id)">
                                <span class="opt-name">{{ t.name }}</span>
                            </el-option>
                        </el-select>
                        <el-button type="primary" :disabled="!previewReady" @click="onDownload">
                            ⬇️ 下载导入模板
                        </el-button>
                        <el-button :disabled="!previewReady" @click="onPreview">👁 预览模板表头</el-button>
                    </div>
                    <div class="pv-meta">
                        <template v-if="previewTpl">
                            写入物理表 <b>{{ previewTpl.tb_name }}</b> · 共 {{ previewColumns.length }} 列 · 其中必填
                            <b>{{ previewRequired.length }}</b> 项 · 随附 <b>{{ previewExamples.length }}</b> 条示范行
                        </template>
                        <span v-else class="pv-empty">未选择模板，可先在下拉里挑一套看看</span>
                        <span v-if="previewTpl && !previewImportable" class="pv-blocked">
                            该物理表不存在或未就绪：模板可下载填写，线上导入暂不可用
                        </span>
                    </div>
                    <div v-if="previewRequired.length" class="pv-req">
                        必填字段：{{ previewRequired.join('、') }}
                    </div>
                    <div class="pv-tip">
                        下载的模板共 4 段：第 1 行字段名、第 2 行中文表头、第 3 行填写说明（每列的类型 / 是否必填 / 格式样例）、
                        第 4 行起为示范行。直接在示范行下面接着填即可，说明行与示范行导入时自动忽略
                    </div>
                </el-card>

                <!-- 下半：该部门的默认合同模版 -->
                <el-card shadow="never" class="tpl-card" v-loading="loading">
                    <div v-if="!deptCode" class="empty-hint">
                        <div class="empty-icon">🗂️</div>
                        <div class="empty-text">请从左侧组织架构中选择部门</div>
                        <div class="empty-sub">选中后可查看并调整该部门的默认合同模板</div>
                    </div>

                    <template v-else>
                        <div class="tpl-head">
                            <span class="tpl-dept" :title="deptPath(deptCode)">{{ deptPath(deptCode) }}</span>
                            <span class="tpl-code"> -- {{ deptCode }}</span>
                        </div>

                        <el-divider/>

                        <el-form label-width="96px" class="tpl-form">
                            <el-form-item label="本部门模板">
                                <el-select v-model="pickedTplId" placeholder="未绑定，请选择" clearable style="width:300px">
                                    <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="String(t.id)">
                                        <span class="opt-name">{{ t.name }}</span>
                                    </el-option>
                                </el-select>
                                <div class="form-tip">一个部门只能有一套模板，保存后直接覆盖原有设置</div>
                            </el-form-item>

                            <el-form-item label="当前生效">
                                <el-tag v-if="effective" type="success" effect="plain">
                                    {{ effective.tpl ? effective.tpl.name : `#${effective.tplId}` }}
                                </el-tag>
                                <span v-else class="none-text">未配置</span>
                                <div class="form-tip">
                                    <template v-if="effective">本部门已配置模板，可以导入数据</template>
                                    <template v-else>本部门没有配置模板，该部门目前无法导入数据（不存在向上级继承）</template>
                                </div>
                            </el-form-item>

                            <el-form-item v-if="bind" label="上次修改">
                                <span class="meta-text">{{ bind.bound_by || '-' }} · {{ fmtTime(bind.bound_at) }}</span>
                            </el-form-item>

                            <el-form-item>
                                <el-button type="primary" :loading="saving" @click="save">保存</el-button>
                                <el-button :disabled="!bind" :loading="unbinding" @click="unbind">解除绑定</el-button>
                            </el-form-item>
                        </el-form>
                    </template>
                </el-card>
            </div>
        </div>

        <!-- 模板表头预览：列清单与下载下来的 Excel 同源，先在页面上对一眼 -->
        <el-dialog v-model="previewOpen" :title="`导入模板表头 · ${previewTpl ? previewTpl.name : ''}`" width="900px">
            <div class="drag-tip">
                <span>按住任意一行可上下拖动 ⠿ ，排好后点「保存顺序」</span>
                <span class="drag-sub">保存后的顺序作用于「导出 Excel」与「下载导入模板」；导入解析按字段名认列，不受顺序影响</span>
            </div>
            <div class="drag-list">
                <div class="drag-row drag-head">
                    <span class="dg-idx">#</span>
                    <span class="dg-grip"></span>
                    <span class="dg-field">字段名</span>
                    <span class="dg-header">Excel 表头</span>
                    <span class="dg-type">类型</span>
                    <span class="dg-req">必填</span>
                    <span class="dg-hint">填写说明</span>
                    <span class="dg-demo">示范值</span>
                </div>
                <div class="drag-body">
                    <div v-for="(row, i) in previewColumns" :key="row.field"
                         class="drag-row"
                         :class="{dragging: dragFrom === i, over: dragOver === i}"
                         draggable="true"
                         @dragstart="onDragStart(i, $event)"
                         @dragover.prevent="onDragOver(i)"
                         @drop.prevent="onDrop(i)"
                         @dragend="endDrag">
                        <span class="dg-idx">{{ i + 1 }}</span>
                        <span class="dg-grip" title="按住拖动排序">⠿</span>
                        <span class="dg-field" :title="row.field">{{ row.field }}</span>
                        <span class="dg-header" :title="row.header">{{ row.header }}</span>
                        <span class="dg-type"><span class="type-tag">{{ row.typeLabel }}</span></span>
                        <span class="dg-req"><b v-if="row.required">是</b></span>
                        <span class="dg-hint" :title="row.hint">{{ row.hint }}</span>
                        <span class="dg-demo">{{ demoOf(row.field) }}</span>
                    </div>
                </div>
            </div>
            <div class="drag-foot">
                <span class="dirty-tip" :class="{on: dirty}">
                    {{ dirty ? '顺序有改动，尚未保存' : '当前顺序与已保存的一致' }}
                </span>
                <el-button size="small" :disabled="!dirty" @click="resetOrder">恢复默认顺序</el-button>
                <el-button size="small" type="primary" :loading="savingOrder" :disabled="!dirty" @click="saveOrder">
                    保存顺序
                </el-button>
            </div>
            <div style="font-size:12px;color:#94a3b8;margin-top:12px;line-height:1.8">
                下载的模板与上表逐格同源：第 1 行字段名、第 2 行中文表头、第 3 行填写说明、第 4 行起为示范行<template
                    v-if="previewExamples.length">（共 {{ previewExamples.length }} 条）</template>；说明行与示范行导入时自动忽略，请勿改动前三行。
            </div>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.dept-tpl-page {
    /* 与 ledger-page / users-page 保持同一字号基准 */
    font-size: 10px;

    :deep(.el-form-item__label) {
        font-size: 12px;
    }

    :deep(.el-input__inner) {
        font-size: 12px;
    }

    /* 下拉类控件统一走全局变量（见 styles/cghtz.css），压回 el-input__inner 那条 12px */
    :deep(.el-select__wrapper) {
        font-size: var(--cghtz-dd-font-size);
    }

    :deep(.el-button) {
        font-size: 12px;
    }

    :deep(.el-tag) {
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

    /* 左树 + 右配置：两栏等高定长，树超高时在 .tree-box 内部滚动 */
    .dept-tpl-body {
        display: flex;
        align-items: stretch;
        gap: 12px;
        min-height: 460px;

        .dept-aside {
            width: 268px;
            flex-shrink: 0;
            height: 620px;
            display: flex;
            flex-direction: column;

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

                .aside-tools {
                    display: flex;
                    align-items: center;
                    gap: 8px;
                    flex-shrink: 0;

                    /* 侧栏只有 268px：勾选框字号压到 12px 才不至于把标题挤去换行 */
                    :deep(.el-checkbox__label) {
                        font-size: 12px;
                        padding-left: 4px;
                        color: #475569;
                    }
                }

                .aside-clear {
                    font-size: 12px;
                    color: #2563eb;
                    cursor: pointer;
                }
            }

            .tree-hint {
                margin-top: 8px;
                font-size: 12px;
                color: #e6a23c;
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

                /* 节点内容行是 flex 容器：让文本项 flex:1 + min-width:0 才能真的触发省略号 */
                :deep(.el-tree-node__content) {
                    overflow: hidden;
                }

                .tree-none {
                    padding: 16px 8px;
                    text-align: center;
                    font-size: 12px;
                    color: #94a3b8;
                    line-height: 1.6;
                }
            }
        }

        /* 右列竖排两张卡：上半 = 模板预览与下载，下半 = 该部门的模板配置。
           高度与左树对齐，超出部分在各自卡片内部滚动。 */
        .tpl-col {
            flex: 1;
            min-width: 0;
            height: 620px;
            display: flex;
            flex-direction: column;
            gap: 12px;
        }

        .preview-card {
            /* 固定占右列上部约 2/5：内容是紧凑的（下拉 + 两个按钮 + 元信息），
               不给下限的话卡片会扁成一条，看着像"附属说明"而不是上半区的主体。 */
            flex: 0 0 auto;
            min-height: 248px;
            display: flex;
            flex-direction: column;

            :deep(.el-card__body) {
                flex: 1;
                display: flex;
                flex-direction: column;
                justify-content: center;
                padding: 14px 16px;
            }
        }

        .tpl-card {
            flex: 1;
            min-height: 0;

            :deep(.el-card__body) {
                padding: 16px;
                overflow: auto;
            }
        }
    }

    /* ---- 上半区：模板预览与下载 ---- */
    .preview-card {
        .pv-head {
            display: flex;
            align-items: baseline;
            gap: 8px;

            .pv-title {
                font-size: 14px;
                font-weight: 600;
                color: #0f172a;
            }

            .pv-sub {
                font-size: 12px;
                color: #94a3b8;
            }
        }

        .pv-body {
            display: flex;
            align-items: center;
            flex-wrap: wrap;
            gap: 10px;
            margin-top: 12px;
        }

        .pv-meta {
            margin-top: 10px;
            font-size: 12px;
            color: #64748b;

            b {
                color: #2563eb;
            }

            .pv-empty {
                color: #e6a23c;
            }

            /* 物理表不可用：红字阻断，与"未选择"的橙色区分开 */
            .pv-blocked {
                margin-left: 10px;
                color: #dc2626;
            }
        }

        .pv-req {
            margin-top: 6px;
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.6;
        }

        .pv-tip {
            margin-top: 6px;
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.6;
        }
    }

    /* 树节点：部门名占满剩余宽度并省略，已绑定的部门在右侧挂模版徽标 */
    .tree-node {
        flex: 1;
        min-width: 0;
        display: flex;
        align-items: center;
        gap: 6px;
        overflow: hidden;

        .node-name {
            flex: 1;
            min-width: 0;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .node-tpl {
            flex-shrink: 0;
            max-width: 96px;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
            font-size: 11px;
            line-height: 16px;
            padding: 0 5px;
            border-radius: 8px;
            color: #2563eb;
            background: #eff6ff;
            border: 1px solid #bfdbfe;
        }

        /* 祖先路径节点：不在数据范围内，不可点，灰显 */
        &.node-plain {
            color: #c0c4cc;
            cursor: not-allowed;
        }
    }

    .empty-hint {
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: #94a3b8;

        .empty-icon {
            font-size: 34px;
            line-height: 1;
            margin-bottom: 12px;
        }

        .empty-text {
            font-size: 14px;
            color: #475569;
        }

        .empty-sub {
            font-size: 12px;
            margin-top: 6px;
        }
    }

    .tpl-head {
        .tpl-dept {
            font-size: 15px;
            font-weight: 600;
            color: #0f172a;
            word-break: break-all;
        }

        .tpl-code {
            font-size: 12px;
            color: #94a3b8;
            margin-top: 2px;
        }
    }

    .tpl-form {
        .form-tip {
            font-size: 12px;
            color: #94a3b8;
            line-height: 1.5;
            margin-top: 2px;
        }

        .none-text {
            font-size: 12px;
            color: #94a3b8;
        }

        .meta-text {
            font-size: 12px;
            color: #64748b;
        }
    }

    /* 下拉选项：模版名（物理表名不在这里展示，避免下拉里出现一列技术名词） */
    .opt-name {
        float: left;
    }
}

/* ---------------- 模板表头预览弹窗：拖拽排序 ----------------
   刻意写在 .dept-tpl-page 之外：el-dialog 的内容会被 teleport 到 body，
   若挂在该祖先下面，编译出的 `.dept-tpl-page .drag-row` 永远匹配不到弹窗里的元素。 */
.drag-tip {
    display: flex;
    flex-direction: column;
    gap: 2px;
    margin-bottom: 8px;
    font-size: 12px;
    color: #64748b;

    .drag-sub {
        color: #94a3b8;
    }
}

.drag-list {
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    overflow: hidden;

    /* 表头行留在上面，列表体单独滚动 */
    .drag-body {
        max-height: 420px;
        overflow: auto;
    }

    .drag-row {
        display: grid;
        grid-template-columns: 30px 20px 132px 116px 60px 44px minmax(140px, 1fr) 124px;
        align-items: center;
        gap: 0 8px;
        padding: 6px 10px;
        font-size: 12px;
        line-height: 1.5;
        background: #fff;
        border-bottom: 1px solid #f1f5f9;
        cursor: grab;

        &:last-child {
            border-bottom: none;
        }

        &.drag-head {
            background: #f8fafc;
            color: #475569;
            font-weight: 600;
            cursor: default;
        }

        /* 被拖起的那一行淡出、落点行整行高亮 —— 用户不必猜"会插到哪一行" */
        &.dragging {
            opacity: .4;
        }

        &.over {
            background: #eff6ff;
            box-shadow: inset 0 0 0 1px #bfdbfe;
        }

        .dg-idx {
            color: #94a3b8;
            text-align: center;
        }

        .dg-grip {
            color: #cbd5e1;
            text-align: center;
            letter-spacing: -2px;
        }

        /* 窄列统一省略号，完整内容走 title */
        .dg-field,
        .dg-header,
        .dg-hint,
        .dg-demo {
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
        }

        .dg-field {
            color: #0f172a;
        }

        .dg-hint,
        .dg-demo {
            color: #64748b;
        }

        .dg-req b {
            color: #dc2626;
        }
    }
}

/* 预览弹窗里的类型标签（与 .drag-list 同级放在顶层，理由同上） */
.type-tag {
    display: inline-block;
    padding: 0 6px;
    border-radius: 8px;
    font-size: 11px;
    line-height: 18px;
    color: #2563eb;
    background: #eff6ff;
    border: 1px solid #bfdbfe;
}

.drag-foot {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 10px;

    .dirty-tip {
        margin-right: auto;
        font-size: 12px;
        color: #94a3b8;

        &.on {
            color: #e6a23c;
        }
    }
}
</style>
