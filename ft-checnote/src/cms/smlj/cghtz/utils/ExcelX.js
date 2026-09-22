import * as XLSX from 'xlsx'
import gd from '../data/gd.json'

/**
 * 合同台账 - Excel 导入/导出工具（v5 - 2026-09-15）
 * 导入：按英文字段名读取（Excel 第 1 行中文表头，第 2 行英文字段名，第 3 行忽略，从第 4 行开始读数据）
 * 导出：中文表头 + 英文字段名两行表头，数据从第 3 行开始
 *
 * v5 变更：sign_type（签订方式）在库中已是 varchar 自由文本，
 *         Excel 与前后端统一使用中文原文，不再做 int 编码互转。
 */

/* 付款类型 int ↔ 文本：1-即时结算类 2-周期结算类 */
const PAYMENT_TYPE_CODE_TO_STR = (i) => {
    const n = Number(i)
    if (n === 1) return '即时结算类'
    if (n === 2) return '周期结算类'
    return ''
}
const PAYMENT_TYPE_STR_TO_CODE = (v) => {
    const num = Number(v)
    if (num === 1 || num === 2) return num
    const s = String(v ?? '').trim()
    if (s.includes('即时')) return 1
    if (s.includes('周期')) return 2
    return null
}

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
/* 财务环节 finish_step int ↔ 文本：0预付款待付 1到货款待付 2质保款待付 3全付 */
const FINISHED_INT_TO_STR = (n) => {
    const v = Number(n) || 0
    if (v === 0) return '预付款待付'
    if (v === 1) return '到货款待付'
    if (v === 2) return '质保款待付'
    return '全付'
}
const FINISHED_STR_TO_INT = (v) => {
    const num = Number(v)
    if (!Number.isNaN(num) && Number.isFinite(num)) {
        const n = parseInt(num, 10)
        if (n >= 0 && n <= 3) return n
    }
    const s = String(v ?? '').trim()
    if (!s) return 0
    if (s === '0' || s.includes('预付款待付') || s.includes('预付') && s.includes('待')) return 0
    if (s === '1' || s.includes('到货款待付') || s.includes('到货') && s.includes('待')) return 1
    if (s === '2' || s.includes('质保款待付') || s.includes('质保') && s.includes('待')) return 2
    if (s === '3' || s.includes('全付') || s.includes('已付完') || s.includes('完结') || s.includes('未开始')) return 3
    return 0
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
// 当前导入落表的物理表，与后端 CCGHT.IMPORT_TARGET_TABLE 对齐（台账读路径也只认这一张表）
const IMPORT_TABLE = 't_contract'

/** 取某张物理表的登记项（columns / examples）；未登记该表时回落到当前导入表，保证老调用方零改动 */
function tableOf(tbName) {
    const all = gd.contractTables || {}
    return all[tbName] || all[IMPORT_TABLE] || {}
}

function columnsOf(tbName) {
    return tableOf(tbName).columns || []
}

/** 该表登记的示范行（模板里给用户照着填的样例）。样例值一律写在 gd.json，本文件不写死任何值 */
function examplesOf(tbName) {
    return tableOf(tbName).examples || []
}

/** 页面"预览模板表头"要展示的示范行：原样返回 gd.json 里登记的样例，不做任何改写 */
export function templateExamples(tbName) {
    return examplesOf(tbName)
}

// 导出 / 导入模板 / 导入解析三处共用（= 当前导入表 t_contract 的列）
const FIELD_DEFS = columnsOf(IMPORT_TABLE)

/**
 * 类型字典也来自 gd.json 的 fieldTypes：label = 单元格类型名，sample = 该类型的格式样例。
 * 本文件只做拼装，不写死「文本/数字/日期」这些中文 —— 以后新增一种类型只改 JSON。
 */
const TYPE_DEFS = gd.fieldTypes || {}

/** 单元格类型名；未登记的类型按"文本"处理，而不是抛错（老数据里没有 type 的列也一样） */
export function typeLabelOf(type) {
    const d = TYPE_DEFS[type]
    return (d && d.label) || (TYPE_DEFS.text && TYPE_DEFS.text.label) || String(type || '')
}

/**
 * 模板第 3 行的「填写说明」：每列一句话，说清这格该填什么形状。
 * 形如「数字（必填），如 3836.92」「选项：预付款待付／到货款待付／质保款待付／全付」。
 * 每个单元格都以类型名开头 —— 导入解析据此认出这一行并跳过（见 isHintLike）。
 */
export function templateHints(tbName) {
    return columnsOf(tbName).map(({type, required, options}) => {
        const d = TYPE_DEFS[type] || {}
        let s = typeLabelOf(type)
        if (required) s += '（必填）'
        if (options && options.length) s += '：' + options.join('／')
        else if (d.sample) s += '，如 ' + d.sample
        return s
    })
}

/** 说明行识别：单元格以某个已登记的类型名开头，且类型名后面不是汉字。
 *  类型名来自 gd.json（新增类型无需改这里）；用"后面不是汉字"而不是列举（/：/，——
 *  因为说明文案里既有「数字（必填），如 …」也有「选项：A／B」，硬列分隔符迟早漏一个；
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
 */
export function templateColumns(tbName) {
    return columnsOf(tbName).map(({field, header, type, required, options}) => ({
        field,
        header,
        type: type || 'text',
        typeLabel: typeLabelOf(type),
        required: !!required,
        options: options || null,
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
    ;((gd.contractTables[k] || {}).columns || []).forEach(c => KNOWN_HEADERS.add(norm(c.header)))
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
 */
export function exportContractExcel(rows, filename = '合同台账_导出') {
    // 第 1 行：英文字段名
    const fieldRow = FIELD_DEFS.map(d => d.field)
    // 第 2 行：中文表头（不再加 * 号）
    const headerRow = FIELD_DEFS.map(d => d.header)
    // 数据行
    const dataRows = rows.map(c => {
        return FIELD_DEFS.map(({field, type}) => {
            let v = c[field]
            if (v === undefined || v === null) v = ''
            if (field === 'payment_type') v = PAYMENT_TYPE_CODE_TO_STR(v)
            else if (field === 'finish_step') v = FINISHED_INT_TO_STR(v)
            else if (type === 'date') v = formatExportDate(v)
            return v
        })
    })

    const aoa = [fieldRow, headerRow, ...dataRows]
    const sheet = XLSX.utils.aoa_to_sheet(aoa)
    sheet['!cols'] = FIELD_DEFS.map(d => {
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

/**
 * 导给财务：按制定 12 列表头编排导出（角色 >= EDITOR 才能调用）
 * 列：序号、付款类型、供应商单位名称、付款事由、结算金额、已付金额、未付金额、
 *     本次计划付款金额、计划电汇金额、计划承兑金额、备注、业务员
 * 所有金额均保留两位小数
 */
export function exportFinanceExcel(rows, filename = '导给财务') {
    const headerRow = [
        '序号', '付款类型', '供应商单位名称', '付款事由',
        '结算金额', '已付金额', '未付金额',
        '本次计划付款金额', '计划电汇金额', '计划承兑金额',
        '备注', '业务员',
    ]
    const dataRows = rows.map((c, idx) => {
        const settle = Number(c.settle_amount) || 0
        const has = Number(c.has_amount) || 0
        const remain = Math.max(0, settle - has)
        // 即时结算类(1)：备注=id；周期结算类(2)：备注=id + bz
        const remark = Number(c.payment_type) === 1
            ? String(c.id || '')
            : String(c.id || '') + String(c.bz || '')
        return [
            idx + 1,                              // 序号 1 起
            '备品备件',                           // 付款类型固定
            String(c.supplier || ''),             // 供应商
            String(c.title || ''),                // 付款事由 = 合同 title
            FIX2(settle),                         // 结算金额
            FIX2(has),                            // 已付金额
            FIX2(remain),                         // 未付金额 = 结算 - 已付
            FIX2(remain),                         // 本次计划付款 = 未付金额
            FIX2(remain),                         // 计划电汇 = 未付金额
            FIX2(0),                              // 计划承兑 = 0
            remark,                               // 备注
            String(c.sign_person || ''),          // 业务员 = 签订人（存的就是姓名，无需转码）
        ]
    })

    const aoa = [headerRow, ...dataRows]
    const sheet = XLSX.utils.aoa_to_sheet(aoa)
    // 列宽
    sheet['!cols'] = [
        {wch: 6},  // 序号
        {wch: 12}, // 付款类型
        {wch: 32}, // 供应商单位名称
        {wch: 36}, // 付款事由
        {wch: 14}, // 结算金额
        {wch: 14}, // 已付金额
        {wch: 14}, // 未付金额
        {wch: 18}, // 本次计划付款金额
        {wch: 16}, // 计划电汇金额
        {wch: 16}, // 计划承兑金额
        {wch: 22}, // 备注
        {wch: 12}, // 业务员
    ]
    // 金额列按数字类型写入会更贴业务习惯，但用户明确"保留两位小数"，写字符串更保险
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '导给财务')
    const stamp = new Date().toISOString().slice(0, 10)
    XLSX.writeFile(wb, `${filename}_${stamp}.xlsx`)
}

/* ---------------- 模板下载 ---------------- */
/**
 * 下载导入模板
 * @param tplName 所选合同模版名。只进文件名，让不同模版下载下来的文件互不覆盖。
 * @param tbName  该模版引用的物理表名（t_contract_template.tb_name）。列定义按表取，
 *                与「预览模板表头」读的是同一份 gd.json，两边永远一致；不传则用当前导入表。
 *
 * 版式（前三行是骨架，第 4 行起是照着填的样例）：
 *   第 1 行 英文字段名 —— 导入按这一行认列，勿改
 *   第 2 行 中文表头   —— 供人阅读，勿改
 *   第 3 行 填写说明   —— 每列的类型 / 是否必填 / 格式样例（gd.json 的 fieldTypes）
 *   第 4 行起 示范行   —— gd.json 的 examples；导入时按 id + title 自动忽略，不必手工删
 */
export function downloadTemplate(tplName, tbName) {
    const defs = columnsOf(tbName)
    const fieldRow = defs.map(d => d.field)
    const headerRow = defs.map(d => d.header)
    // 第 3 行：填写说明（类型 / 必填 / 格式样例），列列对齐
    const hintRow = templateHints(tbName)
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
export function parseContractExcel(file) {
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
                    const def = FIELD_DEFS.find(d => d.field === lower)
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
                    FIELD_DEFS.forEach(({field, type}) => {
                        const colIdx = colMap[field]
                        let v = colIdx !== undefined ? rawRow[colIdx] : ''
                        if (v === undefined || v === null) v = ''

                        // 类型转换
                        if (type === 'date') {
                            v = formatDate(v)
                        } else if (type === 'bool') {
                            v = YES_NO_TO_BOOL(v)
                        } else if (type === 'number' || type === 'int') {
                            // 数字类型：空值补 0（与库里的 NOT NULL DEFAULT 0 口径一致）
                            if (v === '' || v === null) v = 0
                            if (type === 'number') v = Number(v) || 0
                            else v = parseInt(v, 10) || 0
                        } else {
                            // 字符串字段
                            if (v !== '') v = String(v).trim()
                        }

                        // sign_type：DB 允许 NULL，空串统一归一为 null（与"未填写"语义一致，不落空串）
                        if (field === 'sign_type' && v === '') v = null
                        // payment_type：收中文，转成 int code
                        if (field === 'payment_type') v = v !== '' && v !== null ? PAYMENT_TYPE_STR_TO_CODE(v) : null
                        // finish_step：收中文/数字，转成 int 进度
                        if (field === 'finish_step') v = FINISHED_STR_TO_INT(v)

                        row[field] = v
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
