import {computed, reactive} from 'vue'

/**
 * 入口级共享状态：交易日、市场状态、刷新档位。
 * 顶栏（home.vue）负责写入，各业务页只读。
 */
export const store = reactive({
    /** 当前查看的交易日，格式 YYYY-MM-DD */
    tradeDate: '',
    /** 近 20 个交易日，格式 YYYYMMDD */
    tradeDays: [],
    /** 今天是否交易日 */
    isTradeDay: true,
    /** 1=交易中 2=已收盘 3=非交易日 */
    marketStatus: 0,
    /** 轮询间隔（秒），0 = 关闭 */
    intervalSec: 30,
    /** 最近一次成功取数时间 */
    updatedAt: '',
    /** 手动刷新信号：顶栏「刷新」按钮递增，各页 watch 后重取 */
    refreshTick: 0,
})

/** 当前是否查看「今天」 */
export const isToday = computed(() => {
    if (!store.tradeDate) return true
    const now = new Date()
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    return store.tradeDate === today
})

/** 是否需要轮询：开关打开 + 看的是今天 + 盘中 */
export const shouldPoll = computed(
    () => store.intervalSec > 0 && isToday.value && store.marketStatus === 1,
)

export const MARKET_STATUS_TEXT = {
    1: '交易中',
    2: '已收盘',
    3: '非交易日',
}

export function markUpdated() {
    const now = new Date()
    store.updatedAt = [now.getHours(), now.getMinutes(), now.getSeconds()]
        .map((n) => String(n).padStart(2, '0'))
        .join(':')
}

export function triggerRefresh() {
    store.refreshTick += 1
}
