<template>
    <div class="page">
        <el-alert
            v-if="errorMsg"
            class="error-tip"
            type="error"
            show-icon
            :closable="false"
            :title="errorMsg"
            description="行情服务未启动时请先运行 tool/stock-service/start.bat（端口 7300）"
        />

        <div class="stat-row">
            <StatCard title="涨停家数" :value="data.limit_up_count ?? '-'" unit="家" tone="up"
                      :sub="`实际涨停 ${data.emotion?.sjzt ?? '-'} · ST ${data.emotion?.stzt ?? '-'}`"/>
            <StatCard title="最高连板" :value="data.highest_boards ? data.highest_boards + '板' : '-'"
                      tone="up" :sub="highestNames"/>
            <StatCard title="封板率" :value="data.sentiment?.up_ratio ?? '-'" tone="primary"
                      :sub="`封板 ${data.sentiment?.up_ratio_num ?? '-'} · 炸板 ${data.sentiment?.broken_num ?? '-'}`"/>
            <StatCard title="市场热度" :value="data.sentiment?.market_degree ?? '-'" unit="/100"
                      tone="primary" :sub="data.emotion?.sign || ''"/>
            <StatCard title="两市成交额" :value="data.sentiment?.turnover || '-'" tone="flat"
                      :sub="`较上日 ${data.sentiment?.turnover_change || '-'}`"/>
        </div>

        <div class="panel">
            <div class="panel-head">
                <span class="panel-title">大盘指数</span>
                <span class="panel-sub">{{ data.is_today ? '实时' : '仅当日实时接口，历史日期不展示' }}</span>
            </div>
            <div class="stat-row">
                <StatCard
                    v-for="index in data.indices || []"
                    :key="index.code"
                    :title="index.name"
                    :value="index.price?.toFixed?.(2) ?? '-'"
                    :tone="Number(index.change_pct) >= 0 ? 'up' : 'down'"
                    :sub="`${fmtPct(index.change_pct)} · 成交 ${fmtMoney(index.amount)}`"
                />
                <div v-if="!(data.indices || []).length" class="empty-tip">暂无指数数据</div>
            </div>

            <div class="kline-bar">
                <el-radio-group v-model="klinePeriod" size="small">
                    <el-radio-button value="minute">分时</el-radio-button>
                    <el-radio-button value="day">日K</el-radio-button>
                    <el-radio-button value="week">周K</el-radio-button>
                    <el-radio-button value="month">月K</el-radio-button>
                    <el-radio-button value="year">年K</el-radio-button>
                </el-radio-group>
                <el-radio-group v-model="klineCode" size="small">
                    <el-radio-button v-for="ix in INDEXES" :key="ix.code" :value="ix.code">
                        {{ ix.name }}
                    </el-radio-button>
                </el-radio-group>
                <div v-if="klinePeriod !== 'minute'" class="ind-bar">
                    <el-check-tag v-for="ind in INDICATORS" :key="ind.key"
                                  :checked="inds.includes(ind.key)" @change="toggleInd(ind.key)">
                        {{ ind.label }}
                    </el-check-tag>
                </div>
            </div>
            <ChartBox :option="klineOpt" :isEmpty="!chartRows.length" :height="klineChartH"
                      :loading="klineLoading" :emptyText="klineError || '暂无指数数据'"
                      @data-zoom="onKlineZoom"/>
        </div>

        <div class="grid-2">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">连板梯队分布</span>
                    <span class="panel-sub">按连板数统计家数</span>
                </div>
                <ChartBox :option="ladderOption" :isEmpty="!ladderRows.length" :height="266"/>
            </div>

            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">涨停板块 Top10</span>
                    <span class="panel-sub">涨停股所属板块家数</span>
                </div>
                <ChartBox :option="upSectorOption" :isEmpty="!upSectors.length" :height="266"/>
            </div>
        </div>

        <div class="grid-2">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">涨停天梯</span>
                    <span class="panel-sub">来自涨停池连板数</span>
                </div>
                <div class="list-scroll">
                    <div v-for="level in data.ladder || []" :key="level.boards" class="ladder-level">
                        <div class="ladder-badge" :style="{color: boardColor(level.boards)}">
                            {{ level.boards }}板
                            <small>{{ level.count }} 家</small>
                        </div>
                        <div class="ladder-stocks">
                            <span v-for="stock in level.stocks" :key="stock.code" class="stock-chip">
                                {{ stock.name }}<small>{{ stock.sector }}</small>
                            </span>
                        </div>
                    </div>
                    <div v-if="!(data.ladder || []).length" class="empty-tip">当日无涨停股票</div>
                </div>
            </div>

            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">涨跌分布</span>
                    <span class="panel-sub dist-sum">
                        上涨 <b class="num-up">{{ distSummary.rise }}</b>
                        · 平盘 <b>{{ distSummary.flat }}</b>
                        · 下跌 <b class="num-down">{{ distSummary.fall }}</b>
                        · 共 <b>{{ distSummary.total }}</b> 家
                    </span>
                </div>
                <ChartBox :option="distOption" :isEmpty="!distRows.length" :height="266"/>
            </div>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, reactive, ref, watch} from 'vue'
import ChartBox from '../components/ChartBox.vue'
import StatCard from '../components/StatCard.vue'
import {fetchIndexKline, fetchIndexMinute, fetchOverview} from '../system/ApiX.js'
import {usePolling} from '../system/PollX.js'
import {markUpdated, store} from '../system/Store.js'
import {INDICATORS, useKlineControls, useKlinePaging} from '../system/useKline.js'
import {hbarOption, klineOption, minuteOption, vbarOption} from '../utils/ChartX.js'
import {boardColor, fmtMoney, fmtPct} from '../utils/FmtX.js'

const data = reactive({})

// 涨跌分布标题摘要：上涨/平盘/下跌/总家数（emotion 概览，缺失降级 '-'）
// ⚠️ 上游值可能是字符串（service 透传 raw_emotion 原值）⇒ 显示保留原值，
// 总数用 Number 归一后求和（Number(null)=0，须先用 nullish 降级挡掉缺失）
const distSummary = computed(() => {
    const e = data.emotion || {}
    const nums = [e.rise_num, e.fall_num, e.flat].map((v) => (v == null ? NaN : Number(v)))
    const total = nums.every((v) => Number.isFinite(v)) ? nums.reduce((a, b) => a + b, 0) : '-'
    return {rise: e.rise_num ?? '-', flat: e.flat ?? '-', fall: e.fall_num ?? '-', total}
})

const {errorMsg} = usePolling(async () => {
    Object.assign(data, await fetchOverview(store.tradeDate))
    markUpdated()
    loadKline(false) // 指数 K 线跟随轮询：只刷新最新一段，保留已加载历史与视图
})

// ---------------------------------------------------------------- 指数图表 --

const INDEXES = [
    {code: '000001.SH', name: '上证指数'},
    {code: '399006.SZ', name: '创业板指'},
    {code: '000688.SH', name: '科创50'},
]
const klineCode = ref(INDEXES[0].code)
const minuteData = ref(null)        // 分时 {list, prev_close}
const klineLoading = ref(false)
const klineError = ref('')

// 周期 K 动态分页（system/useKline.js，与个股行情页共用一份实现）：
// 初始拉最近 120 根，dataZoom 拖到最左再向头部补 250 根
const {bars: klineBars, view: klineView, load: loadKlinePage, refresh: refreshKlinePage,
       loadOlder: loadOlderKline}
    = useKlinePaging((q) => fetchIndexKline(q.code, q.period,
        // K线展示到选中交易日（end 游标）；loadOlder 传了更早的游标则优先
        {limit: q.limit, end: q.end || store.tradeDate}))

// 周期与指标开关（system/useKline.js，与个股行情页共用一份实现）
const {klinePeriod, inds, toggleInd, klineChartH, indFlags} = useKlineControls()

// 序号守卫：切指数/切周期/轮询并发在途时，只让最新一次请求落结果
let klineSeq = 0

async function loadKline(reset = true) {
    const seq = ++klineSeq
    klineLoading.value = reset   // 轮询跟随不进全屏 loading，避免闪烁
    try {
        if (klinePeriod.value === 'minute') {
            const res = await fetchIndexMinute(klineCode.value, store.tradeDate)
            if (seq !== klineSeq) return
            minuteData.value = res
            klineError.value = (res.list || []).length ? '' : '上游暂无该指数数据'
        } else if (reset) {
            await loadKlinePage(klineCode.value, klinePeriod.value)
            if (seq !== klineSeq) return
            klineError.value = klineBars.value.length ? '' : '上游暂无该指数数据'
        } else {
            await refreshKlinePage(klineCode.value, klinePeriod.value)
        }
    } catch (exc) {
        if (seq !== klineSeq) return
        minuteData.value = null
        klineBars.value = []
        klineError.value = exc?.response?.data?.detail || '指数数据加载失败'
    } finally {
        if (seq === klineSeq) klineLoading.value = false
    }
}

watch([klineCode, klinePeriod], () => loadKline(), {immediate: true})
// 切交易日：K 线/分时都跟日期走（其它面板由 usePolling 的 tradeDate watcher 重拉）
watch(() => store.tradeDate, () => loadKline())

/** dataZoom 拖/缩到最左时向头部补历史。
 *  ⚠️ 事件 payload 常只有百分比 start/end（slider/inside 均有此形态），
 *  startValue 优先、缺省退回 start 百分比（<=0 即最左） */
function onKlineZoom(e) {
    if (klinePeriod.value === 'minute') return
    const d = e?.batch?.[0] || e || {}
    const sv = d.startValue ?? d.start
    if (sv == null || sv > 0) return
    loadOlderKline(klineCode.value, klinePeriod.value, {
        startValue: Number.isFinite(d.startValue) ? d.startValue : 0,
        endValue: Number.isFinite(d.endValue) ? d.endValue : null,
    })
}

const chartRows = computed(() =>
    klinePeriod.value === 'minute' ? (minuteData.value?.list || []) : klineBars.value)

const klineOpt = computed(() => klinePeriod.value === 'minute'
    ? minuteOption(chartRows.value, minuteData.value?.prev_close)
    : klineOption(chartRows.value, indFlags.value, klineChartH.value, klineView.value))

const ladderRows = computed(() => data.ladder || [])
const upSectors = computed(() => data.top_sectors?.up || [])

const highestNames = computed(() => {
    const top = (data.ladder || [])[0]
    if (!top) return ''
    return top.stocks.map((s) => s.name).join(' / ')
})

const ladderOption = computed(() => vbarOption(
    ladderRows.value.map((l) => `${l.boards}板`),
    ladderRows.value.map((l) => l.count),
    {color: '#ef232a'},
))

const upSectorOption = computed(() => hbarOption(
    upSectors.value.map((s) => s.name),
    upSectors.value.map((s) => s.count),
    '#ef232a',
))

// 把 rise_dist / fall_dist 拼成一条从 -10 到 +10 的分布
const distRows = computed(() => {
    const rise = data.emotion?.rise_dist || {}
    const fall = data.emotion?.fall_dist || {}
    const rows = []
    for (let i = -10; i <= 10; i++) {
        if (i === 0) continue
        const count = i > 0 ? rise[String(i)] : fall[String(i)]
        rows.push({label: i > 0 ? `涨${i}` : `跌${-i}`, count: count || 0, up: i > 0})
    }
    return rows
})

const distOption = computed(() => ({
    grid: {left: 8, right: 12, top: 24, bottom: 8, containLabel: true},
    tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
    xAxis: {
        type: 'category',
        data: distRows.value.map((r) => r.label),
        axisLine: {lineStyle: {color: '#2c3a58'}},
        axisTick: {show: false},
        axisLabel: {color: '#8b9ab5', fontSize: 10, interval: 0, rotate: 45},
    },
    yAxis: {
        type: 'value',
        axisLabel: {color: '#8b9ab5', fontSize: 11},
        splitLine: {lineStyle: {color: 'rgba(44,58,88,.55)', type: 'dashed'}},
    },
    series: [{
        type: 'bar',
        barWidth: 12,
        data: distRows.value.map((r) => ({
            value: r.count,
            itemStyle: {color: r.up ? '#ef232a' : '#12b981', borderRadius: [3, 3, 0, 0]},
        })),
    }],
}))
</script>
