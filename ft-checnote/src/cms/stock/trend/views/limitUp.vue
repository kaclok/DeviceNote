<template>
    <div class="page">
        <el-alert v-if="errorMsg" class="error-tip" type="error" show-icon :closable="false" :title="errorMsg"/>

        <div class="stat-row">
            <StatCard title="涨停家数" :value="payload.total ?? '-'" unit="家" tone="up"
                      :sub="`${store.tradeDate} ${isToday ? '（今日）' : '（历史）'}`"/>
            <StatCard title="连板及以上" :value="connectedCount" unit="家" tone="up"
                      :sub="`最高 ${maxBoards} 连板`"/>
            <StatCard title="首板" :value="firstBoardCount" unit="家" tone="primary"/>
            <StatCard title="炸板股" :value="brokenCount" unit="家" tone="flat" sub="盘中打开过涨停"/>
            <StatCard title="合计成交额" :value="fmtMoney(totalAmount)" tone="flat"/>
        </div>

        <div class="grid-3">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">连板梯队</span>
                    <span class="panel-sub">按连板数分档</span>
                </div>
                <ChartBox :option="boardOption" :isEmpty="!rows.length" :height="264"/>
            </div>
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">所属板块分布</span>
                    <span class="panel-sub">涨停股行业板块</span>
                </div>
                <ChartBox :option="sectorPie" :isEmpty="!sectorRows.length" :height="264"/>
            </div>
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">首次涨停时间</span>
                    <span class="panel-sub">每半小时封板家数</span>
                </div>
                <ChartBox :option="timeOption" :isEmpty="!timeRows.length" :height="264"/>
            </div>
        </div>

        <div class="panel">
            <div class="panel-head">
                <span class="panel-title">涨停股明细</span>
                <span class="panel-sub">共 {{ rows.length }} 只 · 点击表头可排序</span>
                <div class="panel-grow"></div>
                <el-input v-model="keyword" size="small" placeholder="按名称/代码/板块/原因筛选"
                          clearable style="width: 260px"/>
            </div>
            <div class="table-wrap">
                <LimitTable :rows="filteredRows" :loading="loading"/>
            </div>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, reactive, ref} from 'vue'
import ChartBox from '../components/ChartBox.vue'
import LimitTable from '../components/LimitTable.vue'
import StatCard from '../components/StatCard.vue'
import {fetchLimitUp} from '../system/ApiX.js'
import {usePolling} from '../system/PollX.js'
import {isToday, markUpdated, store} from '../system/Store.js'
import {hbarOption, pieOption} from '../utils/ChartX.js'
import {fmtMoney} from '../utils/FmtX.js'

const payload = reactive({total: 0, list: []})
const keyword = ref('')

const {loading, errorMsg} = usePolling(async () => {
    Object.assign(payload, await fetchLimitUp(store.tradeDate))
    markUpdated()
})

const rows = computed(() => payload.list || [])
const maxBoards = computed(() => rows.value.reduce((max, s) => Math.max(max, s.boards), 0))
const connectedCount = computed(() => rows.value.filter((s) => s.boards >= 2).length)
const firstBoardCount = computed(() => rows.value.filter((s) => s.boards <= 1).length)
const brokenCount = computed(() => rows.value.filter((s) => s.broken_times > 0).length)
const totalAmount = computed(() => rows.value.reduce((sum, s) => sum + (s.amount || 0), 0))

const filteredRows = computed(() => {
    const text = keyword.value.trim()
    if (!text) return rows.value
    return rows.value.filter((s) =>
        [s.code, s.name, s.sector, s.reason].some((field) => String(field || '').includes(text)))
})

// 连板梯队
const boardOption = computed(() => {
    const buckets = new Map()
    rows.value.forEach((s) => buckets.set(s.boards, (buckets.get(s.boards) || 0) + 1))
    const levels = [...buckets.entries()].sort((a, b) => b[0] - a[0])
    return hbarOption(
        levels.map(([boards]) => (boards <= 1 ? '首板' : `${boards}连板`)),
        levels.map(([, count]) => count),
        '#ef232a',
    )
})

// 板块占比
const sectorRows = computed(() => {
    const buckets = new Map()
    rows.value.forEach((s) => buckets.set(s.sector, (buckets.get(s.sector) || 0) + 1))
    return [...buckets.entries()]
        .map(([name, value]) => ({name, value}))
        .sort((a, b) => b.value - a.value)
})

const PIE_COLORS = ['#ef232a', '#f97316', '#eab308', '#4c8dff', '#8b5cf6',
    '#12b981', '#06b6d4', '#ec4899', '#64748b', '#a855f7']

const sectorPie = computed(() => {
    const top = sectorRows.value.slice(0, 9)
    const rest = sectorRows.value.slice(9).reduce((sum, s) => sum + s.value, 0)
    const items = top.map((s, index) => ({...s, color: PIE_COLORS[index % PIE_COLORS.length]}))
    if (rest > 0) items.push({name: '其他', value: rest, color: '#475569'})
    return pieOption(items)
})

// 首次涨停时间分布（每 30 分钟一档，覆盖 09:25 - 15:00）
const timeRows = computed(() => {
    const buckets = new Map()
    for (let h = 9; h <= 14; h++) {
        for (const m of [0, 30]) {
            if (h === 9 && m === 0) continue
            buckets.set(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`, 0)
        }
    }
    buckets.set('15:00', 0)

    rows.value.forEach((stock) => {
        const time = stock.first_limit_time
        if (!time) return
        const [hh, mm] = time.split(':').map(Number)
        const key = `${String(hh).padStart(2, '0')}:${mm < 30 ? '00' : '30'}`
        buckets.set(key, (buckets.get(key) || 0) + 1)
    })

    return [...buckets.entries()].sort((a, b) => a[0].localeCompare(b[0]))
})

const timeOption = computed(() => ({
    grid: {left: 8, right: 12, top: 20, bottom: 8, containLabel: true},
    tooltip: {trigger: 'axis', axisPointer: {type: 'shadow'}},
    xAxis: {
        type: 'category',
        data: timeRows.value.map(([label]) => label),
        axisLine: {lineStyle: {color: '#2c3a58'}},
        axisTick: {show: false},
        axisLabel: {color: '#8b9ab5', fontSize: 10, interval: 0, rotate: 60},
    },
    yAxis: {
        type: 'value',
        axisLabel: {color: '#8b9ab5', fontSize: 11},
        splitLine: {lineStyle: {color: 'rgba(44,58,88,.55)', type: 'dashed'}},
    },
    series: [{
        type: 'bar',
        barWidth: 10,
        data: timeRows.value.map(([, count]) => count),
        itemStyle: {color: '#ef232a', borderRadius: [3, 3, 0, 0]},
    }],
}))
</script>

<style scoped>
.table-wrap {
    height: calc(100vh - 470px);
    min-height: 320px;
}
</style>
