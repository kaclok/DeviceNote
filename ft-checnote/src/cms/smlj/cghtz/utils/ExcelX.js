import * as XLSX from 'xlsx'
import gd from '../data/gd.json'

/**
 * 合同台账 - Excel 导入/导出工具（v5 - 2026-09-15）
 * 导入：按英文字段名读取（Excel 第 1 行中文表头，第 2 行英文字段名，第 3 行忽略，从第 4 行开始读数据）
 * 导出：中文表头 + 英文字段名两行表头，数据从第 3 行开始
 *
 * v5 变更：sign_type（签订方式）在库中已是 varchar 自由文本，
 *         Excel 与前后端统一使用中文原文，不再做 int 编码互转。
 *
 * v6 变更：类型字典收敛为「文字 text / 小数 float / 整数 int / 日期 date / 是-否 bool」，
 *         可选值改为列上的 options:[{v,label}]（v = 入库值）。
 *         同时本文件成为「模板配置」的唯一读取入口：台账筛选栏读 filtersOf()，
 *         编辑表单读 formGroupsOf()/formColumnsOf()，Excel 三件套读 columnsOf()（不含 system 列）。
 *
 * v7 变更：导出与导入解析不再按**字段名**特判（付款类型、财务环节都曾写死在代码里），
 *         改由列上的 type + options 驱动 —— 列怎么配，两个方向就怎么转，新增表零改动。
 *         两者都接受 tbName：模板指向哪张表，就按哪张表的列走。
 *
 * v8 变更：① 列顺序可被模版级覆盖 —— order 参数接受 t_contract_template.col_order
 *            （逗号分隔字段名），导出与导入模板都按它排；不传 / 传空则用 gd.json 的登记顺序。
 *         ② 「导给财务」的列清单与取值方式整块搬进 gd.json 的 financeExport（见下），
 *            本文件只提供几种通用取值 kind，不再写死任何列名与文案。
 */

/* 是/否 → boolean，空值默认 false */
const YES_NO_TO_BOOL = (v) => {
    if (v === true || v === 1) return true
    if (v === false || v === 0) return false
    const s = String(v ?? '').trim()
    if (!s) return false
    const low = s.toLowerCase()
    if (s === '是' || low === 'y' || low === 'true' || s === '√' || s === '✓') return true
    if (s === '否' || low === 'n' || low === 'false' || s === '×' || s === '✗') return false
    return false  // 无法识别时默认 false
}
const BOOL_TO_YES_NO = (b) => {
    if (b === true) return '是'
    if (b === false) return '否'
    return ''
}
/**
 * 合同表字段清单的唯一来源 = data/gd.json 的 contractTables（按物理表名索引入口）。
 *
 * 为什么不写在本文件里：同一份列清单有四个使用者 —— 导出列 / 导入模板列 / 导入解析列 / 模板预览表头，
 * 而"哪张物理表有哪些列"是**表结构知识**，不该埋在工具函数里。放进 gd.json 后按 tb_name 取，
 * 模板（`t_contract_template.tb_name`）一换，四处一起换。
 * 表头文案 = PG 列注释（col_description）精简后的结果：注释里有的太长
 * （如「入库日期(标识是否已入库),即挂账日期」）、有的带冗余单位（如「(单位:月)」），
 * 这里沿用用户已在用的简称；改文案前先确认现有 Excel 文件对得上（导入按 field 名匹配，不受影响）。
 *
 * ⚠️ 系统字段刻意不进本表：
 *   · creator     录入人，后端按登录态写入 —— 导出（含导给财务）不带录入人信息
 *   · dept_code   归属部门，导入时由页面上的部门选择器逐行注入（见 import.vue）
 *   · unique_id / open_status   后端主键与逻辑删除标记
 */
/**
 * 默认物理表：未显式传 tbName 时的回落值（= 标准采购合同表）。
 * 台账（列表/编辑/读取）与**批量导入**都已按 t_contract_template.tb_name 路由，
 * 所以各处都应显式传 tbName；这里只是"没传"时的兜底，保证老调用方零改动。
 */
const IMPORT_TABLE = 't_contract'

/** 取某张物理表的登记项（columns / examples / filters / formGroups）；
 *  未登记该表时回落到当前导入表，保证老调用方零改动 */
export function tableOf(tbName) {
    const all = gd.contractTables || {}
    return all[tbName] || all[IMPORT_TABLE] || {}
}

/**
 * 该表的**全部**列（含 system 列，如归属部门 dept_code）—— 台账筛选栏与编辑表单按它渲染。
 * system 列只活在页面上：Excel 的导出/模板/解析都不带它（合同归属由导入页逐行注入）。
 */
export function formColumnsOf(tbName) {
    return tableOf(tbName).columns || []
}

/**
 * 列顺序的覆盖值归一化：接受逗号串（库里 t_contract_template.col_order）或数组。
 * 空 / 非法一律返回空数组 = 不做覆盖，按 gd.json 的登记顺序。
 */
export function orderListOf(v) {
    if (Array.isArray(v)) return v.map(x => String(x ?? '').trim()).filter(Boolean)
    if (typeof v === 'string') return v.split(',').map(x => x.trim()).filter(Boolean)
    return []
}

/**
 * 按覆盖顺序重排列定义。order 里没提到的列按原相对顺序排在最后 ——
 * "顺而不丢"：顺序配置不完整（或库里后来新增了列）时，多出来的列只会在末尾出现，不会消失。
 * 用显式下标做次级比较（不是依赖 sort 的稳定性），保证同一份配置每次排出来都一样。
 */
function applyOrder(defs, order) {
    const list = orderListOf(order)
    if (!list.length) return defs
    const rank = new Map(list.map((f, i) => [f, i]))
    const at = d => (rank.has(d.field) ? rank.get(d.field) : Number.MAX_SAFE_INTEGER)
    return defs
        .map((d, i) => ({d, i}))
        .sort((a, b) => (at(a.d) === at(b.d) ? a.i - b.i : at(a.d) - at(b.d)))
        .map(x => x.d)
}

/**
 * Excel 视角的列（导出 / 导入模板 / 导入解析三处共用）= 全部列去掉 system 列。
 * @param order 可选的列顺序覆盖值（见 applyOrder）；不传则按 gd.json 的登记顺序
 */
function columnsOf(tbName, order) {
    return applyOrder(formColumnsOf(tbName).filter(c => !c.system), order)
}

/** 按字段名取某表的一列登记项；找不到返回 null（调用方自己决定怎么兜底，不抛错） */
export function columnOf(tbName, field) {
    return formColumnsOf(tbName).find(c => c.field === field) || null
}

/**
 * 列的可选值归一化为 [{v, label}]（v = 入库值，label = 显示文案）。
 * gd.json 统一写成对象；这里兜底裸值（v 与 label 相同），免得一处写错就让页面崩掉。
 */
export function optionsOf(col) {
    const src = (col && col.options) || null
    if (!src || !src.length) return null
    return src.map(o => (o && typeof o === 'object')
        ? {v: o.v, label: o.label == null ? String(o.v) : String(o.label), tag: o.tag || ''}
        : {v: o, label: String(o), tag: ''})
}

/**
 * 入库值 → 显示文案（导出用）。认不出来返回 null，由调用方按类型兜底 —— 不猜。
 * 比对用字符串：库里是数字(1)、表里写的是文本('1')这类的偏差不该让单元格变空。
 */
export function optionLabelOf(col, v) {
    const opts = optionsOf(col)
    if (!opts) return null
    const hit = opts.find(o => String(o.v) === String(v))
    return hit ? hit.label : null
}

/**
 * 显示文案 / 入库值 → 入库值（导入解析用）。认不出来返回 undefined，由调用方决定怎么办。
 * 匹配顺序：先按入库值、再按文案全等，最后退一步做"包含"匹配 ——
 * 用户常写「即时结算」而不是「即时结算类」，旧代码的写死分支也是这么松。
 */
export function optionValueOf(col, cell) {
    const opts = optionsOf(col)
    if (!opts) return undefined
    const s = String(cell ?? '').trim()
    if (s === '') return undefined
    const hit = opts.find(o => String(o.v) === s)
        || opts.find(o => o.label === s)
        || opts.find(o => o.label.includes(s) || s.includes(o.label))
    return hit ? hit.v : undefined
}

/**
 * 空单元格的落库值，按"库里的约束"定，不由前端发挥：
 *   数字 -> 0（这些列多是 NOT NULL DEFAULT 0，留 null 会直接撞 NOT NULL）
 *   是/否 -> false
 *   日期 -> null（日期列都可空）
 *   文本 -> ''（varchar 的 NOT NULL 只约束"不能是 NULL"，空串是合法值 —— 旧解析器也是这么写的）
 *           标了 nullWhenEmpty 的列例外（如 sign_type）：空值统一归一 null，与"未填写"语义一致
 */
function emptyOf(col) {
    if (col.type === 'float' || col.type === 'int') return 0
    if (col.type === 'bool') return false
    if (col.type === 'date') return null
    return col.nullWhenEmpty ? null : ''
}

/** 该模板的筛选栏定义（每张表自己的筛选条件，全在 gd.json 的 filters 里） */
export function filtersOf(tbName) {
    return tableOf(tbName).filters || []
}

/**
 * 该模板的编辑表单分组（[{title, fields[]}]）—— 每个模板的编辑页都从自己这份配置渲染。
 * 兜底：没被任何分组引用的列统一追加到「其他」，绝不因为漏配 groups 就悄悄丢字段。
 */
export function formGroupsOf(tbName) {
    const groups = (tableOf(tbName).formGroups || [])
        .map(g => ({title: g.title, fields: [...(g.fields || [])]}))
    const grouped = new Set(groups.flatMap(g => g.fields))
    const rest = formColumnsOf(tbName).filter(c => !grouped.has(c.field)).map(c => c.field)
    if (rest.length) groups.push({title: '其他', fields: rest})
    return groups
}

/** 该表登记的示范行（模板里给用户照着填的样例）。样例值一律写在 gd.json，本文件不写死任何值 */
function examplesOf(tbName) {
    return tableOf(tbName).examples || []
}

/** 页面"预览模板表头"要展示的示范行：原样返回 gd.json 里登记的样例，不做任何改写 */
export function templateExamples(tbName) {
    return examplesOf(tbName)
}

/**
 * 类型字典也来自 gd.json 的 fieldTypes：label = 单元格类型名，sample = 该类型的格式样例。
 * 本文件只拼装文案，不写死「文字/小数/整数/日期」这些中文 —— 以后新增一种类型只改 JSON。
 */
const TYPE_DEFS = gd.fieldTypes || {}

/** 单元格类型名；未登记的类型按「文字」处理，而不是抛错（老数据里没有 type 的列也一样） */
export function typeLabelOf(type) {
    const d = TYPE_DEFS[type]
    return (d && d.label) || (TYPE_DEFS.text && TYPE_DEFS.text.label) || String(type || '')
}

/**
 * 模板第 3 行的「填写说明」：每列一句话，说清这格该填什么形状。
 * 形如「小数（必填），如 3836.92」「整数：预付款待付／到货款待付／质保款待付／全付」。
 * 每个单元格都以类型名开头 —— 导入解析据此认出这一行并跳过（见 isHintLike）。
 */
/** 单列的「填写说明」：类型 / 是否必填 / 可填项或格式样例（模板第 3 行那一格） */
function hintOfCol(col) {
    let s = typeLabelOf(col.type)
    if (col.required) s += '（必填）'
    // 登记了可选值的列（财务环节 / 是否挂账 / 付款类型）直接列出可填项 ——
    // 比塞给用户一个格式样例有用得多。
    const opts = optionsOf(col)
    if (opts) {
        s += '：' + opts.map(o => o.label).join('／')
    } else {
        // 样例优先取列自己登记的（如"已支付比例"该是 0.7，而不是金额样例 3836.92）
        const sp = col.sample || (TYPE_DEFS[col.type] || {}).sample
        if (sp) s += '，如 ' + sp
    }
    return s
}

/** 模板第 3 行的「填写说明」整行（逐列一句话），顺序与 downloadTemplate 的列一致 */
export function templateHints(tbName, order) {
    return columnsOf(tbName, order).map(hintOfCol)
}

/** 说明行识别：单元格以某个已登记的类型名开头，且类型名后面不是汉字。
 *  类型名来自 gd.json（新增类型无需改这里）；用"后面不是汉字"而不是列举（/：/，——
 *  因为说明文案里既有「小数（必填），如 …」也有「整数：A／B」，硬列分隔符迟早漏一个；
 *  同时「日期未定」这类自由文本不会被误判成说明格。 */
const HINT_HEAD_RE = new RegExp('^(' + Object.keys(TYPE_DEFS)
    .map(k => String((TYPE_DEFS[k] || {}).label || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
    .filter(Boolean).join('|') + ')(?![一-龥])')

/** 整行都长成"类型说明"的样子 → 判定为模板自带的填写说明行 */
function isHintLike(row) {
    const cells = row.map(c => String(c ?? '').trim()).filter(Boolean)
    if (!cells.length) return false
    return cells.filter(c => HINT_HEAD_RE.test(c)).length >= Math.ceil(cells.length * 0.6)
}

/**
 * 导入模板的列清单（供页面上"预览模板表头"用）。
 * 与 downloadTemplate 同源（同一份 gd.json 的同一张表），所以"预览到的"＝"下载下来的"，不会两处漂移。
 * @param tbName 该模板引用的物理表名（t_contract_template.tb_name）；不传则用当前导入表
 * @param order  可选的列顺序覆盖值（t_contract_template.col_order）；不传则用登记顺序
 */
export function templateColumns(tbName, order) {
    return columnsOf(tbName, order).map(c => ({
        field: c.field,
        header: c.header,
        type: c.type || 'text',
        typeLabel: typeLabelOf(c.type),
        required: !!c.required,
        options: optionsOf(c),
        // 填写说明挂到列上：预览弹窗按列渲染，不必再按下标去另一份数组里取（重排后极易错位）
        hint: hintOfCol(c),
    }))
}

/** 比较用归一：去掉空白，避免用户文件里"合同 名称"这类排版差异导致认不出 */
function norm(v) {
    return String(v ?? '').replace(/\s+/g, '')
}

/**
 * 已知中文表头文案 = gd.json 里所有登记表的 header（+ 台账常见的"序号"列）。
 * 用途是"认出上传文件里的中文表头行"。
 * ⚠️ 刻意用**全等**而不是"包含关键词"：前者认错只会把表头行当成数据（抛必填错误，看得见），
 *   后者会反过来 —— 数据行里出现「某某供应商有限公司」+「备注」这类值就命中多个关键词，
 *   整行被当成表头静默跳过。宁可见报错，不可丢数据。
 */
const KNOWN_HEADERS = new Set(['序号'])
Object.keys(gd.contractTables || {}).forEach(k => {
    columnsOf(k).forEach(c => KNOWN_HEADERS.add(norm(c.header)))
})

/** 「示范行」的比对键：id + title。两个字段同时命中才认，避免误伤真实数据 */
function exampleKeys() {
    const out = []
    Object.keys(gd.contractTables || {}).forEach(k => {
        ;(gd.contractTables[k].examples || []).forEach(ex => {
            out.push([String(ex.id ?? '').trim(), String(ex.title ?? '').trim()])
        })
    })
    return out
}

/* ---------------- 导出 ---------------- */
/**
 * 将合同数据导出为 xlsx 并触发浏览器下载
 * 导出格式：第 1 行英文字段名，第 2 行中文表头，第 3 行起为数据
 * 导出文件可直接当导入文件使用
 * @param rows 合同数组
 * @param filename 文件名
 * @param tbName 该模板引用的物理表名（决定用哪份列清单）
 * @param order  可选的列顺序覆盖值（t_contract_template.col_order）
 */
export function exportContractExcel(rows, filename = '合同台账_导出', tbName, order) {
    const defs = columnsOf(tbName, order)
    // 第 1 行：英文字段名
    const fieldRow = defs.map(d => d.field)
    // 第 2 行：中文表头（不再加 * 号）
    const headerRow = defs.map(d => d.header)
    // 数据行：显示值一律由"该列的类型 + 可选值"推出来，不按字段名特判
    const dataRows = rows.map(c => defs.map(({field, type, options}) => {
        let v = c[field]
        if (v === undefined || v === null) v = ''
        // 可选值列（付款类型 / 财务环节 / 是否挂账…）导出成中文文案：读表的人不必认识内部编码
        const label = optionLabelOf({options}, v)
        if (label !== null) return label
        if (type === 'date') return formatExportDate(v)
        if (type === 'bool') return BOOL_TO_YES_NO(v)
        return v
    }))

    const aoa = [fieldRow, headerRow, ...dataRows]
    const sheet = XLSX.utils.aoa_to_sheet(aoa)
    sheet['!cols'] = defs.map(d => {
        if (d.header.includes('供应商') || d.header.includes('备注') || d.header.includes('移交物资')) return {wch: 28}
        if (d.header.includes('合同') || d.header.includes('日期') || d.header.includes('时间') || d.header.includes('方式')) return {wch: 16}
        return {wch: 12}
    })
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '合同台账')

    const stamp = new Date().toISOString().slice(0, 10)
    XLSX.writeFile(wb, `${filename}_${stamp}.xlsx`)
}

/* 金额保留两位小数（字符串形式，避免 xlsx 把 0 显示为空） */
const FIX2 = (n) => {
    const v = Number(n)
    if (!Number.isFinite(v)) return '0.00'
    return v.toFixed(2)
}

/** 原样取值为字符串（null / undefined → 空串），给"直接取字段"的列用 */
function strOf(v) {
    return v === null || v === undefined ? '' : String(v)
}

/**
 * 「导给财务」报表定义（按物理表取）。返回 null = 这张表没有这套报表 ——
 * 台账页据此决定显不显示「导给财务」按钮，不再靠一份写死的字段清单。
 *
 * 为什么整块搬进 gd.json：这是一张给财务的**固定口径**报表，列名、顺序、列宽、
 * 以及"金额怎么算"都是业务约定；写死在 JS 里，改一个字都要跟着发一次前端版本。
 * 搬进配置后，新增/调整列只动 gd.json —— 本文件只负责几种通用的取值方式（kind）。
 */
export function financeExportOf(tbName) {
    return tableOf(tbName).financeExport || null
}

/**
 * 取值条件（可选）：{field, ne} / {field, eq}，只做等值比较 —— 够表达"某一类才带备注"这类规则。
 * 字段缺失或没写 when 一律视为成立。两边都按字符串比：库里是数字 1、
 * 配置里写 1 或 "1"，都不该让单元格变空。
 */
function whenOk(row, when) {
    if (!when || !when.field) return true
    const v = strOf(row[when.field])
    if (when.ne !== undefined && v === strOf(when.ne)) return false
    if (when.eq !== undefined && v !== strOf(when.eq)) return false
    return true
}

/**
 * 「导给财务」单元格取值：只认 gd.json 里登记的这几种 kind，本文件不写死任何列名与文案。
 *   index   序号（1 起）
 *   const   固定文案
 *   field   直接取字段
 *   money   取字段，保留两位小数
 *   diff    两个字段相减（下限 0），保留两位小数
 *   zero    固定 0.00
 *   concat  多个片段首尾相接，片段可用 when 控制是否参与
 */
function financeCell(row, idx, col) {
    switch (col.kind) {
        case 'index':
            return idx + 1
        case 'const':
            return strOf(col.value)
        case 'money':
            return FIX2(row[col.field])
        case 'diff': {
            const f = col.fields || []
            const a = Number(row[f[0]]) || 0
            const b = Number(row[f[1]]) || 0
            return FIX2(Math.max(0, a - b))
        }
        case 'zero':
            return FIX2(0)
        case 'concat':
            return (col.parts || [])
                .filter(p => whenOk(row, p.when))
                .map(p => strOf(row[p.field]))
                .join('')
        case 'field':
        default:
            return strOf(row[col.field])
    }
}

/**
 * 导给财务：表名、列清单、列宽、文件名全部来自 gd.json 的 financeExport
 * （角色 >= EDITOR 才能调用，见台账页按钮上的 v-hasRole）。
 * 返回 false = 该表没登记这套报表配置 —— 调用方据此提示，而不是导出一张空表。
 * 金额一律保留两位小数（写字符串，避免 xlsx 把 0 显示成空）。
 * @param rows   合同数组（当前筛选条件下的全量）
 * @param tbName 该模版引用的物理表名
 */
export function exportFinanceExcel(rows, tbName) {
    const cfg = financeExportOf(tbName)
    if (!cfg) return false
    const defs = cfg.columns || []
    const headerRow = defs.map(c => c.header)
    const dataRows = rows.map((c, idx) => defs.map(col => financeCell(c, idx, col)))

    const aoa = [headerRow, ...dataRows]
    const sheet = XLSX.utils.aoa_to_sheet(aoa)
    sheet['!cols'] = defs.map(c => ({wch: c.width || 12}))

    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, cfg.sheet || '导给财务')
    const stamp = new Date().toISOString().slice(0, 10)
    XLSX.writeFile(wb, `${cfg.filename || '导给财务'}_${stamp}.xlsx`)
    return true
}

/* ---------------- 模板下载 ---------------- */
/**
 * 下载导入模板
 * @param tplName 所选合同模版名。只进文件名，让不同模版下载下来的文件互不覆盖。
 * @param tbName  该模版引用的物理表名（t_contract_template.tb_name）。列定义按表取，
 *                与「预览模板表头」读的是同一份 gd.json，两边永远一致；不传则用当前导入表。
 * @param order   该模版登记的列顺序覆盖值（t_contract_template.col_order）。
 *                与「导出 Excel」共用同一份顺序，保证"预览 = 下载 = 导出"始终一致。
 *
 * 版式（前三行是骨架，第 4 行起是照着填的样例）：
 *   第 1 行 英文字段名 —— 导入按这一行认列，勿改
 *   第 2 行 中文表头   —— 供人阅读，勿改
 *   第 3 行 填写说明   —— 每列的类型 / 是否必填 / 格式样例（gd.json 的 fieldTypes）
 *   第 4 行起 示范行   —— gd.json 的 examples；导入时按 id + title 自动忽略，不必手工删
 */
export function downloadTemplate(tplName, tbName, order) {
    const defs = columnsOf(tbName, order)
    const fieldRow = defs.map(d => d.field)
    const headerRow = defs.map(d => d.header)
    // 第 3 行：填写说明（类型 / 必填 / 格式样例），列列对齐
    const hintRow = templateHints(tbName, order)
    // 第 4 行起：示范行，值照抄 gd.json（日期按用户习惯写成 2025/09/04；解析器 - 与 / 都认）
    const exampleRows = examplesOf(tbName).map(ex => defs.map(({field}) => {
        const v = ex[field]
        return v === undefined || v === null ? '' : v
    }))

    const aoa = [fieldRow, headerRow, hintRow, ...exampleRows]
    const sheet = XLSX.utils.aoa_to_sheet(aoa)
    sheet['!cols'] = defs.map(d => {
        if (d.header.includes('供应商') || d.header.includes('备注') || d.header.includes('移交物资')) return {wch: 28}
        if (d.header.includes('合同') || d.header.includes('日期') || d.header.includes('时间') || d.header.includes('方式')) return {wch: 16}
        return {wch: 12}
    })
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '导入模板')
    const suffix = tplName ? `_${tplName}` : ''
    XLSX.writeFile(wb, `合同台账导入模板${suffix}.xlsx`)
}


/* ---------------- 导入解析 ---------------- */
/**
 * 解析上传的 Excel 文件为合同行数据
 *
 * Excel 结构约定（兼容两种常见格式）：
 *   格式 A：第 1 行中文表头，第 2 行英文字段名，第 3 行起数据
 *   格式 B：第 1 行英文字段名，第 2 行中文表头，第 3 行起数据
 *   格式 C：仅英文字段名一行表头，下一行起数据
 *   下载下来的模板是「格式 D」：字段名 / 中文表头 / 填写说明 / 若干示范行，随后才是用户数据；
 *   说明行与示范行都会被自动跳过（前者按"类型说明"签名，后者按 gd.json 登记的 id+title 比对），
 *   所以用户拿到模板后可以直接在示范行下面接着填，不必先手工删样例。
 *   表头行识别用"与 gd.json 登记的 header 全等"而不是"包含关键词"—— 见 KNOWN_HEADERS 处的注释；
 *   代码自动识别英文字段名行（包含 id 和 title），并跳过紧随其后的表头行
 *
 * @param file File 对象
 * @returns Promise<Array> 行对象数组
 */
export function parseContractExcel(file, tbName) {
    // 列按**目标物理表**取：导入模板 / 导出 / 解析三处同源，模板一换这里跟着换
    const defs = columnsOf(tbName)
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = e => {
            try {
                const wb = XLSX.read(e.target.result, {type: 'array'})
                const sheet = wb.Sheets[wb.SheetNames[0]] // 读取第一个sheet
                // 转为二维数组
                const aoa = XLSX.utils.sheet_to_json(sheet, {header: 1, defval: ''})
                if (aoa.length < 3) {
                    resolve([])
                    return
                }

                // 定位英文字段名行：扫描前 5 行，找到包含"id"和"title"的行
                let fieldRowIdx = -1
                for (let i = 0; i < Math.min(aoa.length, 5); i++) {
                    const cells = aoa[i].map(c => String(c || '').trim().toLowerCase())
                    if (cells.includes('id') && cells.includes('title')) {
                        fieldRowIdx = i
                        break
                    }
                }
                if (fieldRowIdx < 0) {
                    reject(new Error('未找到英文字段名行（需包含 id 和 title），请确认 Excel 格式'))
                    return
                }

                // 建立列索引：field name → column index
                const fieldNames = aoa[fieldRowIdx].map(c => String(c || '').trim())
                const colMap = {}  // field → colIdx
                fieldNames.forEach((name, idx) => {
                    const lower = name.toLowerCase()
                    const def = defs.find(d => d.field === lower)
                    if (def) colMap[def.field] = idx
                })

                // 模板自带的示范行：id + title 命中 gd.json 里登记的样例 → 是样例不是业务数据
                const EX_KEYS = exampleKeys()
                const isExampleRow = (row) => {
                    if (!EX_KEYS.length) return false
                    const ci = colMap.id, ct = colMap.title
                    if (ci === undefined || ct === undefined) return false
                    const k0 = String(row[ci] ?? '').trim(), k1 = String(row[ct] ?? '').trim()
                    return EX_KEYS.some(([i, t]) => k0 === i && k1 === t)
                }

                // 跳过英文字段名行之后的表头行 / 填写说明行 / 示范行，找到真正的数据起始行
                // 判断依据：整行是中文表头 → 表头行；整行都是类型说明 → 说明行；id+title 命中样例 → 示范行
                // 整行就是中文表头（与 gd.json 登记的 header 全等）→ 表头行。
                // 不要退回"包含关键词"：数据行里的「某某供应商有限公司」「备注」会命中关键词被误吞。
                const isHeaderLike = (row) => {
                    const cells = row.map(c => norm(c)).filter(Boolean)
                    if (cells.length < 2) return false
                    return cells.filter(c => KNOWN_HEADERS.has(c)).length >= Math.max(2, Math.ceil(cells.length * 0.6))
                }

                // 从 fieldRowIdx + 1 开始，跳过"模板头部区"（表头行 / 填写说明行 / 模板自带示范行），
                // 定位真实数据起始位置。只在这一段连续区块里按样例键跳过示范行 —— 用户写在示范行
                // 之后的数据一律视为业务数据，绝不按样例键丢弃（避免真实合同与样例同名时被吃掉）。
                let startIdx = fieldRowIdx + 1
                while (startIdx < aoa.length && (isHeaderLike(aoa[startIdx]) || isHintLike(aoa[startIdx]) || isExampleRow(aoa[startIdx]))) {
                    startIdx++
                }

                const rows = []
                for (let i = startIdx; i < aoa.length; i++) {
                    const rawRow = aoa[i]
                    // 跳过完全空的行
                    if (rawRow.every(c => String(c ?? '').trim() === '')) continue
                    // 跳过表头行 / 填写说明行
                    // ⚠️ 示范行刻意不在这里判：示范行的 id+title 可能真的与库里某条合同同名
                    //    （模板原有的样例就是真实数据），只有"紧跟表头的连续区块"才允许按样例键跳过，
                    //    否则用户导出一份真台账再导回来会被静默丢数据。见上面 startIdx 处的循环。
                    if (isHeaderLike(rawRow) || isHintLike(rawRow)) continue

                    const row = {}
                    defs.forEach(col => {
                        const {field, type, required} = col
                        const colIdx = colMap[field]
                        let v = colIdx !== undefined ? rawRow[colIdx] : ''
                        if (v === undefined || v === null) v = ''
                        const empty = String(v).trim() === ''
                        const hasOpt = !!optionsOf(col)

                        // ① 可选值列：中文文案 → 入库值（付款类型 / 财务环节 / 是否挂账…）。
                        //    能认出就直接用；认不出来的非空值落 null，交给后端按行报错 ——
                        //    猜一个值写进库，比报错更难发现。
                        if (hasOpt && !empty) {
                            const ov = optionValueOf(col, v)
                            row[field] = ov === undefined ? null : ov
                            return
                        }
                        // 可选值列空着：必填列留 null（后端报"缺 XX"），非必填列按类型补兜底值
                        // （库里的 finish_step 是 NOT NULL DEFAULT 0，不补会直接撞 NOT NULL）。
                        if (hasOpt) {
                            row[field] = required ? null : emptyOf(col)
                            return
                        }
                        // ② 其余空值按库约束补（见 emptyOf 的说明）
                        if (empty) {
                            row[field] = emptyOf(col)
                            return
                        }
                        // ③ 有值：按类型转
                        if (type === 'date') row[field] = formatDate(v)
                        else if (type === 'bool') row[field] = YES_NO_TO_BOOL(v)
                        else if (type === 'float') row[field] = Number(v) || 0
                        else if (type === 'int') row[field] = parseInt(v, 10) || 0
                        else row[field] = String(v).trim()
                    })
                    rows.push(row)
                }
                resolve(rows)
            } catch (err) {
                reject(err)
            }
        }
        reader.onerror = reject
        reader.readAsArrayBuffer(file)
    })
}

function formatDate(v) {
    if (!v && v !== 0) return null
    // Excel 序列号日期
    if (typeof v === 'number') {
        const d = new Date(Math.round((v - 25569) * 86400 * 1000))
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }
    const s = String(v)
    const m = s.match(/(\d{4})[\/\-.](\d{1,2})[\/\-.](\d{1,2})/)
    if (m) return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`
    return s.trim() || null
}

/**
 * 导出时日期格式化：将 Date 对象、时间戳、ISO 字符串等统一转为 yyyy-MM-dd
 */
function formatExportDate(v) {
    if (v === null || v === undefined || v === '') return ''
    // Date 对象
    if (v instanceof Date) {
        if (isNaN(v.getTime())) return ''
        return `${v.getFullYear()}-${String(v.getMonth() + 1).padStart(2, '0')}-${String(v.getDate()).padStart(2, '0')}`
    }
    // 数字时间戳（毫秒）
    if (typeof v === 'number') {
        const d = new Date(v)
        if (isNaN(d.getTime())) return ''
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }
    // 字符串：尝试解析
    const s = String(v).trim()
    if (!s) return ''
    // 已经是 yyyy-MM-dd 格式 → 直接返回
    if (/^\d{4}-\d{1,2}-\d{1,2}/.test(s)) {
        return s.replace(/(\d{4})-(\d{1,2})-(\d{1,2})/, (_, y, m, d) => `${y}-${m.padStart(2, '0')}-${d.padStart(2, '0')}`)
    }
    // yyyy/MM/dd 等斜杠格式
    const m = s.match(/(\d{4})[\/\-.](\d{1,2})[\/\-.](\d{1,2})/)
    if (m) return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`
    // 其他：尝试 Date 构造
    const d = new Date(s)
    if (!isNaN(d.getTime())) {
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }
    return s
}
