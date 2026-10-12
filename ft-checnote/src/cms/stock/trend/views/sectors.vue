<template>
    <div class="page">
        <el-alert v-if="errorMsg" class="error-tip" type="error" show-icon :closable="false" :title="errorMsg"/>

        <div class="stat-row">
            <StatCard title="涨停覆盖板块" :value="upSectors.length" unit="个" tone="up"
                      :sub="`涨停 ${data.up?.total ?? '-'} 家`"/>
            <StatCard title="行业板块红盘" :value="industryUpCount" unit="个" tone="up"
                      :sub="`共 ${industry.length} 个行业板块`"/>
            <StatCard title="行业主力净流入" :value="fmtMoneySigned(industryInflow)" tone="flat"
                      :sub="`统计口径：${store.tradeDate}`"/>
        </div>

        <div class="panel">
            <div class="panel-head">
                <span class="panel-title">涨停板块聚合</span>
                <span class="panel-sub">涨停股所属板块家数（含最高连板）</span>
            </div>
            <div class="list-scroll">
                <div v-for="item in upSectors.slice(0, 20)" :key="item.name" class="sector-row">
                    <span class="sector-name" :title="item.name">{{ item.name }}</span>
                    <span class="sector-bar">
                        <i :style="{width: barWidth(item.count, data.up?.total), background: '#ef232a'}"></i>
                    </span>
                    <span class="sector-count">{{ item.count }} 家</span>
                    <span class="sector-stocks" :title="namesOf(item.stocks)">
                        {{ fmtBoards(item.max_boards) }} · {{ namesOf(item.stocks) }}
                    </span>
                </div>
                <div v-if="!upSectors.length" class="empty-tip">当日无涨停板块</div>
            </div>
        </div>

        <div class="grid-2">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">行业板块涨幅榜</span>
                    <span class="panel-sub">财联社行业板块涨跌幅</span>
                </div>
                <ChartBox :option="industryOption" :isEmpty="!industry.length" :height="330"/>
            </div>

            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">板块热度榜</span>
                    <span class="panel-sub">财联社热度值 · 括号为排名变化</span>
                </div>
                <ChartBox :option="heatOption" :isEmpty="!heat.length" :height="330"/>
            </div>
        </div>

        <div class="grid-2">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">风口板块</span>
                    <span class="panel-sub">财联社当日风口及催化剂</span>
                </div>
                <div class="list-scroll">
                    <div v-for="(item, index) in wind" :key="item.plate_name || index" class="news-item">
                        <span class="news-time">{{ item.plate_name }}</span>
                        <div class="news-title">{{ item.catalyst || '暂无催化剂说明' }}</div>
                    </div>
                    <div v-if="!wind.length" class="empty-tip">暂无风口板块数据（仅当日提供）</div>
                </div>
            </div>

            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">板块轮动</span>
                    <span class="panel-sub">近 {{ rotation.length || 4 }} 个交易日 Top10</span>
                </div>
                <div class="list-scroll">
                    <div v-for="day in rotation" :key="day.trade_date" class="rotation-day">
                        <div class="rotation-date">{{ fmtDate(day.trade_date) }}</div>
                        <div class="rotation-plates">
                            <span v-for="plate in day.plates" :key="plate.plate_code" class="stock-chip">
                                {{ plate.plate_name }}
                                <small :class="trendClass(plate.change_pct)">{{ fmtPct(plate.change_pct) }}</small>
                            </span>
                        </div>
                    </div>
                    <div v-if="!rotation.length" class="empty-tip">暂无板块轮动数据</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, reactive} from 'vue'
import ChartBox from '../components/ChartBox.vue'
import StatCard from '../components/StatCard.vue'
import {fetchSectorHeat, fetchSectorLimitStats, fetchSectorRotation} from '../system/ApiX.js'
import {usePolling} from '../system/PollX.js'
import {markUpdated, store} from '../system/Store.js'
import {hbarOption} from '../utils/ChartX.js'
import {fmtBoards, fmtDate, fmtMoneySigned, fmtPct, trendClass} from '../utils/FmtX.js'

const data = reactive({})
const heat = reactive([])
const rotation = reactive([])

const {errorMsg} = usePolling(async () => {
    const [stats, heatList, rotationList] = await Promise.all([
        fetchSectorLimitStats(store.tradeDate),
        fetchSectorHeat(),
        fetchSectorRotation(4),
    ])
    Object.assign(data, stats)
    Object.assign(heat, heatList)
    Object.assign(rotation, rotationList)
    markUpdated()
})

const upSectors = computed(() => data.up?.sectors || [])
const industry = computed(() => data.industry || [])
const wind = computed(() => data.wind || [])

const industryUpCount = computed(() => industry.value.filter((s) => s.change_pct > 0).length)
const industryInflow = computed(() => industry.value.reduce((sum, s) => sum + (s.main_fund_diff || 0), 0))

/** 涨幅前 16 个行业：条形长度用绝对值，标签显示带符号涨跌幅，红涨绿跌 */
const industryOption = computed(() => {
    const top = [...industry.value]
        .sort((a, b) => b.change_pct - a.change_pct)
        .slice(0, 16)
    const pctByName = new Map(top.map((s) => [s.name, s.change_pct]))
    return hbarOption(
        top.map((s) => s.name),
        top.map((s) => Math.abs(s.change_pct)),
        top.map((s) => (s.change_pct >= 0 ? '#ef232a' : '#12b981')),
        {formatter: (params) => fmtPct(pctByName.get(params.name))},
    )
})

const heatOption = computed(() => {
    const top = [...heat].sort((a, b) => a.rank - b.rank).slice(0, 16)
    return hbarOption(
        top.map((s) => s.plate_name),
        top.map((s) => s.cur_heat),
        top.map((s) => (s.is_new ? '#eab308' : '#4c8dff')),
    )
})

function barWidth(count, total) {
    const percent = total ? (count / total) * 100 : 0
    return `${Math.max(percent, 3)}%`
}

function namesOf(stocks) {
    return (stocks || []).map((s) => s.name).join('、')
}
</script>

<style scoped>
.rotation-day {
    display: flex;
    gap: 10px;
    padding: 8px 0;
    border-bottom: 1px dashed rgba(36, 48, 74, .8);
}

.rotation-day:last-child {
    border-bottom: none;
}

.rotation-date {
    flex: 0 0 84px;
    color: var(--tx-muted);
    font-variant-numeric: tabular-nums;
}

.rotation-plates {
    flex: 1 1 auto;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
}
</style>
