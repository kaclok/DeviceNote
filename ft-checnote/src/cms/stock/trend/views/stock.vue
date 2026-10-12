<template>
    <div class="page">
        <div class="panel">
            <div class="panel-head">
                <span class="panel-title">个股行情</span>
                <el-select
                    v-model="picked" class="stock-search" filterable remote clearable
                    :remote-method="doSearch" :loading="searching" value-key="code"
                    placeholder="搜索个股：代码 / 名称 / 拼音（如 600519 / 茅台 / maotai）"
                    @change="onPick">
                    <el-option v-for="opt in searchItems" :key="opt.market + opt.code"
                               :value="opt" :label="`${opt.name} ${opt.code}`">
                        <span class="opt-name">{{ opt.name }}</span>
                        <small class="opt-code">{{ opt.market.toUpperCase() }} · {{ opt.code }}</small>
                    </el-option>
                </el-select>
                <span v-if="quote" class="panel-sub stock-quote">
                    现价
                    <b :style="{color: quote.pct === null ? '' : (quote.pct >= 0 ? 'var(--up)' : 'var(--down)')}">
                        {{ quote.price }}
                    </b>
                    <template v-if="quote.pct !== null">
                        <b :style="{color: quote.pct >= 0 ? 'var(--up)' : 'var(--down)'}">{{ quoteText }}</b>
                    </template>
                </span>
            </div>

            <div class="kline-bar">
                <el-radio-group v-model="klinePeriod" size="small" :disabled="!stock">
                    <el-radio-button v-for="p in PERIODS" :key="p.value" :value="p.value">
                        {{ p.label }}
                    </el-radio-button>
                </el-radio-group>
                <div v-if="klinePeriod !== 'minute' && stock" class="ind-bar">
                    <el-check-tag v-for="ind in INDICATORS" :key="ind.key"
                                  :checked="inds.includes(ind.key)" @change="toggleInd(ind.key)">
                        {{ ind.label }}
                    </el-check-tag>
                </div>
            </div>

            <ChartBox :option="klineOpt" :isEmpty="!chartRows.length" :height="klineChartH"
                      :loading="klineLoading" :emptyText="klineError || '请先在上方搜索并选择个股'"
                      @data-zoom="onKlineZoom"/>
            <div v-if="minuteData?.board" class="board-note">
                板块线：{{ minuteData.board.sector_name }} → {{ minuteData.board.index_name }}（右轴，虚线）
            </div>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, ref, watch} from 'vue'
import ChartBox from '../components/ChartBox.vue'
import {fetchStockKline, fetchStockMinute, fetchStockSearch} from '../system/ApiX.js'
import {usePolling} from '../system/PollX.js'
import {markUpdated, store} from '../system/Store.js'
import {INDICATORS, useKlineControls, useKlinePaging} from '../system/useKline.js'
import {klineOption, minuteOption} from '../utils/ChartX.js'

const PERIODS = [
    {value: 'minute', label: '分时'},
    {value: 'day', label: '日K'},
    {value: 'week', label: '周K'},
    {value: 'month', label: '月K'},
    {value: 'year', label: '年K'},
]

// ---- 搜索（远程联想，序号守卫防过期结果回落） ----
const picked = ref(null)
const stock = computed(() => picked.value)
const searchItems = ref([])
const searching = ref(false)
let searchSeq = 0

async function doSearch(q) {
    const kw = String(q || '').trim()
    if (!kw) {
        searchItems.value = []
        return
    }
    const seq = ++searchSeq
    searching.value = true
    try {
        const res = await fetchStockSearch(kw)
        if (seq === searchSeq) searchItems.value = res.list || []
    } catch {
        if (seq === searchSeq) searchItems.value = []
    } finally {
        if (seq === searchSeq) searching.value = false
    }
}

function onPick() {
    // picked 已由 v-model 更新；清空时回到引导空态
    if (!picked.value) {
        klineBars.value = []
        minuteData.value = null
        klineError.value = ''
    }
}

// ---- 周期与指标（与大盘总览共用 useKlineControls） ----
const {klinePeriod, inds, toggleInd, klineChartH, indFlags} = useKlineControls()

const minuteData = ref(null)        // 分时 {list, prev_close, price, board}
const klineLoading = ref(false)
const klineError = ref('')

// 周期 K 动态分页（system/useKline.js，与大盘总览页共用一份实现）：
// 初始拉最近 120 根，dataZoom 拖到最左再向头部补 250 根
const {bars: klineBars, view: klineView, load: loadKlinePage, refresh: refreshKlinePage,
       loadOlder: loadOlderKline}
    = useKlinePaging((q) => fetchStockKline(q.code, q.period,
        // K线展示到选中交易日（end 游标）；loadOlder 传了更早的游标则优先
        {limit: q.limit, end: q.end || store.tradeDate}))

// 序号守卫：切股/切周期/轮询并发在途时，只让最新一次请求落结果
let klineSeq = 0

async function loadKline(reset = true) {
    if (!stock.value) return
    const seq = ++klineSeq
    klineLoading.value = reset   // 轮询跟随不进全屏 loading，避免闪烁
    try {
        if (klinePeriod.value === 'minute') {
            const res = await fetchStockMinute(stock.value.code, store.tradeDate)
            if (seq !== klineSeq) return
            minuteData.value = res
            klineError.value = (res.list || []).length ? '' : '上游暂无该股分时数据'
        } else if (reset) {
            await loadKlinePage(stock.value.code, klinePeriod.value)
            if (seq !== klineSeq) return
            klineError.value = klineBars.value.length ? '' : '上游暂无该股 K 线数据'
        } else {
            await refreshKlinePage(stock.value.code, klinePeriod.value)
        }
    } catch (exc) {
        if (seq !== klineSeq) return
        klineError.value = exc?.response?.data?.detail || '个股数据加载失败'
    } finally {
        if (seq === klineSeq) klineLoading.value = false
    }
}

watch([() => picked.value?.code, klinePeriod], () => loadKline())
// 切交易日：K 线/分时都跟日期走（其它面板由 usePolling 的 tradeDate watcher 重拉）
watch(() => store.tradeDate, () => loadKline())

/** dataZoom 拖/缩到最左时向头部补历史。
 *  ⚠️ 事件 payload 常只有百分比 start/end（slider/inside 均有此形态），
 *  startValue 优先、缺省退回 start 百分比（<=0 即最左） */
function onKlineZoom(e) {
    if (klinePeriod.value === 'minute' || !stock.value) return
    const d = e?.batch?.[0] || e || {}
    const sv = d.startValue ?? d.start
    if (sv == null || sv > 0) return
    loadOlderKline(stock.value.code, klinePeriod.value, {
        startValue: Number.isFinite(d.startValue) ? d.startValue : 0,
        endValue: Number.isFinite(d.endValue) ? d.endValue : null,
    })
}

const chartRows = computed(() =>
    klinePeriod.value === 'minute' ? (minuteData.value?.list || []) : klineBars.value)

const klineOpt = computed(() => {
    if (klinePeriod.value !== 'minute') {
        return klineOption(chartRows.value, indFlags.value, klineChartH.value, klineView.value)
    }
    const md = minuteData.value
    return minuteOption(chartRows.value, md?.prev_close, md?.board ? {
        board: {
            name: md.board.index_name || md.board.sector_name,
            points: md.board.points,
            prevClose: md.board.prev_close,
        },
    } : {})
})

// ---- 报价摘要：分时用 qt 现价/昨收，周期 K 用末根 close/change_pct ----
const quote = computed(() => {
    if (klinePeriod.value === 'minute') {
        const md = minuteData.value
        if (!md || !Number.isFinite(md.price)) return null
        const pct = Number.isFinite(md.prev_close) && md.prev_close > 0
            ? (md.price / md.prev_close - 1) * 100 : null
        return {price: md.price.toFixed(2), pct}
    }
    const last = klineBars.value[klineBars.value.length - 1]
    if (!last) return null
    return {price: Number(last.close).toFixed(2),
            pct: Number.isFinite(last.change_pct) ? last.change_pct : null}
})
const quoteText = computed(() => quote.value && quote.value.pct !== null
    ? `${quote.value.pct >= 0 ? '+' : ''}${quote.value.pct.toFixed(2)}%` : '')

// ---- 轮询跟随顶栏心跳：选了个股才刷新（分时/周期 K 服务端都有短缓存） ----
usePolling(async () => {
    if (stock.value) await loadKline(false) // 只刷新最新一段，保留已加载历史与视图
    markUpdated()
})
</script>

<style scoped>
.stock-search {
    width: 340px;
}

.stock-quote b {
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    margin-left: 4px;
}

.opt-code {
    margin-left: 8px;
    color: var(--tx-muted);
}

.board-note {
    margin-top: 6px;
    font-size: 12px;
    color: var(--tx-muted);
}
</style>
