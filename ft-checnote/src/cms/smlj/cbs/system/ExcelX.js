/*
 * cbs 模块专用 Excel 读写（唯一入口）。
 * 导出/导出模板/批量导入三处共用，避免各页面各写一套。
 * 依赖 xlsx（与 cghtz 台账同一套库）。
 */
import * as XLSX from 'xlsx'

function stamp() {
    const d = new Date()
    const p = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}${p(d.getMonth() + 1)}${p(d.getDate())}${p(d.getHours())}${p(d.getMinutes())}`
}

/** 单元格值 → 纯文本（Date 统一成 yyyy-MM-dd HH:mm） */
export function cellText(v) {
    if (v === null || v === undefined) return ''
    if (v instanceof Date) {
        // Excel 日期序列转 Date 会差几十毫秒（08:30 变 08:29:59.99），先四舍五入到整分
        const t = new Date(Math.round(v.getTime() / 60000) * 60000)
        const p = n => String(n).padStart(2, "0")
        return `${t.getFullYear()}-${p(t.getMonth() + 1)}-${p(t.getDate())} ${p(t.getHours())}:${p(t.getMinutes())}`
    }
    return String(v).trim()
}

/**
 * 导出 xlsx。
 * @param filename 文件名（不含扩展名，自动追加时间戳）
 * @param sheetName 工作表名
 * @param headers [{label, value: row => any, width?}]
 * @param rows 数据行
 */
export function exportXlsx(filename, sheetName, headers, rows) {
    const aoa = [headers.map(h => h.label)]
    rows.forEach(r => aoa.push(headers.map(h => h.value(r))))
    const ws = XLSX.utils.aoa_to_sheet(aoa)
    ws['!cols'] = headers.map(h => ({wch: h.width || 16}))
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, sheetName.slice(0, 31))
    XLSX.writeFile(wb, `${filename}_${stamp()}.xlsx`)
}

/** 导出空模板（表头 + 一行示例），文件名固定不带时间戳，便于分发 */
export function exportTemplate(filename, sheetName, headers, sample) {
    const aoa = [headers.map(h => h.label)]
    if (sample) aoa.push(headers.map(h => sample[h.label] ?? ''))
    const ws = XLSX.utils.aoa_to_sheet(aoa)
    ws['!cols'] = headers.map(h => ({wch: h.width || 18}))
    const wb = XLSX.utils.book_new()
    XLSX.utils.book_append_sheet(wb, ws, sheetName.slice(0, 31))
    XLSX.writeFile(wb, `${filename}.xlsx`)
}

/** 读第一个工作表为二维数组（含表头行），日期保留为 Date */
export async function readSheet(file) {
    const buf = await file.arrayBuffer()
    const wb = XLSX.read(buf, {type: 'array', cellDates: true})
    const ws = wb.Sheets[wb.SheetNames[0]]
    if (!ws) return []
    return XLSX.utils.sheet_to_json(ws, {header: 1, defval: '', blankrows: false})
}
