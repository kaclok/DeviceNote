export function fmtDistance(meters) {
    if (meters === null || meters === undefined || Number.isNaN(meters)) return '-'
    if (meters >= 1000) return `${(meters / 1000).toFixed(2)} 公里`
    return `${Math.round(meters)} 米`
}

export function fmtDuration(seconds) {
    if (seconds === null || seconds === undefined || Number.isNaN(seconds)) return '-'
    if (seconds < 60) return `${Math.max(1, Math.round(seconds))} 秒`
    return `${Math.max(1, Math.round(seconds / 60))} 分钟`
}
