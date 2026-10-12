import {computed, onActivated, onDeactivated, onMounted, onUnmounted, ref, watch} from 'vue'
import {shouldPoll, store} from './Store.js'

/** 距下一次自动刷新的倒计时（秒）。0 = 无倒计时（未激活页 / 关闭轮询 / 非今天 / 非盘中）。
 *  模块级共享：顶栏（home.vue）直接读；由当前激活页的 usePolling 心跳驱动。 */
export const refreshCountdown = ref(0)

let cdOwner = null   // 当前持有倒计时的 usePolling 实例令牌（keep-alive 下同刻至多一个激活页）

function cdClaim(token, seconds) {
    cdOwner = token
    refreshCountdown.value = seconds
}

function cdRelease(token) {
    if (cdOwner !== token) return   // 只有持有者能释放（防页面切换时旧页误清新页的倒计时）
    cdOwner = null
    refreshCountdown.value = 0
}

/**
 * 行情轮询（页面级唯一入口）：
 * - 挂载 / keep-alive 激活时立刻取数（首次显式 loading，之后静默刷新，避免列表闪烁）；
 * - 心跳逐秒递减 refreshCountdown，归零即取数并复位 —— 手动刷新 / 切交易日 / 换档位都会重置；
 * - 在途去重：上一拍没回来，这一拍直接跳过。**绝不去 abort 自己上一次**，
 *   否则 5s 间隔 + 8s 响应会永远拿不到数据；
 * - 页面隐藏（切 tab）暂停递减，切回后下一秒补拍；
 * - 非交易日 / 已收盘 / 关闭轮询自动停（`shouldPoll`），倒计时随之归零隐藏。
 *
 * @param task 取数函数，必须返回 Promise；内部已 try/catch，一次失败不会卡死心跳
 */
export function usePolling(task) {
    const loading = ref(false)
    const errorMsg = ref('')
    const loaded = ref(false)

    let timer = null
    let running = false
    let active = false
    const token = {}   // 倒计时所有权令牌

    async function run(silent = false) {
        if (running) return
        running = true
        if (!silent) loading.value = true
        try {
            await task()
            errorMsg.value = ''
            loaded.value = true
        } catch (error) {
            errorMsg.value = error?.message || String(error)
            if (!silent) console.warn('[行情]', errorMsg.value)
        } finally {
            running = false
            loading.value = false
        }
    }

    function stop() {
        if (timer) {
            clearInterval(timer)
            timer = null
        }
    }

    /** 逐秒心跳：倒计时 >1 递减；归零打一拍（页面隐藏时挂起，切回下一秒补拍）并复位 */
    function tick() {
        if (!active || !shouldPoll.value) { schedule(); return }
        if (cdOwner !== token) return   // 已让位给新激活页
        if (refreshCountdown.value > 1) {
            refreshCountdown.value--
            return
        }
        if (document.hidden) return
        run(true)
        refreshCountdown.value = store.intervalSec
    }

    /** 重排心跳：倒计时复位为满档位（激活 / 手动刷新 / 切交易日 / 换档位都会走到） */
    function schedule() {
        stop()
        if (!active || !shouldPoll.value) { cdRelease(token); return }
        cdClaim(token, store.intervalSec)
        timer = setInterval(tick, 1000)
    }

    function activate() {
        active = true
        run(loaded.value)
        schedule()
    }

    function deactivate() {
        active = false
        stop()
        cdRelease(token)
    }

    // 交易日切换：未激活的页面不重复取数；取数 + 倒计时复位重排
    watch(() => store.tradeDate, () => {
        if (active) { run(false); schedule() }
    })
    // 顶栏「刷新」按钮：取数 + 倒计时复位重排
    watch(() => store.refreshTick, () => {
        if (active) { run(false); schedule() }
    })
    // 刷新档位 / 市场状态变化：重排心跳
    watch(() => [store.intervalSec, store.marketStatus].join('|'), () => {
        if (active) schedule()
    })

    onMounted(activate)
    onActivated(activate)
    onDeactivated(deactivate)
    onUnmounted(deactivate)

    return {loading, errorMsg, loaded, run, schedule}
}

/** 顶栏时钟：返回 {date, week, time} = 年月日 / 周几 / HH:MM:SS，秒级刷新 */
export function useClock() {
    const now = ref(new Date())
    const timer = setInterval(() => {
        now.value = new Date()
    }, 1000)
    onUnmounted(() => clearInterval(timer))
    const p2 = (n) => String(n).padStart(2, '0')
    return {
        date: computed(() =>
            `${now.value.getFullYear()}年${now.value.getMonth() + 1}月${now.value.getDate()}日`),
        week: computed(() => '周' + '日一二三四五六'[now.value.getDay()]),
        time: computed(() =>
            [now.value.getHours(), now.value.getMinutes(), now.value.getSeconds()]
                .map(p2).join(':')),
    }
}
