import http from './Http.js'

/**
 * 行情服务接口清单（本地 Python 服务 tool/stock-service，端口 7300）
 * 全部 GET；`date` 统一传 "YYYY-MM-DD"，留空由服务端取今天。
 */

/** 交易日：今天是否交易日 + 近 20 个交易日 */
export function fetchTradeMeta() {
    return http.get('/meta/trade')
}

/** 大盘总览：指数、情绪、涨停家数、连板梯队、涨停板块 Top */
export function fetchOverview(date) {
    return http.get('/market/overview', {params: {date}})
}

/** 指数周期 K 线：period ∈ day/week/month/year（雪球源，含成交额）
 *  动态分页：params {limit, end}，end=游标日期（取该日及更早），响应带 has_more */
export function fetchIndexKline(code, period = 'day', params = {}) {
    return http.get('/index/kline', {params: {code, period, ...params}})
}

/** 指数分时线：分钟点 + 昨收（Ashare/腾讯源）；date 非空 = 历史分时 */
export function fetchIndexMinute(code, date = '') {
    return http.get('/index/minute', {params: {code, date}})
}

/** 涨停榜 */
export function fetchLimitUp(date) {
    return http.get('/limit/up', {params: {date}})
}

/** 涨停原因（财联社，仅当天） */
export function fetchLimitUpReason(date) {
    return http.get('/limit/up-reason', {params: {date}})
}

/** 昨日涨停今日表现 */
export function fetchYesterdayLimitUp(date) {
    return http.get('/limit/yesterday', {params: {date}})
}

/** 涨停股按所属板块聚合 + 行业板块行情 + 风口板块 */
export function fetchSectorLimitStats(date) {
    return http.get('/sector/limit-stats', {params: {date}})
}

/** 板块热度排行 */
export function fetchSectorHeat() {
    return http.get('/sector/heat')
}

/** 板块轮动（近 N 个交易日 Top10） */
export function fetchSectorRotation(days = 4) {
    return http.get('/sector/rotation', {params: {days}})
}

/** 涨停归因：按因素（政策/市场/外围/题材/业绩/公告/资金/技术/风险）归类涨停原因 */
export function fetchReasonAnalyze(date) {
    return http.get('/reason/analyze', {params: {date}})
}

/** 财联社电报 */
export function fetchTelegraph(date, category = 'important', limit = 40) {
    return http.get('/news/telegraph', {params: {date, category, limit}})
}

/** 涨停天梯 */
export function fetchLadder(date) {
    return http.get('/review/ladder', {params: {date}})
}

/** 盘面梳理（板块事件流） */
export function fetchPmsl(date, size = 50) {
    return http.get('/review/pmsl', {params: {date, size}})
}

/** 历史涨停复盘（按板块分组 + 涨停原因） */
export function fetchHistory(date, size = 100) {
    return http.get('/review/history', {params: {date, size}})
}

/** 同花顺人气股 */
export function fetchHotRank(limit = 50) {
    return http.get('/stock/hot', {params: {limit}})
}

/** 个股联想搜索（腾讯 smartbox：代码/名称/拼音，只返回 A 股个股） */
export function fetchStockSearch(q, limit = 8) {
    return http.get('/stock/search', {params: {q, limit}})
}

/** 个股分时：分钟点 + 现价/昨收 + 板块指数叠加线 board */
export function fetchStockMinute(code, date = '') {
    return http.get('/stock/minute', {params: {code, date}})
}

/** 个股周期 K 线：period ∈ day/week/month/year（财联社源，含成交额）
 *  动态分页：params {limit, end}，end=游标日期（取该日及更早），响应带 has_more */
export function fetchStockKline(code, period = 'day', params = {}) {
    return http.get('/stock/kline', {params: {code, period, ...params}})
}
