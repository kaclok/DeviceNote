import * as XLSX from 'xlsx'
import {METHOD_OPTIONS} from "../system/MockX.js"

/**
 * 合同台账 - Excel 导入/导出工具（v3 - 2026-08-21 对齐后端 TCGHTContract 24 字段）
 * 基于工程已依赖的 xlsx 库，提供模板下载、导入解析、导出生成。
 *
 * 字段与 MockX.normalizeContract / 后端实体 TCGHTContract 保持一致：
 *   基本：合同编号 / 合同名称 / 合同金额(元) / 签订时间 / 签订人 / 合同签订方式 / 供应商
 *         付款方式 / 到货付款周期(月) / 质保付款周期(月)
 *         预付款比例(%) / 到货款比例(%) / 质保金比例(%)
 *         预付款日期 / 到货款日期 / 质保金付款日期 / 入库日期
 *         备注 / 结算金额(元) / 货期(天)
 *   移交：合同移交日期 / 发票移交日期 / 实际到货日期 / 入库资料移交物资日期
 */

/* 签订方式 string → int 编码（sign_type）互转，与 gd.json METHOD_OPTIONS 下标一致 */
const SIGN_STR_TO_CODE = (s) => {
    const i = METHOD_OPTIONS.indexOf(String(s || ''))
    return i < 0 ? 0 : i
}
const SIGN_CODE_TO_STR = (i) => {
    const n = Number(i)
    return Number.isInteger(n) && METHOD_OPTIONS[n] ? METHOD_OPTIONS[n] : ''
}
// 签订人 account → username，导入用；导入填写中文名，匹配到对应 account
const ACCOUNT_FROM_USERNAME = (username, signers) => {
    const s = signers.find(x => String(x.username) === String(username || ''))
    return s ? s.account : String(username || '')
}
// 签订人 account → username，导出展示用
const USERNAME_FROM_ACCOUNT = (account, signers) => {
    const s = signers.find(x => String(x.account) === String(account || ''))
    return s ? s.username : String(account || '')
}

/* 中文表头 -> 内部字段 的映射（导出/导入共用）
 * 注意：sign_type 导出显示文字、导入收文字→转 code；sign_person 导出显示姓名、导入收姓名→转 account */
const HEADER_FIELD_MAP = [
    {header: '合同编号', field: 'id', required: true},
    {header: '合同名称', field: 'title', required: true},
    {header: '合同金额(元)', field: 'amount', required: true, type: 'float'},
    {header: '签订时间', field: 'date_sign', required: true, type: 'date'},
    {header: '签订人', field: 'sign_person', required: true},
    {header: '合同签订方式', field: 'sign_type', required: true},
    {header: '供应商', field: 'supplier', required: true},
    {header: '付款方式', field: 'pay_type'},
    {header: '到货付款周期(月)', field: 'paycycle_dh', type: 'float'},
    {header: '质保付款周期(月)', field: 'paycycle_zb', type: 'float'},
    {header: '预付款比例(%)', field: 'rate_yfk', type: 'float'},
    {header: '到货款比例(%)', field: 'rate_dhk', type: 'float'},
    {header: '质保金比例(%)', field: 'rate_zbj', type: 'float'},
    {header: '预付款日期', field: 'date_yfk', type: 'date'},
    {header: '到货款日期', field: 'date_dhk', type: 'date'},
    {header: '质保金付款日期', field: 'date_zbj', type: 'date'},
    {header: '入库日期', field: 'date_rk', type: 'date'},
    {header: '备注', field: 'bz'},
    {header: '结算金额(元)', field: 'settle_amount', type: 'float'},
    {header: '货期(天)', field: 'hq', type: 'int'},
    {header: '合同移交日期', field: 'date_htyj', type: 'date'},
    {header: '发票移交日期', field: 'date_fpyj', type: 'date'},
    {header: '实际到货日期', field: 'date_actual_dh', type: 'date'},
    {header: '入库资料移交物资日期', field: 'date_ruzlyj', type: 'date'},
]

/* ---------------- 导出 ---------------- */
/**
 * 将合同数据导出为 xlsx 并触发浏览器下载
 * @param rows 合同数组（包含 sign_type int、sign_person account）
 * @param filename 文件名（不含扩展名）
 * @param signers 签订人字典 [{account, username}]，可选；不传则按 account 直接填
 */
export function exportContractExcel(rows, filename = '合同台账_导出', signers = []) {
    const headers = HEADER_FIELD_MAP.map(h => h.header)
    const data = rows.map(c => {
        const row = {}
        HEADER_FIELD_MAP.forEach(({header, field}) => {
            let v = c[field] ?? ''
            if (field === 'sign_type') v = SIGN_CODE_TO_STR(v)
            if (field === 'sign_person') v = USERNAME_FROM_ACCOUNT(v, signers)
            row[header] = v
        })
        return row
    })

    const sheet = XLSX.utils.json_to_sheet(data, {header: headers})
    sheet['!cols'] = HEADER_FIELD_MAP.map(h => {
        if (h.header.includes('供应商') || h.header.includes('备注') || h.header.includes('移交物资')) return {wch: 28}
        if (h.header.includes('合同') || h.header.includes('日期') || h.header.includes('时间') || h.header.includes('方式')) return {wch: 16}
        return {wch: 12}
    })
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '合同台账')

    const stamp = new Date().toISOString().slice(0, 10)
    XLSX.writeFile(wb, `${filename}_${stamp}.xlsx`)
}

/* ---------------- 模板下载 ---------------- */
export function downloadTemplate() {
    const headers = HEADER_FIELD_MAP.map(h => h.required ? `${h.header}*` : h.header)
    const example = HEADER_FIELD_MAP.map(({field}) => EXAMPLE_ROW[field] ?? '')

    const sheet = XLSX.utils.aoa_to_sheet([headers, example])
    sheet['!cols'] = HEADER_FIELD_MAP.map(h => {
        if (h.header.includes('供应商') || h.header.includes('备注') || h.header.includes('移交物资')) return {wch: 28}
        if (h.header.includes('合同') || h.header.includes('日期') || h.header.includes('时间') || h.header.includes('方式')) return {wch: 16}
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
    sign_person: '薛少军',          // 导入按姓名→account
    sign_type: '询比价',            // 导入按文字→code
    supplier: '榆林景云五金机电设备有限公司',
    pay_type: '货到票到3个月付款',
    paycycle_dh: 3,
    paycycle_zb: 12,
    rate_yfk: 0,
    rate_dhk: 90,
    rate_zbj: 10,
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
}

/* ---------------- 导入解析 ---------------- */
/**
 * 解析上传的 Excel 文件为合同行数据
 * @param file File 对象
 * @param signers 签订人字典 [{account, username}]，用于“签订人姓名→account”转码；
 *                不传则直接把姓名原样写到 sign_person 字段（MockX 支持账号/姓名不严格匹配）
 * @returns Promise<Array> 行对象数组（键与 MockX.importContractExcel 期望一致，sign_type 已转 int）
 */
export function parseContractExcel(file, signers = []) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = e => {
            try {
                const wb = XLSX.read(e.target.result, {type: 'array'})
                const sheet = wb.Sheets[wb.SheetNames[0]]
                const raw = XLSX.utils.sheet_to_json(sheet, {defval: ''})
                const rows = raw.map(r => {
                    const row = {}
                    HEADER_FIELD_MAP.forEach(({header, field, type}) => {
                        let v = r[header] ?? r[`${header}*`] ?? ''
                        if (type === 'date') v = formatDate(v)
                        else if (v !== '') v = String(v).trim()
                        // sign_type：收中文，转成 int code
                        if (field === 'sign_type' && v !== '') v = SIGN_STR_TO_CODE(v)
                        // sign_person：收中文姓名，转成 account（若 signers 字典能匹配）
                        if (field === 'sign_person' && v !== '' && signers.length > 0) {
                            v = ACCOUNT_FROM_USERNAME(v, signers)
                        }
                        // 数字列：导入的“”归一化为空串，交给 MockX normalizeContract 转 0
                        if ((type === 'float' || type === 'int') && (v === '')) v = ''
                        if (type === 'float' && v !== '') v = Number(v) || 0
                        if (type === 'int' && v !== '') v = parseInt(v, 10) || 0
                        row[field] = v
                    })
                    return row
                })
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
    if (!v && v !== 0) return ''
    // Excel 序列号日期
    if (typeof v === 'number') {
        const d = new Date(Math.round((v - 25569) * 86400 * 1000))
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }
    const s = String(v)
    const m = s.match(/(\d{4})[\/\-.](\d{1,2})[\/\-.](\d{1,2})/)
    if (m) return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`
    return s.trim()
}
