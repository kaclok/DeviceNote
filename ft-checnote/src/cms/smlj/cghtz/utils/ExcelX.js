import * as XLSX from 'xlsx'

/**
 * 合同台账 - Excel 导入/导出工具
 * 基于工程已依赖的 xlsx 库，提供模板下载、导入解析、导出生成。
 */

/* ---------------- 导出 ---------------- */
/**
 * 将合同数据导出为 xlsx 并触发浏览器下载
 * @param rows 合同数组（含 planDate / payStatus 展示字段）
 * @param filename 文件名（不含扩展名）
 */
export function exportContractExcel(rows, filename = '合同台账_导出') {
    const headers = [
        '合同编号', '合同名称', '签订人', '签订方式', '供应商',
        '合同金额(元)', '签订时间', '付款方式', '计划付款日期', '付款状态', '是否已入库', '备注',
    ]
    const data = rows.map(c => ({
        '合同编号': c.no,
        '合同名称': c.name,
        '签订人': c.signer,
        '签订方式': c.method,
        '供应商': c.supplier,
        '合同金额(元)': c.amount,
        '签订时间': c.signDate,
        '付款方式': c.payMethod,
        '计划付款日期': c.planDate ?? '',
        '付款状态': c.payStatus ?? '',
        '是否已入库': c.stock,
        '备注': c.remark ?? '',
    }))

    const sheet = XLSX.utils.json_to_sheet(data, {header: headers})
    // 设置列宽
    sheet['!cols'] = headers.map(h => ({wch: h.includes('合同') ? 22 : h.includes('供应商') ? 30 : 14}))
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '合同台账')

    const stamp = new Date().toISOString().slice(0, 10)
    XLSX.writeFile(wb, `${filename}_${stamp}.xlsx`)
}

/* ---------------- 模板下载 ---------------- */
export function downloadTemplate() {
    const headers = ['合同编号*', '合同名称*', '签订人', '合同签订方式', '供应商*', '合同金额(元)*', '签订时间*', '付款方式', '付款周期(月)', '计划付款金额', '是否已入库', '备注']
    const example = [
        'SMLJ-CG-CL-26330', '螺栓', '薛少军', '网络询比价', '榆林景云五金机电设备有限公司',
        3836.92, '2026-08-01', '货到票到3个月付款', 3, 3836.92, '未入库', '示例行，可删除',
    ]
    const sheet = XLSX.utils.aoa_to_sheet([headers, example])
    sheet['!cols'] = headers.map(h => ({wch: h.includes('合同') ? 22 : h.includes('供应商') ? 30 : 12}))
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, sheet, '导入模板')
    XLSX.writeFile(wb, '合同台账导入模板.xlsx')
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
                const rows = raw.map(r => ({
                    no: String(r['合同编号'] ?? r['合同编号*'] ?? '').trim(),
                    name: String(r['合同名称'] ?? r['合同名称*'] ?? '').trim(),
                    signer: String(r['签订人'] ?? '').trim(),
                    method: String(r['合同签订方式'] ?? '').trim(),
                    supplier: String(r['供应商'] ?? r['供应商*'] ?? '').trim(),
                    amount: r['合同金额(元)'] ?? r['合同金额(元)*'],
                    signDate: formatDate(r['签订时间'] ?? r['签订时间*']),
                    payMethod: String(r['付款方式'] ?? '').trim(),
                    payCycleMonths: Number(r['付款周期(月)'] ?? 0),
                    plannedAmount: r['计划付款金额'] ?? '',
                    stock: String(r['是否已入库'] ?? '').trim() || '未入库',
                    remark: String(r['备注'] ?? '').trim(),
                }))
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
    if (!v) return ''
    // Excel 序列号日期
    if (typeof v === 'number') {
        const d = new Date(Math.round((v - 25569) * 86400 * 1000))
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }
    const s = String(v)
    // 2026/8/1 或 2026-8-1 归一化
    const m = s.match(/(\d{4})[\/\-.](\d{1,2})[\/\-.](\d{1,2})/)
    if (m) return `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`
    return s
}
