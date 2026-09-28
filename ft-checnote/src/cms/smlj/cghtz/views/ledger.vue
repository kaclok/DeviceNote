<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js"
import {exportContractExcel, exportFinanceExcel, filtersOf, ledgerColumnsOf, columnOf, optionsOf, financeExportOf} from "../utils/ExcelX.js"
import {notifyError} from "@/framework/services/net/NwCodeMap.js"
import {useRouter, useRoute} from 'vue-router';
import {nextTick} from 'vue';
import dayjs from 'dayjs';
import {
    buildDeptPathMap,
    buildScopedDeptTree,
    matchDept,
    deptDisplay,
    deptShort,
    isUnknownDept,
    deptScopeDepts,
    effectiveScope,
    scopeText,
    deptTplBadges,
    deptTplMap,
    SCOPE
} from "../utils/DeptX.js"
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts"

/**
 * 合同台账
 * ----------------
 * 页面结构 = 左「组织架构树」+ 右「该部门的合同」。
 *
 * 唯一的入口是部门：点中某部门 → 该部门**自己绑定**的那套合同模板决定"看哪张表"
 * （绑定表 t_dept_contract_template 以 dept_code 为主键 ⇒ 一个部门至多一套模板），
 * 列表只查**该部门本身**（dept_code 精确匹配，不含下级）。右侧因此只有四种状态：
 *   未选部门 → 引导语；部门没绑模板 → 「无合同模板」（可跳配置页）；
 *   绑了但字段未登记 → 配置缺失说明；正常 → 列表（0 行 = 「无合同数据」空态）。
 *
 * 三件配置都在 data/gd.json 的 contractTables[<tb_name>] 下，前端不写死任何一张表的列：以及新增/编辑页的表单（编辑页拿到的就是当前选中的 tb）。
 *   归属部门 —— 决定"看谁的数据"。选中节点含其**所有下级**（后端 deptFilterOf 展开子树），
 *              再与账号的数据范围求交。
 * 于是"选公司 + 选模板"看到的就是：该节点及其下级、且合同属于这张表的那批数据。
 *
 * 三件配置都在 data/gd.json 的 contractTables[<tb_name>] 下，前端不写死任何一张表的列：
 *   filters     该模板的筛选条件（怎么筛）
 *   columns     该模板的全量列（筛选栏的可选值、表格列、编辑表单都从这里取）
 *   formGroups  该模板编辑表单的分组（编辑页按它排布）
 */

const router = useRouter();
const route = useRoute();

/* ---------------- 账号与数据范围 ----------------
 * 与后端 CCGHT.resolveScopeDepts 同口径（档位编号即包含序）：
 * 4 = 全集团（不限制）；3 本公司 / 2 本部门（含下级）/ 1 本人 逐级收敛。
 * ⚠️ 前端只是体验层（让用户点不到越权部门），真正的拦截在后端 contract/list：
 *   选了部门走 deptFilterOf（子树 ∩ 可见范围），只选模板走 tplHolderFilter（持有集 ∩ 可见范围）。 */
const {wsCache} = useSessionCache()
const _acc = wsCache.get(ECacheType.ACCOUNT) || {}
const dataScope = effectiveScope(_acc)
const scopeAll = dataScope === SCOPE.ALL
const myDeptCode = _acc.dept_code || ''
/** 受限数据范围（用于界面提示）。额外要求 myDeptCode 非空：账号没归属部门时后端 fail-closed 返回空集，
 *  此时提示"数据范围"是误导。 */
const scopeLimited = !scopeAll && !!myDeptCode
/**
 * 「本人」档（1）：后端把它收窄成「本部门子树 ∩ 签订人含我的姓名」。
 * 签订人筛选因此没有可筛的余地（筛别人必然 0 行，筛自己等于没筛）—— 按 filters 里的 hideForSelf 隐藏。
 */
const onlySelf = dataScope === SCOPE.SELF
/**
 * 有没有配置类权限（perm:assign）：决定"该部门没有模板"时给不给「前往配置」的出口。
 * 没有这个权限的人点了「前往配置」会被路由守卫弹回来 —— 与其让他撞一次空门，不如直接说"找管理员"。 */
const canAssign = Array.isArray(_acc.role?.perms) && _acc.role.perms.includes('perm:assign')

/* ---------------- 组织架构字典（动态数据，来自 /cghtz/dept/list） ---------------- */
const allDeptOptions = ref([])
const deptPathMap = computed(() => buildDeptPathMap(allDeptOptions.value))

/** 归属部门展示文本：命中字典 → 「公司/部门」；未命中 → 「未知部门(code)」 */
function deptPath(code) {
    return deptDisplay(deptPathMap.value, code)
}

/** 该编码未命中字典（用于把「未知部门(code)」标灰） */
function deptUnknown(code) {
    return isUnknownDept(deptPathMap.value, code)
}

/** 表格列位窄：只显示末级部门名，完整「公司/部门」路径放 tooltip */
function deptShortName(code) {
    return deptShort(deptPathMap.value, code)
}

/* ---------------- 模板清单（树徽标与「部门 → 模板」派生的数据源） ----------------
 * 模板列表来自 /cghtz/template/list（SysX 里带缓存，台账/导入/模板页共用一份）。
 * 台账不再手选模板 —— 点中的部门自己绑定的那套模板就是"看哪张表"（见 curDeptTpl）。 */
const tplList = ref([])

/* ---------------- 左侧（2）常驻组织架构树 ---------------- */
const treeRef = ref()
const treeKeyword = ref('')
const deptCode = ref('')

/**
 * 本账号「可见部门」的原样三态：null = 不限制（全集团）/ [] = 无可见部门（fail-closed）。
 * ⚠️ 不做全量 fallback —— 受限账号不能因为"恰好等于全量"被当成不限。
 */
const visibleDeptCodes = computed(() => {
    const vis = deptScopeDepts(allDeptOptions.value, dataScope, myDeptCode)
    return vis === null ? null : new Set(vis.map(d => d.dept_code))
})

/**
 * 部门 → 其持有的模板名 / 模板对象（来自 /template/list 各模板的 dept_codes 全集）。
 *
 * 与选中部门的模板派生（curDeptTpl）同源（DeptX 里同一个 tplDeptPairs）—— 点中的部门必然查得出自己的绑定模板。
 * 这条体验保证是**结构性**的（两图键集逐项相等），不靠约定。未配模板的部门不挂标签、树面保持干净。
 *
 * ⚠️ 这里取的是**全部**模板的绑定，而不是当前模板的 dept_codes：
 *    · 标签要说的是"这个部门用的是哪套模板"，与"现在看的是哪张表"是两件事；
 *    · 树上是全部可见部门：正好用这层信息区分「已配模板」与「未配模板」的部门。
 */
const tplBadgeMap = computed(() => deptTplBadges(tplList.value))
const deptTpls = computed(() => deptTplMap(tplList.value))

/** 当前选中部门绑定的模板（null = 未选部门 / 该部门没配模板 → 右侧不显示数据）。
 *  tpl_id / tb / 列 / 筛选全部由它派生 —— 绑定表以 dept_code 为主键，一个部门至多一套模板。 */
const curDeptTpl = computed(() => deptTpls.value[String(deptCode.value || '')] || null)
/** 当前模板对应的物理表名 —— 筛选/列/编辑页全部由它派生 */
const tb = computed(() => (curDeptTpl.value && curDeptTpl.value.tb_name) || '')

/** 该部门持有的模板名；'' = 未配置（不挂徽标） */
function tplBadge(code) {
    return (code && tplBadgeMap.value[String(code)]) || ''
}

/**
 * 树数据：全量字典按可见范围剪枝（不再有模板持有集收窄）+ 补回祖先链（否则父节点缺失，每个部门都会变成根节点）。
 * 祖先节点由 buildScopedDeptTree 标成 selectable=false —— 只作层级路径，不可选。
 * 默认全展开（模板上的 default-expand-all），超出栏高时由 .tree-box 滚动。
 * 搜索无需额外处理 —— Element Plus 的 tree-store.filter 会自顶向下遍历并对命中项的祖先路径自动展开。
 */
const treeData = computed(() => buildScopedDeptTree(allDeptOptions.value, visibleDeptCodes.value))

/** 树搜索：部门名 / 公司·部门全路径 / 部门编码 任一命中 */
function filterNode(value, data) {
    return matchDept(data, value, deptPathMap.value)
}

watch(treeKeyword, v => {
    treeRef.value?.filter(String(v || ''))
})

function nodeTitle(data) {
    return data.selectable ? deptPath(data.dept_code) : '该部门不在你的数据范围内，仅作为层级路径展示'
}

/**
 * 点树节点即切换台账视角；祖先节点只作层级路径，不可选。
 * 右侧"看哪张表"由该部门绑定的模板决定（见 curDeptTpl），这里只记部门。
 */
function onTreeClick(data) {
    if (!data.selectable) {
        ElMessage.warning('该部门不在你的数据范围内，仅作为层级路径展示')
        return
    }
    deptCode.value = data.dept_code
}

/** 清空选择：回到"未选部门"视角（右侧改为展示该模板下全部持有部门的合同） */
function clearDept() {
    deptCode.value = ''
    treeRef.value?.setCurrentKey(null)
}

/** 已选路径按层拆行（与导入页同构）：部门在第几层就占几行，完整路径另挂 title */
const deptPathSegments = computed(() => deptCode.value ? deptPath(deptCode.value).split('/').filter(Boolean) : [])

/** 「部门合同模板」页的入口：一个部门都没配当前模版时的出口（有无权限由调用处按 canAssign 分流） */
function goDeptTpl() {
    router.push({name: 'home_deptTpl'})
}

/* ---------------- 按模板取「表 / 列 / 筛选」配置 ----------------
 * 这一节是整页的数据形状来源：模板一变，筛选栏、表格列、编辑入口全部跟着变。 */
/**
 * 该模板的列登记项（含 system 列，如归属部门）—— 表格列与筛选可选值都从这里取。
 * 顺序 = 该模板的 col_order（t_contract_template.col_order，与导出 Excel / 下载导入模板同一份，
 * 在「部门合同模板」页拖拽排序保存）；库里没配时回落登记顺序（= 库表物理顺序，不承担展示取舍）。
 * 归属部门列不在 col_order 里（Excel 不带 system 列）⇒ 由 applyOrder 顺延在末尾，不会被丢掉。
 */
const cols = computed(() => (tb.value ? ledgerColumnsOf(tb.value, curDeptTpl.value?.col_order) : []))
/** 该模板的筛选条件（每个模板自己的那一份，写在 gd.json 的 filters 里） */
const filterDefs = computed(() => (tb.value ? filtersOf(tb.value) : []))
/**
 * 能不能渲染：选了模板，且前端登记了这张表的列。
 * gd.json 里没登记该 tb_name（运维新登记了一张表、前端还没配）时不静默显示空表，
 * 而是明确提示"字段配置未登记"—— 空表会让用户以为"这个部门就是没合同"。
 */
const tableReady = computed(() => !!tb.value && cols.value.length > 0)

/** 表格列的展示默认值（按类型给）。列自己登记了 width / minWidth / align / fixed 则以列为准。 */
const TYPE_TABLE_DEFAULTS = {
    text: {minWidth: 140},
    float: {width: 130, align: 'right'},
    int: {width: 100, align: 'center'},
    date: {width: 110},
    bool: {width: 90, align: 'center'},
}

/**
 * 表格列：直接照该模板的 col_order 展示顺序渲染（见 cols）。
 * 顺带把三个渲染期要用的东西在这里算好（可选值、宽度、对齐），模板里就不用反复解析配置：
 *   opts  该列的可选值 [{v,label,tag}]，有它就把值渲染成标签（如 财务环节 / 付款类型 / 是否挂账）
 */
const tableCols = computed(() => cols.value.map(c => {
    const d = TYPE_TABLE_DEFAULTS[c.type] || TYPE_TABLE_DEFAULTS.text
    return {
        field: c.field,
        label: c.header,
        type: c.type || 'text',
        system: !!c.system,
        widget: c.widget || '',
        opts: optionsOf(c),
        fixed: c.fixed || false,
        width: c.width ?? d.width,
        minWidth: c.width ? null : (c.minWidth ?? d.minWidth ?? null),
        align: c.align || d.align || 'left',
    }
}))

/** 归属部门列：值→「公司/部门」全路径（唯一需要特殊渲染的系统列） */
function isDeptCol(c) {
    return c.system && c.widget === 'dept'
}

/* ---------------- 「预警天数」合成列 ----------------
 * 它不是库里的列，而是由挂账日期 + 付款周期算出来的到期剩余天数（与后端 contract/list 的
 * warn_day 口径完全对齐：EXTRACT(DAY FROM (date_rk + paycycle*30 - CURRENT_DATE))）。
 * 只有三列齐全的模板才谈得上"预警"（与后端 supportsWarn 同一判据），缺列就不显示这一列。 */
const warnCapable = computed(() => !!(
    columnOf(tb.value, 'date_rk') && columnOf(tb.value, 'paycycle_dh')
    && columnOf(tb.value, 'paycycle_zb') && columnOf(tb.value, 'finish_step')
))

/** 剩余天数：1 到货款待付 → 挂账日 + 到货周期；2 质保款待付 → 挂账日 + 质保周期；其余不谈预警 */
function calcRemainingDays(row) {
    if (Number(row.finish_step) === 1 && row.date_rk) return calcRemainDay(row.date_rk, row.paycycle_dh)
    if (Number(row.finish_step) === 2 && row.date_rk) return calcRemainDay(row.date_rk, row.paycycle_zb)
    return '--'
}

function calcRemainDay(date, payCycleMonth) {
    const v = dateToDate(date)
    if (!v) return '--'
    const due = new Date(v)
    due.setDate(due.getDate() + (Number(payCycleMonth) || 0) * 30)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    return Math.floor((due.getTime() - today.getTime()) / (24 * 60 * 60 * 1000))
}

/* ---------------- 列表（服务端分页） ---------------- */
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(13)
/** 导出/导财务要"当前筛选下的全量"，显式要一个大页：不依赖 pageSize=0 的语义，行为可预期 */
const EXPORT_LIMIT = 100000
/** 服务端分页每次翻页都要发请求，需取消上一次未完成的请求，避免旧响应覆盖新响应 */
let AC_list = new AbortController()
let AC_dept = new AbortController()

/** 筛选状态三件套：文本/下拉值、区间起止、勾选项。按当前模板的 filters 重建（见 resetFilterState） */
const fstate = ref({})
const frange = ref({})
const fwarn = ref({})

/** 按当前模板的筛选定义重建状态：换模板/换表后必须重建，否则会残留上一张表的字段 */
function resetFilterState() {
    const st = {}, rg = {}, wn = {}
    filterDefs.value.forEach(f => {
        if (f.type === 'range') rg[f.field] = {begin: '', end: ''}
        else if (f.type === 'warn') wn[f.field] = false
        else st[f.field] = f.type === 'select' ? null : ''
    })
    fstate.value = st
    frange.value = rg
    fwarn.value = wn
}

/** 筛选项是否展示：hideForSelf 的项在「本人」档隐藏（见 onlySelf 的注释） */
function visibleFilter(f) {
    return !(f.hideForSelf && onlySelf)
}

/** 下拉型筛选项的可选值 = 该列登记的可选值（付款类型 / 财务环节 / 是否挂账） */
function filterOptionsOf(f) {
    return optionsOf(columnOf(tb.value, f.field))
}

/**
 * 筛选状态 → 后端 contract/list 的查询参数。
 * 约定：f_ + 列名 + _ + 比较符（后端 CCGHT.FILTER_OPS 只认 like/eq/gte/lte/neq），
 * 区间筛成 gte + lte 两条；空值一律不发 —— 后端把"没这个参数"当作"不筛这一项"。
 * tpl_id = 当前部门绑定的模板 id（后端按它反查物理表 t_contract_template.tb_name）；
 * dept_code = 左侧点中的部门，**必传**且只匹配该部门本身（不含下级）——
 * 后端 selfDeptFilter 与可见范围求交，范围外部门查不到任何行（fail-closed）。
 */
function buildParams() {
    const p = {}
    p.tpl_id = curDeptTpl.value?.id
    p.dept_code = deptCode.value
    filterDefs.value.forEach(f => {
        if (f.type === 'warn') {
            if (fwarn.value[f.field]) p.warn_day = f.value ?? 10
            return
        }
        if (f.type === 'range') {
            const r = frange.value[f.field] || {}
            if (r.begin) p[`f_${f.field}_gte`] = r.begin
            if (r.end) p[`f_${f.field}_lte`] = r.end
            return
        }
        const v = fstate.value[f.field]
        if (v !== '' && v !== null && v !== undefined) p[`f_${f.field}_${f.op || 'eq'}`] = v
    })
    return p
}

function loadList() {
    if (!tableReady.value) return
    AC_list.abort()
    AC_list = new AbortController()
    loading.value = true
    const paras = {...buildParams(), pageNum: page.value, pageSize: pageSize.value}
    Singleton.getInstance(SysX).getContractList(paras, AC_list.signal, () => {
    }, (r, data) => {
        loading.value = false
        if (r) {
            list.value = data.data.list || []
            total.value = data.data.total || 0
        } else {
            list.value = []
            total.value = 0
            notifyError(data, '查询合同列表失败')
        }
    })
}

function applyFilters() {
    if (!tableReady.value) return
    page.value = 1
    loadList()
}

function resetFilters() {
    resetFilterState()
    applyFilters()
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

/* ---------------- 单元格渲染 ---------------- */
/** 日期：兼容 PG date 字符串、ISO 串、时间戳毫秒 */
function dateStr(v) {
    const d = dateToDate(v)
    return d ? dayjs(d).format('YYYY-MM-DD') : (v == null || v === '' ? '' : String(v))
}

function dateToDate(v) {
    if (v === null || v === undefined || v === '') return null
    if (typeof v === 'number') return new Date(v)
    if (v instanceof Date) return v
    const s = String(v)
    const m = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})/)
    // 「2025-09-04」与「2025-09-04 00:00:00」都取日期部分，避免时区把日期挪一天
    if (m) return new Date(Number(m[1]), Number(m[2]) - 1, Number(m[3]))
    const d = new Date(s)
    return isNaN(d.getTime()) ? null : d
}

/** 小数：最多两位小数，千分位分隔；空值给 '—' */
function numStr(v) {
    if (v === null || v === undefined || v === '') return ''
    const n = Number(v)
    if (!Number.isFinite(n)) return String(v)
    return n.toLocaleString('zh-CN', {maximumFractionDigits: 2})
}

/** 可选值 → 显示文案（值可能来自库里的数字，也可能已是文案，两种都认） */
function optLabel(c, v) {
    if (!c.opts) return null
    const hit = c.opts.find(o => String(o.v) === String(v))
    return hit ? hit.label : null
}

/** 可选值 → el-tag 语义色（写在 gd.json 的 options[].tag 上，没配就用 info） */
function optTag(c, v) {
    if (!c.opts) return 'info'
    const hit = c.opts.find(o => String(o.v) === String(v))
    return (hit && hit.tag) || 'info'
}

/** 兜底文本：空值统一显示为「-」，避免整片单元格空白分不清"没填"和"没加载" */
function plain(v) {
    return (v === null || v === undefined || v === '') ? '-' : v
}

/* ---------------- 导出 / 跳转 ---------------- */
/** 拉当前筛选条件下的全量合同（不分页），结果交给 onSuccess */
function fetchAllFiltered({loadingMsg, onSuccess}) {
    if (!tableReady.value) return
    const paras = {...buildParams(), pageNum: 1, pageSize: EXPORT_LIMIT}
    if (loadingMsg) ElMessage.info(loadingMsg)
    Singleton.getInstance(SysX).getContractList(paras, null, () => {
    }, (r, data) => {
        if (!r) {
            notifyError(data, '拉取合同数据失败，请重试')
            return
        }
        const payload = data?.data ?? data
        const allList = Array.isArray(payload?.list) ? payload.list : (Array.isArray(payload?.rows) ? payload.rows : [])
        if (allList.length === 0) {
            ElMessage.warning('没有匹配的合同数据')
            return
        }
        onSuccess(allList)
    })
}

function doExport() {
    fetchAllFiltered({
        loadingMsg: '正在导出，请稍候...',
        onSuccess: (allList) => {
            // 导出列按**当前模板**取（Excel 视角，不含 system 列）、顺序用该模板已保存的列顺序覆盖值 ——
            // 与「下载导入模板」共用同一份配置，所以"预览 = 下载 = 导出"三处永远一致
            exportContractExcel(allList, `${curDeptTpl.value?.name || '合同台账'}_导出`, tb.value, curDeptTpl.value?.col_order)
            ElMessage.success(`已导出 ${allList.length} 条合同`)
        },
    })
}

/**
 * 「导给财务」：固定口径报表，列清单与取值方式登记在 gd.json 的 financeExport 里。
 * 因此"哪张表有这张报表"是配置说了算 —— 不再写死一张字段清单来判断按钮显不显示。
 */
const financeCapable = computed(() => !!tb.value && !!financeExportOf(tb.value))

function doExportFinance() {
    fetchAllFiltered({
        loadingMsg: '正在生成导给财务的 Excel，请稍候...',
        onSuccess: (allList) => {
            // 返回 false = 该表没登记这套报表（配置被去掉而页面还停在旧状态）
            if (!exportFinanceExcel(allList, tb.value)) {
                ElMessage.warning('该模板没有登记「导给财务」报表配置')
                return
            }
            ElMessage.success(`已导出 ${allList.length} 条导给财务数据`)
        },
    })
}

function gotoImport() {
    router.push({name: 'home_import'})
}

/** 新增：带上模板与部门 —— 编辑页据此知道"往哪张表、按哪套列"录 */
function gotoCreate() {
    router.push({name: 'home_contractEdit', params: {tb: tb.value}, query: {dept_code: deptCode.value}})
}

/** 编辑：单条读/改同样按模板路由（后端要 tb 才能定位到那张表） */
function gotoEdit(row) {
    router.push({
        name: 'home_contractEdit',
        params: {tb: tb.value, uid: row.unique_id},
        query: {dept_code: row.dept_code || deptCode.value},
    })
}

/* ---------------- 作废 ---------------- */
function removeContract(row) {
    ElMessageBox.confirm(`确定作废合同 ${row.id} 吗？作废后不可恢复。`, '作废确认', {type: 'warning'}).then(() => {
        Singleton.getInstance(SysX).deleteContract({tb: tb.value, unique_id: row.unique_id},
            new AbortController().signal, () => {
            }, (r, data) => {
                if (r) {
                    ElMessage.success('已作废')
                    loadList()
                } else {
                    notifyError(data, '作废失败')
                }
            })
    }).catch(() => {
    })
}

/* ---------------- 生命周期 ---------------- */
// 本页被 keep-alive 缓存（见 home.vue）：首次进入与每次从编辑页/导入页回来都走 onActivated。
// 组件状态（模板/部门/筛选/页码）由 keep-alive 原样保留，这里只把**数据**刷新一遍：
// 本次保存可能新增/修改了行，别人也可能改了模板的部门持有集。
onActivated(() => {
    loadTpls()
    loadDepts()
    if (tableReady.value) loadList()
})

onUnmounted(() => {
    AC_list.abort()
    AC_dept.abort()
})

// keep-alive 下离开本页不触发 unmount：中止在途请求并换新控制器，回来时才能正常发新请求
onDeactivated(() => {
    AC_list.abort()
    AC_list = new AbortController()
    AC_dept.abort()
    AC_dept = new AbortController()
})

/** 视角重建（部门或其绑定模板变化共用）：清列表/分页与筛选状态，就绪即按新口径重拉 */
function resetView() {
    AC_list.abort()
    AC_list = new AbortController()
    list.value = []
    total.value = 0
    page.value = 1
    resetFilterState()
    if (tableReady.value) {
        loadList()
    }
}

/** 部门变更 = 台账视角切换；没绑模板的部门不发请求 —— 右侧整块切到「无合同模板」说明 */
watch(deptCode, resetView, {immediate: true})

/**
 * 部门绑定的模板变化（清单首次到达 / 部门被换绑）= 换表：列与筛选全换，重建后按新表重拉。
 * 与 watch(deptCode) 在"点新部门且换模板"时会有一次重叠触发 —— loadList 开头 abort 上一次，幂等。
 */
watch(() => curDeptTpl.value?.id ?? '', resetView)

/** 合同模板列表：SysX 里带缓存，登录后已预加载过的直接命中 */
function loadTpls() {
    Singleton.getInstance(SysX).getTemplateList(null, AC_dept.signal, () => {
    }, (r, data) => {
        if (!r) return
        tplList.value = data.data || []
    })
}

// 归属部门字典：登录后已由 SysX 预加载缓存，这里命中缓存即刻返回
function loadDepts() {
    Singleton.getInstance(SysX).getDeptList(null, AC_dept.signal, () => {
    }, (r, data) => {
        if (!r) return
        allDeptOptions.value = data.data || []
        applyQueryDept()
    })
}

/**
 * URL 里带过来的预选部门（编辑页保存后回跳时带的 ?dept_code=xxx）。
 * 只在"确实落在我的可见范围内"时才采纳 —— 手改 URL 不该能把我带到越权部门上；
 * 采纳时把树上的当前节点一并高亮，用户一眼看到自己停在哪。
 */
function applyQueryDept() {
    const want = String(route.query.dept_code || '')
    if (!want || want === deptCode.value) return
    const vis = visibleDeptCodes.value
    if (vis !== null && !vis.has(want)) return
    deptCode.value = want
    nextTick(() => treeRef.value?.setCurrentKey(want))
}
</script>

<template>
    <div class="ledger-page">
        <div class="ledger-body">
            <!-- 左侧：组织架构树。点中哪个部门，右侧就按「该部门绑定的模板 + 该部门本身」展示 -->
            <el-card shadow="never" class="dept-aside">
                <div class="aside-head">
                    <span class="aside-title">归属部门</span>
                    <span v-if="deptCode" class="aside-clear" @click="clearDept">清空</span>
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
                                <!-- 持有合同模板的部门在名字后挂模板名徽标（与导入页、「部门合同模板」页同构）；
                                     未持有/清单不可信的部门保持树面干净。徽标自身另挂 title，截断时悬停可看全名 -->
                                <span v-if="tplBadge(data.dept_code)" class="node-tpl"
                                      :title="'合同模板：' + tplBadge(data.dept_code)">{{ tplBadge(data.dept_code) }}</span>
                            </span>
                        </template>
                    </el-tree>
                </div>
                <div class="aside-foot">
                    <div class="picked-line">
                        <!-- 选中即弹出绿勾、清空即缩没；勾本身也是清空入口（与导入页同构） -->
                        <Transition name="check">
                            <span v-if="deptCode" class="check-pop" role="button" tabindex="0"
                                  title="清除已选部门" aria-label="清除已选部门"
                                  @click="clearDept" @keydown.enter.prevent="clearDept" @keydown.space.prevent="clearDept">
                                <svg viewBox="0 0 24 24" width="11" height="11" aria-hidden="true">
                                    <path d="M4.5 12.6 L9.8 17.8 L19.5 7.2"/>
                                </svg>
                            </span>
                        </Transition>
                        <span class="picked-label">已选：</span>
                        <b v-if="deptCode" :title="deptPath(deptCode)"><span v-for="(seg, i) in deptPathSegments" :key="i" class="picked-seg">{{ seg }}<i v-if="i < deptPathSegments.length - 1" class="picked-slash">/</i></span></b>
                        <span v-else class="picked-empty">未选择</span>
                    </div>
                    <div v-if="scopeLimited" class="scope-line"
                         title="你的账号只能查看数据范围内的合同。需要更大范围请联系管理员调整数据范围。">
                        数据范围：{{ scopeText(dataScope) }}
                    </div>
                </div>
            </el-card>

            <!-- 右侧：未选部门引导 → 无模板说明 → 字段未登记 → 列表。每种"没内容可看"的状态各有明确说法，不给空白页 -->
            <div class="main-area">
                <!-- 未选部门：台账的入口是左侧组织树 -->
                <el-card v-if="!deptCode" shadow="never" class="empty-card">
                    <div class="empty-state">
                        <div class="empty-icon">📂</div>
                        <div class="empty-title">请点击左侧部门查看合同</div>
                        <div class="empty-desc">台账按部门查看：点中某个部门，右侧展示该部门（不含下级）绑定模板下的合同数据。</div>
                    </div>
                </el-card>

                <!-- 部门没绑模板：无法定表，明确说明并给出口（有无配置权限决定给不给入口） -->
                <el-card v-else-if="!curDeptTpl" shadow="never" class="empty-card">
                    <div class="empty-state">
                        <div class="empty-icon">🧾</div>
                        <div class="empty-title">{{ deptPath(deptCode) }} 未配置合同模板</div>
                        <div class="empty-desc">合同按模板分表存放：该部门还没有绑定模板，无法查看或录入合同。</div>
                        <el-button v-if="canAssign" link type="primary" size="small" @click="goDeptTpl">
                            前往「部门合同模板」配置 →
                        </el-button>
                        <div v-else class="empty-desc">请联系管理员为该部门配置模板</div>
                    </div>
                </el-card>

                <!-- 模板有了，但前端还没登记这张表的列（运维新登记了物理表） -->
                <el-card v-else-if="!tableReady" shadow="never" class="empty-card">
                    <div class="empty-state">
                        <div class="empty-icon">🧩</div>
                        <div class="empty-title">模板「{{ curDeptTpl.name }}」的字段配置尚未登记</div>
                        <div class="empty-desc">该模板指向数据表 {{ tb }}，但前端配置里没有它的字段清单，无法渲染列表与筛选项。请联系管理员补齐配置。</div>
                    </div>
                </el-card>

                <template v-else>
                    <!-- 筛选栏：完全由当前模板的 filters 生成（每张表的筛选项不同） -->
                    <el-card shadow="never" class="filter-card">
                        <el-form :inline="true" class="filter-form">
                            <template v-for="f in filterDefs" :key="f.field">
                                <el-form-item v-if="visibleFilter(f)" :label="f.label">
                                    <el-input v-if="f.type === 'text'" v-model="fstate[f.field]"
                                              :placeholder="f.placeholder || f.label" clearable
                                              :style="{width: (f.width || 140) + 'px'}" @keyup.enter="applyFilters"/>
                                    <el-select v-else-if="f.type === 'select'" v-model="fstate[f.field]" clearable
                                               placeholder="全部" :style="{width: (f.width || 130) + 'px'}"
                                               @change="applyFilters">
                                        <el-option v-for="o in filterOptionsOf(f)" :key="String(o.v)"
                                                   :label="o.label" :value="o.v"/>
                                    </el-select>
                                    <template v-else-if="f.type === 'range'">
                                        <el-date-picker v-model="frange[f.field].begin" type="date" placeholder="开始"
                                                        value-format="YYYY-MM-DD" :style="{width: (f.width || 130) + 'px'}"
                                                        @change="applyFilters"/>
                                        <span class="range-sep">至</span>
                                        <el-date-picker v-model="frange[f.field].end" type="date" placeholder="结束"
                                                        value-format="YYYY-MM-DD" :style="{width: (f.width || 130) + 'px'}"
                                                        @change="applyFilters"/>
                                    </template>
                                    <el-tooltip v-else-if="f.type === 'warn'" :content="f.tip || ''" placement="top">
                                        <el-checkbox v-model="fwarn[f.field]" @change="applyFilters">{{ f.text || f.label }}</el-checkbox>
                                    </el-tooltip>
                                </el-form-item>
                            </template>
                            <el-form-item>
                                <el-button type="primary" @click="applyFilters">查询</el-button>
                                <el-button @click="resetFilters">重置</el-button>
                            </el-form-item>
                        </el-form>
                    </el-card>

                    <!-- 工具栏 -->
                    <div class="toolbar">
                        <div class="toolbar-left">
                            <el-button v-hasPermission="['contract:create']" type="primary" @click="gotoCreate">＋ 新增合同</el-button>
                            <el-button v-hasPermission="['contract:import']" @click="gotoImport">📥 Excel 导入</el-button>
                            <el-button v-hasPermission="['contract:export']" @click="doExport">📤 导出 Excel</el-button>
                            <el-button v-if="financeCapable" v-hasRole="['EDITOR','ADMIN']" type="success"
                                       @click="doExportFinance">💵 导给财务</el-button>
                        </div>
                        <div class="toolbar-right">
                            <span class="total-tip">共 {{ total }} 条</span>
                        </div>
                    </div>

                    <!-- 合同列表：列随模板走（顺序 = 该模板的 col_order，可在「部门合同模板」页拖拽调整） -->
                    <el-card shadow="never" class="table-card">
                        <el-table :data="list" v-loading="loading" border stripe row-key="unique_id"
                                  show-overflow-tooltip style="width:100%">
                            <el-table-column v-for="c in tableCols" :key="c.field" :prop="c.field" :label="c.label"
                                             :width="c.width" :min-width="c.minWidth" :fixed="c.fixed" :align="c.align"
                                             :show-overflow-tooltip="!isDeptCol(c)">
                                <template #default="{row}">
                                    <!-- 归属部门：末级名 + 全路径 tooltip（与其它列的内容 tooltip 互斥，见上） -->
                                    <template v-if="isDeptCol(c)">
                                        <span v-if="!row[c.field]" class="muted">-</span>
                                        <el-tooltip v-else :content="deptPath(row[c.field])" placement="top">
                                            <span :class="{'dept-unknown': deptUnknown(row[c.field])}">{{ deptShortName(row[c.field]) }}</span>
                                        </el-tooltip>
                                    </template>
                                    <!-- 有可选值的列 → 标签（可选值与颜色都写在 gd.json 里） -->
                                    <template v-else-if="c.opts && optLabel(c, row[c.field]) !== null">
                                        <el-tag :type="optTag(c, row[c.field])" size="small" disable-transitions
                                                :class="{'contract-id': c.field === 'id'}">
                                            {{ optLabel(c, row[c.field]) }}
                                        </el-tag>
                                    </template>
                                    <template v-else-if="c.type === 'date'">{{ dateStr(row[c.field]) }}</template>
                                    <template v-else-if="c.type === 'float'">
                                        <span v-if="numStr(row[c.field])" class="money">{{ numStr(row[c.field]) }}</span>
                                        <span v-else class="muted">-</span>
                                    </template>
                                    <template v-else>
                                        <b v-if="c.field === 'id' && row[c.field]" class="contract-id">{{ row[c.field] }}</b>
                                        <span v-else :class="{'muted': plain(row[c.field]) === '-'}">{{ plain(row[c.field]) }}</span>
                                    </template>
                                </template>
                            </el-table-column>

                            <!-- 合成列：到期剩余天数（仅三列齐全的模板显示） -->
                            <el-table-column v-if="warnCapable" label="预警天数" width="81" align="center">
                                <template #default="{row}">
                                    <span :class="Number(calcRemainingDays(row)) < 10 ? 'days-overdue' : ''">
                                        {{ calcRemainingDays(row) }}
                                    </span>
                                </template>
                            </el-table-column>

                            <el-table-column v-hasPermission="'contract:op'" label="操作" width="100" fixed="right" align="center">
                                <template #default="{row}">
                                    <el-button v-hasPermission="['contract:update']" link type="primary" size="small"
                                               @click="gotoEdit(row)">编辑
                                    </el-button>
                                    <el-button v-hasPermission="['contract:delete']" link type="danger" size="small"
                                               @click="removeContract(row)">作废
                                    </el-button>
                                </template>
                            </el-table-column>

<template #empty>
                                <div class="table-empty">
                                    <div class="te-title">该部门暂无合同数据</div>
                                    <div class="te-desc">
                                        口径：仅 {{ deptPath(deptCode) }} 本部门（不含下级）。可放宽筛选条件，或通过「新增合同」「Excel 导入」补充数据。
                                    </div>
                                </div>
                            </template>
                        </el-table>

                        <div class="pager">
                            <el-pagination
                                :current-page="page"
                                :page-size="pageSize"
                                :page-sizes="[13, 30, 45, 60]"
                                :total="total"
                                layout="total, sizes, prev, pager, next, jumper"
                                background
                                @current-change="onPageChange"
                                @size-change="onSizeChange"
                            />
                        </div>
                    </el-card>
                </template>
            </div>
        </div>
    </div>
</template>

<style lang="scss" scoped>
.ledger-page {
    /* 合同台账列表全局字体缩小 2px（14px → 12px） */
    font-size: 10px;

    :deep(.el-table) {
        font-size: 12px;

        .el-table__header th {
            font-size: 12px;
        }

        .el-table__cell {
            font-size: 12px;
        }
    }

    :deep(.el-form-item__label) {
        font-size: 12px;
    }

    :deep(.el-checkbox),
    :deep(.el-checkbox__label) {
        font-size: 12px;
    }

    :deep(.el-input__inner) {
        font-size: 12px;
    }

    /* 下拉类控件统一字号（见 styles/cghtz.css）：日期选择器内部也是 .el-input__inner，
       这里用更高特异性显式压回，保证下拉类控件字号与全局一致。 */
    :deep(.el-select__wrapper),
    :deep(.el-date-editor .el-input__inner) {
        font-size: var(--cghtz-dd-font-size);
    }

    :deep(.el-button) {
        font-size: 12px;
    }

    :deep(.el-pagination) {
        font-size: 12px;

        .el-pagination__total {
            font-size: 12px;
        }
    }

    .ledger-body {
        display: flex;
        align-items: flex-start;
        gap: 12px;

        .main-area {
            flex: 1;
            min-width: 0;
        }
    }

    /* 左栏：合同模板 + 常驻组织架构。高度固定，树内容超高时在 .tree-box 内部滚动 */
    .dept-aside {
        width: 260px;
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

            :deep(.el-tree-node__content) {
                overflow: hidden;
            }
        }

        .aside-foot {
            margin-top: 10px;
            font-size: 12px;
            color: #64748b;

            .picked-line {
                display: flex;
                align-items: flex-start;
                gap: 4px;

                .picked-label {
                    flex-shrink: 0;
                    line-height: 1.45;
                }

                /* 一行一层：部门在第几层就展示几行，完整路径另挂 title */
                b {
                    flex: 1 1 auto;
                    min-width: 0;
                    display: block;
                    line-height: 1.45;
                    color: #2563eb;

                    .picked-seg {
                        display: block;
                        overflow: hidden;
                        white-space: nowrap;
                        text-overflow: ellipsis;
                    }

                    .picked-slash {
                        margin-left: 1px;
                        font-style: normal;
                        color: #94a3b8;
                    }
                }

                .picked-empty {
                    color: #e6a23c;
                }
            }

            .scope-line {
                margin-top: 4px;
                color: #94a3b8;
                cursor: help;
            }

        }

        /* 选中态绿勾：圆形底 + 白色描边勾（enter/leave 两套 animation，"啪"的手感） */
        .check-pop {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            flex-shrink: 0;
            margin-top: 1px;
            width: 16px;
            height: 16px;
            border-radius: 50%;
            background: #16a34a;
            cursor: pointer;
            outline: none;
            transition: transform .12s ease-out, background-color .15s;

            &:hover {
                background: #15803d;
                transform: scale(1.18);
            }

            &:active {
                transform: scale(.92);
            }

            &:focus-visible {
                box-shadow: 0 0 0 2px rgba(22, 163, 74, .35);
            }

            svg {
                display: block;
                transform: translateX(1px);
            }

            path {
                fill: none;
                stroke: #fff;
                stroke-width: 3;
                stroke-linecap: round;
                stroke-linejoin: round;
                stroke-dasharray: 22;
            }
        }

        .check-enter-active {
            animation: checkPopIn .34s cubic-bezier(.34, 1.56, .64, 1) both;

            path {
                animation: checkDraw .26s ease-out both;
            }
        }

        .check-leave-active {
            animation: checkPopOut .15s cubic-bezier(.4, 0, 1, 1) both;
        }

        @keyframes checkPopIn {
            0% {
                transform: scale(0) rotate(-40deg);
                opacity: 0;
            }
            60% {
                transform: scale(1.28) rotate(6deg);
                opacity: 1;
            }
            100% {
                transform: scale(1) rotate(0);
                opacity: 1;
            }
        }

        @keyframes checkPopOut {
            0% {
                transform: scale(1) rotate(0);
                opacity: 1;
            }
            100% {
                transform: scale(0) rotate(35deg);
                opacity: 0;
            }
        }

        @keyframes checkDraw {
            from {
                stroke-dashoffset: 22;
            }
            to {
                stroke-dashoffset: 0;
            }
        }
    }

    /* 树节点：部门名占满剩余宽度并省略（窄栏里超长名截断，完整路径走 title），
       持模板的部门右侧挂模板名徽标（与导入页、「部门合同模板」页同构）。
       ⚠️ text-overflow 要挂在真正承载文本的元素上 —— 挂在这个 flex 容器上对文本子节点无效。 */
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

        &.node-plain {
            color: #c0c4cc;
            cursor: not-allowed;
        }
    }

    /* 未选模板 / 未选部门 / 配置缺失：右栏的整块说明 */
    .empty-card {
        :deep(.el-card__body) {
            padding: 0;
        }

        .empty-state {
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            gap: 10px;
            min-height: 420px;
            text-align: center;
            padding: 0 24px;

            .empty-icon {
                font-size: 40px;
                line-height: 1;
            }

            .empty-title {
                font-size: 15px;
                font-weight: 600;
                color: #334155;
            }

            .empty-desc {
                font-size: 13px;
                color: #94a3b8;
                max-width: 520px;
                line-height: 1.7;
            }
        }
    }

    .filter-card {
        margin-bottom: 14px;

        :deep(.el-card__body) {
            padding: 16px 16px 0;

            .filter-form .el-form-item {
                margin-bottom: 12px;
                margin-right: 14px;
            }
        }

        .range-sep {
            margin: 0 6px;
            color: #94a3b8;
        }
    }

    .toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: 12px;

        .toolbar-right {
            display: flex;
            align-items: center;
            gap: 10px;
        }

        .total-tip {
            font-size: 12px;
            color: #64748b;
        }
    }

    .table-card {
        /* 合同编号列是整表唯一的蓝色加粗单元格：拉丁字符视觉高度压过中文，
           单独压 1px 把视觉高度拉平（蓝色 + 加粗的强调保留）。 */
        .contract-id {
            color: #2563eb;
            font-size: 11px;
        }

        .money {
            font-weight: 600;
            font-variant-numeric: tabular-nums;
        }

        .muted {
            color: #cbd5e1;
        }

        .dept-unknown {
            color: #94a3b8;
        }

        .days-overdue {
            color: #f56c6c;
            font-weight: 700;
        }

        /* 0 行的说明（el-table 的 #empty 插槽） */
        .table-empty {
            padding: 26px 0;

            .te-title {
                font-size: 13px;
                color: #64748b;
            }

            .te-desc {
                margin-top: 6px;
                font-size: 12px;
                color: #94a3b8;
            }

            .el-button {
                margin-top: 6px;
            }
        }

        .pager {
            margin-top: 14px;
            display: flex;
            justify-content: flex-end;
        }
    }
}
</style>
