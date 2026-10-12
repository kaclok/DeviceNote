/** 行情展示层格式化工具 */

/** 涨跌颜色 class：A股习惯红涨绿跌 */
export function trendClass(value) {
    const num = Number(value)
    if (!Number.isFinite(num) || num === 0) return 'tx-flat'
    return num > 0 ? 'tx-up' : 'tx-down'
}

/** 涨跌幅：+9.99% / -3.20% */
export function fmtPct(value, digits = 2) {
    const num = Number(value)
    if (!Number.isFinite(num)) return '-'
    return `${num > 0 ? '+' : ''}${num.toFixed(digits)}%`
}

/** 金额（元）→ 亿/万，用于成交额、封单额、市值 */
export function fmtMoney(value, digits = 2) {
    const num = Number(value)
    if (!Number.isFinite(num)) return '-'
    const abs = Math.abs(num)
    if (abs >= 1e8) return `${(num / 1e8).toFixed(digits)}亿`
    if (abs >= 1e4) return `${(num / 1e4).toFixed(digits)}万`
    return num.toFixed(digits)
}

/** 带正负号的金额：主力净流入用 */
export function fmtMoneySigned(value, digits = 2) {
    const num = Number(value)
    if (!Number.isFinite(num)) return '-'
    return `${num > 0 ? '+' : ''}${fmtMoney(num, digits)}`
}

/** 20261009 / 2026-10-09 → 2026-10-09 */
export function fmtDate(value) {
    const text = String(value ?? '').trim()
    if (/^\d{8}$/.test(text)) {
        return `${text.slice(0, 4)}-${text.slice(4, 6)}-${text.slice(6, 8)}`
    }
    return text || '-'
}

/** 9 → 「9连板」；1 → 「首板」 */
export function fmtBoards(boards) {
    const num = Number(boards) || 1
    return num <= 1 ? '首板' : `${num}连板`
}

/** 连板阶梯的标签：一板 / 二板 … */
const BOARD_LABELS = ['', '一板', '二板', '三板', '四板', '五板', '六板', '七板', '八板', '九板']
export function fmtBoardLevel(boards) {
    const num = Number(boards) || 0
    return num < BOARD_LABELS.length ? BOARD_LABELS[num] : `${num}板`
}

/** 按连板数给颜色（越高越红） */
export function boardColor(boards) {
    const num = Number(boards) || 0
    if (num >= 5) return '#ef232a'
    if (num >= 3) return '#f97316'
    if (num >= 2) return '#eab308'
    return '#64748b'
}

/** 通用降序排序的取值器 */
export function byDesc(key) {
    return (a, b) => Number(b?.[key] ?? 0) - Number(a?.[key] ?? 0)
}
