import * as XLSX from 'xlsx'
import {METHOD_OPTIONS} from "../system/MockX.js"

/**
 * 合同台账 - Excel 导入/导出工具（v4 - 2026-08-24）
 * 导入：按英文字段名读取（Excel 第 1 行中文表头，第 2 行英文字段名，第 3 行忽略，从第 4 行开始读数据）
 * 导出：中文表头 + 英文字段名两行表头，数据从第 3 行开始
 */

/* 签订方式 string → int 编码（sign_type）互转，与 gd.json METHOD_OPTIONS 下标一致 */
const SIGN_STR_TO_CODE = (s) => {
    const str = String(s || '').trim()
    if (!str) return null
    // 精确匹配
    const i = METHOD_OPTIONS.findIndex(m => m.desc === str)
    if (i >= 0) return METHOD_OPTIONS[i].id
    // 模糊匹配：Excel 中可能写"网络询比价"等，只要包含关键词即可
    for (let j = 0; j < METHOD_OPTIONS.length; j++) {
        if (str.includes(METHOD_OPTIONS[j].desc) || METHOD_OPTIONS[j].desc.includes(str)) {
            return METHOD_OPTIONS[j].id
        }
    }
    return 0
}
const SIGN_CODE_TO_STR = (i) => {
    const n = Number(i)
    if (!Number.isInteger(n)) return ''
    const m = METHOD_OPTIONS.find(x => x.id === n)
    return m ? m.desc : ''
}
// 签订人 account → username，导出展示用
const USERNAME_FROM_ACCOUNT = (account, signers) => {
    const s = signers.find(x => String(x.account) === String(account || ''))
    return s ? s.username : String(account || '')
}
// 签订人 username → account，导入用
const ACCOUNT_FROM_USERNAME = (username, signers) => {
    const s = signers.find(x => String(x.username) === String(username || ''))
    return s ? s.account : String(username || '')
}

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
 * 字段定义表：field（后端字段名/英文字段名）、header（中文表头）、type
 * 列顺序与导入 Excel 保持一致，保证导出文件可直接导入
 * 导出格式：第 1 行英文字段名，第 2 行中文表头，第 3 行起为数据
 */
const FIELD_DEFS = [
    {field: 'id', header: '合同编号', required: true},
    {field: 'title', header: '合同名称', required: true},
    {field: 'sign_person', header: '签订人', required: true},
    {field: 'sign_type', header: '合同签订方式', required: true},
    {field: 'supplier', header: '供应商', required: true},
    {field: 'amount', header: '合同金额(元)', required: true, type: 'float'},
    {field: 'date_sign', header: '签订时间', required: true, type: 'date'},
    {field: 'pay_type', header: '付款方式'},
    {field: 'payment_type', header: '付款类型'},
    {field: 'paycycle_dh', header: '到货付款周期(月)', type: 'float'},
    {field: 'paycycle_zb', header: '质保付款周期(月)', type: 'float'},
    {field: 'settle_amount', header: '结算金额(元)', type: 'float'},
    {field: 'hq', header: '货期(天)', type: 'int'},
    {field: 'date_htyj', header: '合同移交日期', type: 'date'},
    {field: 'date_fpyj', header: '发票移交日期', type: 'date'},
    {field: 'date_actual_dh', header: '实际到货日期', type: 'date'},
    {field: 'date_ruzlyj', header: '入库资料移交物资日期', type: 'date'},
    {field: 'date_rk', header: '挂账日期', type: 'date'},
    {field: 'date_yfk', header: '预付款日期', type: 'date'},
    {field: 'date_dhk', header: '到货款日期', type: 'date'},
    {field: 'date_zbj', header: '质保金付款日期', type: 'date'},
    {field: 'bz', header: '备注'},
    {field: 'finish_step', header: '财务环节'},
]

/* ---------------- 导出 ---------------- */
/**
 * 将合同数据导出为 xlsx 并触发浏览器下载
 * 导出格式：第 1 行英文字段名，第 2 行中文表头，第 3 行起为数据
 * 导出文件可直接当导入文件使用
 * @param rows 合同数组
 * @param filename 文件名
 * @param signers 签订人字典 [{account, username}]
 */
export function exportContractExcel(rows, filename = '合同台账_导出', signers = []) {
    // 第 1 行：英文字段名
    const fieldRow = FIELD_DEFS.map(d => d.field)
    // 第 2 行：中文表头（不再加 * 号）
    const headerRow = FIELD_DEFS.map(d => d.header)
    // 数据行
    const dataRows = rows.map(c => {
        return FIELD_DEFS.map(({field, type}) => {
            let v = c[field]
            if (v === undefined || v === null) v = ''
            if (field === 'sign_type') v = SIGN_CODE_TO_STR(v)
            else if (field === 'payment_type') v = PAYMENT_TYPE_CODE_TO_STR(v)
            else if (field === 'sign_person') v = USERNAME_FROM_ACCOUNT(v, signers)
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
export function exportFinanceExcel(rows, filename = '导给财务', signers = []) {
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
            USERNAME_FROM_ACCOUNT(c.sign_person, signers), // 业务员 account→username
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
export function downloadTemplate() {
    const fieldRow = FIELD_DEFS.map(d => d.field)
    const headerRow = FIELD_DEFS.map(d => d.header)
    const exampleRow = FIELD_DEFS.map(({field, type}) => {
        const ex = EXAMPLE_ROW[field]
        if (ex === undefined) return ''
        if (field === 'sign_type') return SIGN_CODE_TO_STR(ex)
        if (field === 'payment_type') return PAYMENT_TYPE_CODE_TO_STR(ex)
        if (field === 'finish_step') return FINISHED_INT_TO_STR(ex)
        if (type === 'date') return formatExportDate(ex)
        return ex
    })

    const aoa = [fieldRow, headerRow, exampleRow]
    const sheet = XLSX.utils.aoa_to_sheet(aoa)
    sheet['!cols'] = FIELD_DEFS.map(d => {
        if (d.header.includes('供应商') || d.header.includes('备注') || d.header.includes('移交物资')) return {wch: 28}
        if (d.header.includes('合同') || d.header.includes('日期') || d.header.includes('时间') || d.header.includes('方式')) return {wch: 16}
        return {wch: 12}
    })
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '导入模板')
    XLSX.writeFile(wb, '合同台账导入模板.xlsx')
}

const EXAMPLE_ROW = {
    id: 'SMLJ-CG-CL-26330',
    title: '螺栓',
    amount: 3836.92,
    date_sign: '2026-08-01',
    sign_person: '薛少军',
    sign_type: 0,
    supplier: '榆林景云五金机电设备有限公司',
    pay_type: '货到票到3个月付款',
    payment_type: 1,
    paycycle_dh: 3,
    paycycle_zb: 12,
    date_yfk: '',
    date_dhk: '2026-11-01',
    date_zbj: '2027-11-01',
    date_rk: '',
    bz: '标准件采购',
    settle_amount: 3836.92,
    hq: 30,
    date_htyj: '2026-08-08',
    date_fpyj: '',
    date_actual_dh: '',
    date_ruzlyj: '',
    finish_step: 0,
}

/* ---------------- 导入解析 ---------------- */
/**
 * 解析上传的 Excel 文件为合同行数据
 *
 * Excel 结构约定（兼容两种常见格式）：
 *   格式 A：第 1 行中文表头，第 2 行英文字段名，第 3 行起数据
 *   格式 B：第 1 行英文字段名，第 2 行中文表头，第 3 行起数据
 *   格式 C：仅英文字段名一行表头，下一行起数据
 *   代码自动识别英文字段名行（包含 id 和 title），并跳过紧随其后的表头行
 *
 * @param file File 对象
 * @param signers 签订人字典 [{account, username}]，用于"签订人姓名→account"转码
 * @returns Promise<Array> 行对象数组
 */
export function parseContractExcel(file, signers = []) {
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

                // 跳过英文字段名行之后的表头行（中文表头/说明行），找到真正的数据起始行
                // 判断依据：行中包含中文表头关键词 → 视为表头，跳过
                const HEADER_KEYWORDS = ['合同编号', '合同名称', '签订人', '供应商', '合同金额', '签订时间',
                    '付款方式', '付款类型', '签订方式', '到货付款', '质保付款', '结算金额', '货期', '移交日期', '到货日期', '挂账日期',
                    '预付款日期', '到货款日期', '质保金付款日期', '备注', '序号',
                    '财务完结', '合同签订方式']
                const isHeaderLike = (row) => {
                    const cells = row.map(c => String(c || '').trim())
                    const hit = cells.filter(c => HEADER_KEYWORDS.some(k => c.includes(k)))
                    // 命中 2 个以上中文表头关键词 → 判定为表头行
                    return hit.length >= 2
                }

                // 从 fieldRowIdx + 1 开始，跳过表头行，定位真实数据起始位置
                let startIdx = fieldRowIdx + 1
                while (startIdx < aoa.length && isHeaderLike(aoa[startIdx])) {
                    startIdx++
                }

                const rows = []
                for (let i = startIdx; i < aoa.length; i++) {
                    const rawRow = aoa[i]
                    // 跳过完全空的行
                    if (rawRow.every(c => String(c ?? '').trim() === '')) continue
                    // 跳过看起来像表头的行（中文表头关键词命中 ≥2）
                    if (isHeaderLike(rawRow)) continue

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
                        } else if (type === 'float' || type === 'int') {
                            if (v === '' || v === null) v = 0
                            if (type === 'float') v = Number(v) || 0
                            else v = parseInt(v, 10) || 0
                        } else {
                            // 字符串字段
                            if (v !== '') v = String(v).trim()
                        }

                        // sign_type：收中文，转成 int code
                        if (field === 'sign_type') v = v !== '' && v !== null ? SIGN_STR_TO_CODE(v) : null
                        // payment_type：收中文，转成 int code
                        if (field === 'payment_type') v = v !== '' && v !== null ? PAYMENT_TYPE_STR_TO_CODE(v) : null
                        // finish_step：收中文/数字，转成 int 进度
                        if (field === 'finish_step') v = FINISHED_STR_TO_INT(v)
                        // sign_person：收中文姓名，转成 account
                        // if (field === 'sign_person' && v !== '' && signers.length > 0) {
                        //     v = ACCOUNT_FROM_USERNAME(v, signers)
                        // }

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
