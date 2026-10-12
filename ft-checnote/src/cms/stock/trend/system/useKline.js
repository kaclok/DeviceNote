import {computed, ref} from 'vue'

/**
 * 周期 K 线的周期档与技术指标开关（大盘总览 / 个股行情共用，同一件事只允许一份实现）。
 * 指标 keys 与 ChartX.klineOption(bars, inds, chartH) 的 inds 字段一一对应。
 */

export const INDICATORS = [
    {key: 'ma', label: '均线'},
    {key: 'boll', label: 'BOLL'},
    {key: 'macd', label: 'MACD'},
    {key: 'vol', label: '成交量'},
]

/**
 * 周期与指标状态：默认五档周期从「分时」起步、全部指标开启。
 * @param {number} minuteH 分时档容器高度（px），周期 K 以此为基数按副图数撑高
 */
export function useKlineControls(minuteH = 340) {
    const klinePeriod = ref('minute')   // minute | day | week | month | year
    // 默认全部指标开启（随 INDICATORS 扩展自动跟随）
    const inds = ref(INDICATORS.map((it) => it.key))

    function toggleInd(key) {
        const set = new Set(inds.value)
        set.has(key) ? set.delete(key) : set.add(key)
        inds.value = [...set]
    }

    // 副图（MACD/成交量）每开一个加高 104px，主图由 klineOption 内部压缩
    const klineChartH = computed(() => {
        if (klinePeriod.value === 'minute') return minuteH
        let n = 0
        for (const key of ['macd', 'vol']) {
            if (inds.value.includes(key)) n++
        }
        return minuteH + n * 104
    })

    /** klineOption 的 inds 入参形态 */
    const indFlags = computed(() => ({
        ma: inds.value.includes('ma'),
        boll: inds.value.includes('boll'),
        macd: inds.value.includes('macd'),
        vol: inds.value.includes('vol'),
    }))

    return {klinePeriod, inds, toggleInd, klineChartH, indFlags}
}

/**
 * 周期 K 动态分页（大盘总览 / 个股行情共用，同一件事只允许一份实现）：
 * 初始拉最近 initial 根，dataZoom 拖到最左（startValue<=0）时按批向头部补历史。
 * @param {{code:string, period:string, limit?:number, end?:string}} q
 * @returns {Promise<{list:Array, has_more:boolean}>}
 *   fetcher 由页面注入（fetchIndexKline / fetchStockKline），响应须带 has_more
 */
export function useKlinePaging(fetcher, {initial = 120, batch = 250} = {}) {
    const bars = ref([])            // 当前已加载的全部 bar（升序）
    const hasMore = ref(false)      // 后端还有更早历史
    const view = ref(null)          // {start,end} bar 索引，补数后恢复 dataZoom 视图
    const loadingMore = ref(false)
    let seq = 0                     // 载入序号：切股/切周期/重复触发只让最新一次生效
    let oldest = ''                 // 当前最老一根日期（end 游标）

    /** 初始加载（切指数/个股/周期时调用）；返回加载后的 bar 数 */
    async function load(code, period) {
        const my = ++seq
        view.value = null
        loadingMore.value = false
        try {
            const res = await fetcher({code, period, limit: initial})
            if (my !== seq) return bars.value.length
            bars.value = res.list || []
            hasMore.value = !!res.has_more
            oldest = bars.value.length ? bars.value[0].date : ''
        } catch {
            if (my === seq) {
                bars.value = []
                hasMore.value = false
            }
        }
        return bars.value.length
    }

    /** 轮询跟随：只刷新最新一段并按日期合并，保留已加载历史与视图（头部没变 ⇒ 索引稳定） */
    async function refresh(code, period) {
        const my = ++seq
        try {
            const res = await fetcher({code, period, limit: initial})
            if (my !== seq) return bars.value.length
            const fresh = res.list || []
            if (!fresh.length || !bars.value.length) return bars.value.length
            const cutoff = fresh[0].date
            bars.value = [...bars.value.filter((b) => b.date < cutoff), ...fresh]
            oldest = bars.value.length ? bars.value[0].date : oldest
        } catch { /* 轮询静默，不抢页面错误提示 */ }
        return bars.value.length
    }

    /**
     * 向头部补一批历史。dz = 触发时 dataZoom 的 {startValue,endValue}（相对旧数组，
     * 拖到最左时 startValue<=0，可为负）；补完后把用户窗口整体平移保持所见内容。
     * 返回是否拉到了新数据。
     */
    async function loadOlder(code, period, dz = null) {
        if (!hasMore.value || loadingMore.value || !bars.value.length) return false
        const my = ++seq
        loadingMore.value = true
        try {
            const res = await fetcher({code, period, limit: batch, end: oldest})
            if (my !== seq) return false
            // end 游标含当日 ⇒ 可能与现有最老一根重叠，只接更早的
            const add = (res.list || []).filter((b) => b.date < oldest)
            if (!add.length) {
                hasMore.value = false
                return false
            }
            const oldLen = bars.value.length
            const shift = add.length
            const sv = Number.isFinite(dz?.startValue) ? dz.startValue : 0
            const ev = Number.isFinite(dz?.endValue) ? dz.endValue : oldLen - 1
            view.value = {start: sv + shift, end: ev + shift}
            bars.value = [...add, ...bars.value]
            oldest = bars.value[0].date
            // 本批近乎拿不满 ⇒ 后端已到头
            hasMore.value = !!res.has_more && add.length >= batch - 2
            return true
        } catch {
            return false
        } finally {
            if (my === seq) loadingMore.value = false
        }
    }

    return {bars, hasMore, view, loadingMore, load, refresh, loadOlder}
}
