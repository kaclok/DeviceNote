<template>
    <div class="page">
        <el-alert v-if="errorMsg" class="error-tip" type="error" show-icon :closable="false" :title="errorMsg"/>

        <div class="stat-row">
            <StatCard title="涨停家数" :value="nums.ZT ?? ladder.total ?? '-'" unit="家" tone="up"/>
            <StatCard title="最高连板" :value="ladder.max_boards ? ladder.max_boards + '板' : '-'" tone="up"
                      :sub="`天梯 ${ladder.total ?? '-'} 只`"/>
            <StatCard title="炸板率" :value="nums.ZBL !== undefined ? nums.ZBL.toFixed(2) + '%' : '-'"
                      tone="flat" sub="全市场炸板占比"/>
            <StatCard title="昨日涨停今日表现" :value="fmtPct(nums.yestRase)" tone="primary"
                      :sub="'= 昨日涨停股今日平均涨幅'"/>
            <StatCard title="上涨 / 下跌" :value="`${nums.SZJS ?? '-'} / ${nums.XDJS ?? '-'}`" tone="flat"/>
            <StatCard title="盘面事件" :value="pmsl.total ?? '-'" unit="条" tone="flat" sub="开盘红盘面梳理"/>
        </div>

        <div class="grid-2">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">涨停天梯</span>
                    <span class="panel-sub">开盘红 · 含一字 / 人气标记</span>
                </div>
                <div class="list-scroll">
                    <div v-for="level in ladder.levels || []" :key="level.boards" class="ladder-level">
                        <div class="ladder-badge" :style="{color: boardColor(level.boards)}">
                            {{ level.boards }}板
                            <small>{{ level.count }} 家</small>
                        </div>
                        <div class="ladder-stocks">
                            <span v-for="stock in level.stocks" :key="stock.code" class="stock-chip">
                                {{ stock.name }}
                                <small>{{ stock.sector_name }}</small>
                                <em v-if="stock.one_word" class="mark">一字</em>
                                <em v-if="stock.popular" class="mark hot">人气</em>
                            </span>
                        </div>
                    </div>
                    <div v-if="!(ladder.levels || []).length" class="empty-tip">当日无涨停天梯数据</div>
                </div>
            </div>

            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">盘面梳理</span>
                    <span class="panel-sub">按时间顺序的板块 / 个股事件流</span>
                </div>
                <div class="list-scroll">
                    <div v-for="(event, index) in pmsl.list || []" :key="event.time + index" class="event-item">
                        <span class="event-time">{{ event.time }}</span>
                        <span class="event-tag" :class="eventTone(event.tone)">{{ event.tag }}</span>
                        <div class="event-detail">
                            <span v-if="event.sector" class="event-sector">【{{ event.sector }}】</span>
                            {{ event.detail }}
                            <span v-if="(event.stocks || []).length" class="event-stocks">
                                {{ event.stocks.map((s) => s.name).join('、') }}
                            </span>
                        </div>
                    </div>
                    <div v-if="!(pmsl.list || []).length" class="empty-tip">暂无盘面事件</div>
                </div>
            </div>
        </div>

        <div class="panel">
            <div class="panel-head">
                <span class="panel-title">历史涨停复盘</span>
                <span class="panel-sub">按板块分组 · 含连板描述与涨停原因（开盘红）</span>
            </div>
            <div class="factor-grid">
                <div v-for="group in groups" :key="group.sector_code + group.sector_name" class="hs-group">
                    <div class="hs-head">
                        <span class="hs-name">{{ group.sector_name }}</span>
                        <span class="hs-count">{{ group.count }} 家</span>
                    </div>
                    <div v-for="stock in group.stocks" :key="stock.code" class="hs-stock">
                        <span class="hs-code">{{ stock.name }}</span>
                        <span class="board-tag" :style="{color: boardColor(stock.boards)}">
                            {{ stock.board_desc || fmtBoards(stock.boards) }}
                        </span>
                        <span class="theme-tag">{{ stock.reason_short || '未标注' }}</span>
                        <div class="hs-detail" :title="stock.reason_detail">{{ stock.reason_detail }}</div>
                    </div>
                </div>
                <div v-if="!groups.length" class="empty-tip">暂无历史涨停复盘数据</div>
            </div>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, reactive} from 'vue'
import StatCard from '../components/StatCard.vue'
import {fetchHistory, fetchLadder, fetchPmsl} from '../system/ApiX.js'
import {usePolling} from '../system/PollX.js'
import {markUpdated, store} from '../system/Store.js'
import {boardColor, fmtBoards, fmtPct} from '../utils/FmtX.js'

const ladder = reactive({})
const pmsl = reactive({})
const history = reactive({groups: []})

const {errorMsg} = usePolling(async () => {
    const [ladderData, pmslData, historyData] = await Promise.all([
        fetchLadder(store.tradeDate),
        fetchPmsl(store.tradeDate, 60),
        fetchHistory(store.tradeDate, 120),
    ])
    Object.assign(ladder, ladderData)
    Object.assign(pmsl, pmslData)
    Object.assign(history, historyData)
    markUpdated()
})

const nums = computed(() => history.nums || {})
const groups = computed(() => history.groups || [])

/** TagShuXing：2=正面 0=负面 1=中性 */
function eventTone(tone) {
    if (tone === 2) return 'tone-up'
    if (tone === 0) return 'tone-down'
    return 'tone-flat'
}
</script>

<style scoped>
.event-tag.tone-up {
    background: rgba(239, 35, 42, .12);
    color: var(--up);
}

.event-tag.tone-down {
    background: rgba(18, 185, 129, .12);
    color: var(--down);
}

.event-tag.tone-flat {
    background: rgba(139, 154, 181, .14);
    color: var(--tx-muted);
}

.event-stocks {
    margin-left: 6px;
    color: var(--accent);
}

.mark {
    margin-left: 5px;
    padding: 0 4px;
    border-radius: 3px;
    font-size: 10px;
    font-style: normal;
    background: rgba(234, 179, 8, .16);
    color: #eab308;
}

.mark.hot {
    background: rgba(239, 35, 42, .16);
    color: var(--up);
}

/* 历史复盘分组卡片 */
.hs-group {
    background: var(--bg-panel-2);
    border: 1px solid var(--bg-border);
    border-radius: 10px;
    padding: 10px 12px;
}

.hs-head {
    display: flex;
    align-items: center;
    margin-bottom: 6px;
}

.hs-name {
    font-size: 14px;
    font-weight: 600;
    color: var(--accent);
}

.hs-count {
    margin-left: auto;
    font-size: 12px;
    color: var(--tx-muted);
}

.hs-stock {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    padding: 6px 0;
    border-top: 1px dashed rgba(36, 48, 74, .8);
}

.hs-code {
    flex: 0 0 66px;
    font-weight: 600;
}

.hs-detail {
    flex: 1 1 100%;
    color: var(--tx-muted);
    font-size: 12px;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>
