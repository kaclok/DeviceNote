import * as XLSX from 'xlsx'

/**
 * 合同台账 - Excel 导入/导出工具（v2 - 2026-08-18 重构）
 * 基于工程已依赖的 xlsx 库，提供模板下载、导入解析、导出生成。
 *
 * 字段与 MockX.normalizeContract 保持一致：
 *   基本：合同编号 / 合同名称 / 签订人 / 合同签订方式 / 供应商
 *         合同金额(元) / 签订时间
 *   付款：付款方式 / 到货付款周期(月) / 质保付款周期(月)
 *         预付款比例(%) / 到货款比例(%) / 质保金比例(%)
 *         预付款日期 / 到货款日期 / 质保金付款日期
 *   状态：是否已入库 / 是否完结 / 结算金额(元) / 货期(天)
 *   移交：合同移交日期 / 发票移交日期 / 挂账日期
 *         实际到货时间 / 入库资料移交物资日期
 */

/* 中文表头 -> 内部字段 的映射（导出/导入共用） */
const HEADER_FIELD_MAP = [
    {header: '合同编号', field: 'no', required: true},
    {header: '合同名称', field: 'name', required: true},
    {header: '签订人', field: 'signer'},
    {header: '合同签订方式', field: 'method'},
    {header: '供应商', field: 'supplier', required: true},
    {header: '合同金额(元)', field: 'amount', required: true, type: 'float'},
    {header: '签订时间', field: 'signDate', required: true, type: 'date'},
    {header: '付款方式', field: 'payMethod'},
    {header: '到货付款周期(月)', field: 'deliveryPayCycle', type: 'float'},
    {header: '质保付款周期(月)', field: 'warrantyPayCycle', type: 'float'},
    {header: '预付款比例(%)', field: 'prepayRatio', type: 'float'},
    {header: '到货款比例(%)', field: 'deliveryPayRatio', type: 'float'},
    {header: '质保金比例(%)', field: 'warrantyPayRatio', type: 'float'},
    {header: '预付款日期', field: 'prepayDate', type: 'date'},
    {header: '到货款日期', field: 'deliveryPayDate', type: 'date'},
    {header: '质保金付款日期', field: 'warrantyPayDate', type: 'date'},
    {header: '是否已入库', field: 'stock'},
    {header: '是否完结', field: 'completed'},
    {header: '结算金额(元)', field: 'settlementAmount', type: 'float'},
    {header: '货期(天)', field: 'deliveryDays', type: 'int'},
    {header: '合同移交日期', field: 'contractTransferDate', type: 'date'},
    {header: '发票移交日期', field: 'invoiceTransferDate', type: 'date'},
    {header: '挂账日期', field: 'accountDate', type: 'date'},
    {header: '实际到货时间', field: 'actualDeliveryDate', type: 'date'},
    {header: '入库资料移交物资日期', field: 'materialTransferDate', type: 'date'},
]

/* ---------------- 导出 ---------------- */
/**
 * 将合同数据导出为 xlsx 并触发浏览器下载
 * @param rows 合同数组
 * @param filename 文件名（不含扩展名）
 */
export function exportContractExcel(rows, filename = '合同台账_导出') {
    const headers = HEADER_FIELD_MAP.map(h => h.header)
    const data = rows.map(c => {
        const row = {}
        HEADER_FIELD_MAP.forEach(({header, field}) => {
            row[header] = c[field] ?? ''
        })
        return row
    })

    const sheet = XLSX.utils.json_to_sheet(data, {header: headers})
    // 列宽：合同/供应商较宽，日期与金额较窄
    sheet['!cols'] = HEADER_FIELD_MAP.map(h => {
        if (h.header.includes('供应商') || h.header.includes('移交物资')) return {wch: 30}
        if (h.header.includes('合同') || h.header.includes('日期') || h.header.includes('时间')) return {wch: 18}
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
    // 示例行（与 MockX 字段顺序一致）
    const example = HEADER_FIELD_MAP.map(({field}) => EXAMPLE_ROW[field] ?? '')

    const sheet = XLSX.utils.aoa_to_sheet([headers, example])
    sheet['!cols'] = headers.map(h => {
        if (h.includes('供应商') || h.includes('移交物资')) return {wch: 30}
        if (h.includes('合同') || h.includes('日期') || h.includes('时间')) return {wch: 18}
        return {wch: 12}
    })
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '导入模板')
    XLSX.writeFile(wb, '合同台账导入模板.xlsx')
}

const EXAMPLE_ROW = {
    no: 'SMLJ-CG-CL-26330',
    name: '螺栓',
    signer: '薛少军',
    method: '询比价',
    supplier: '榆林景云五金机电设备有限公司',
    amount: 3836.92,
    signDate: '2026-08-01',
    payMethod: '货到票到3个月付款',
    deliveryPayCycle: 3,
    warrantyPayCycle: 12,
    prepayRatio: 0,
    deliveryPayRatio: 90,
    warrantyPayRatio: 10,
    prepayDate: '',
    deliveryPayDate: '2026-11-01',
    warrantyPayDate: '2027-11-01',
    stock: '否',
    completed: '否',
    settlementAmount: 0,
    deliveryDays: 30,
    contractTransferDate: '2026-08-08',
    invoiceTransferDate: '',
    accountDate: '',
    actualDeliveryDate: '',
    materialTransferDate: '',
}

/* ---------------- 导入解析 ---------------- */
/**
 * 解析上传的 Excel 文件为合同行数据
 * @param file File 对象
 * @returns Promise<Array> 行对象数组（键与 MockX.importContractExcel 期望一致）
 */
export function parseContractExcel(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader()
        reader.onload = e => {
            try {
                const wb = XLSX.read(e.target.result, {type: 'array'})
                const sheet = wb.Sheets[wb.SheetNames[0]]
                const raw = XLSX.utils.sheet_to_json(sheet, {defval: ''})
                // 映射中文表头 -> 内部字段
                const rows = raw.map(r => {
                    const row = {}
                    HEADER_FIELD_MAP.forEach(({header, field, type}) => {
                        // 支持 "合同编号*" 与 "合同编号" 两种表头
                        let v = r[header] ?? r[`${header}*`] ?? ''
                        if (type === 'date') v = formatDate(v)
                        else if (v !== '') v = String(v).trim()
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
    // 2026/8/1 或 2026-8-1 归一化
    const m = s.match(/(\d{4})[\/\-.](\d{1,2})[\/\-.](\d{1,2})/)
    if (m) return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`
    return s.trim()
}
