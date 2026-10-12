<template>
    <div class="page">
        <el-alert v-if="errorMsg" class="error-tip" type="error" show-icon :closable="false" :title="errorMsg"/>

        <div class="stat-row">
            <StatCard title="涨停样本" :value="data.total ?? '-'" unit="家" tone="up"
                      :sub="`${store.tradeDate}${data.reasons_notice ? ' · ' + data.reasons_notice : ''}`"/>
            <StatCard title="已归类样本" :value="data.covered ?? '-'" unit="家" tone="primary"
                      :sub="`共 ${factors.length} 类因素命中`"/>
            <StatCard title="涉及板块" :value="sectors.length" unit="个" tone="flat"/>
            <StatCard title="市场热度" :value="emotion.zt ?? '-'" unit="家" tone="flat"
                      :sub="emotion.sign || ''"/>
        </div>

        <div class="grid-2">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">因素占比</span>
                    <span class="panel-sub">样本按因素归类（一只股票可命中多因素）</span>
                </div>
                <ChartBox :option="factorPie" :isEmpty="!factors.length" :height="300"/>
            </div>
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">因素家数</span>
                    <span class="panel-sub">命中股票数排序</span>
                </div>
                <ChartBox :option="factorBar" :isEmpty="!factors.length" :height="300"/>
            </div>
        </div>

        <div class="panel">
            <div class="panel-head">
                <span class="panel-title">因素明细</span>
                <span class="panel-sub">关键词 / 聚集板块 / 代表个股</span>
            </div>
            <div class="factor-grid">
                <div v-for="item in factors" :key="item.factor_code" class="factor-card"
                     :style="{borderTop: `2px solid ${item.color}`}">
                    <div class="factor-head">
                        <span class="factor-name" :style="{color: item.color}">{{ item.factor_name }}</span>
                        <span class="factor-count" :style="{color: item.color}">{{ item.count }}</span>
                        <span class="factor-ratio">{{ item.ratio }}%</span>
                    </div>
                    <div class="factor-keywords">
                        <span v-for="word in item.keywords" :key="word" class="theme-tag">{{ word }}</span>
                    </div>
                    <div v-if="item.sectors.length" class="factor-stocks">
                        聚集板块：<b>{{ item.sectors.map((s) => `${s.name}(${s.count})`).join('、') }}</b>
                    </div>
                    <div class="factor-stocks">
                        代表个股：{{ item.stocks.map((s) => s.name).join('、') || '（消息面样本，无个股）' }}
                    </div>
                </div>
                <div v-if="!factors.length" class="empty-tip">暂无可归类的因素</div>
            </div>
        </div>

        <div class="grid-2">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">涨停原因明细</span>
                    <span class="panel-sub">财联社涨停原因（仅当日提供）</span>
                </div>
                <div class="list-scroll">
                    <div v-for="item in reasons" :key="item.code + item.theme" class="reason-item">
                        <div class="reason-head">
                            <span class="reason-name">{{ item.name }}</span>
                            <span class="theme-tag">{{ item.theme || '未标注' }}</span>
                            <span :class="trendClass(item.change_pct)">{{ fmtPct(item.change_pct) }}</span>
                        </div>
                        <div class="news-content">{{ item.detail || '（无详细说明）' }}</div>
                    </div>
                    <div v-if="!reasons.length" class="empty-tip">
                        当日涨停原因暂不可用（该数据源仅提供当天数据）
                    </div>
                </div>
            </div>

            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">风口催化剂</span>
                    <span class="panel-sub">财联社风口板块</span>
                </div>
                <div class="list-scroll">
                    <div v-for="(item, index) in wind" :key="item.plate_name || index" class="reason-item">
                        <div class="reason-head">
                            <span class="theme-tag">{{ item.plate_name }}</span>
                        </div>
                        <div class="news-content">{{ item.catalyst || '暂无催化剂说明' }}</div>
                    </div>
                    <div v-if="!wind.length" class="empty-tip">暂无风口板块数据</div>
                </div>
            </div>
        </div>

        <div class="grid-2">
            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">市场情绪</span>
                    <span class="panel-sub">开盘红市场情绪快照</span>
                </div>
                <div class="chip-row">
                    <span class="stock-chip">涨停 <b class="tx-up">{{ emotion.zt ?? '-' }}</b></span>
                    <span class="stock-chip">实际涨停 <b class="tx-up">{{ emotion.sjzt ?? '-' }}</b></span>
                    <span class="stock-chip">上涨 <b class="tx-up">{{ emotion.rise_num ?? '-' }}</b></span>
                    <span class="stock-chip">下跌 <b class="tx-down">{{ emotion.fall_num ?? '-' }}</b></span>
                    <span class="stock-chip">平盘 <b>{{ emotion.flat ?? '-' }}</b></span>
                    <span v-if="emotion.sign" class="stock-chip">情绪 <b>{{ emotion.sign }}</b></span>
                </div>
            </div>

            <div class="panel">
                <div class="panel-head">
                    <span class="panel-title">重要电报</span>
                    <span class="panel-sub">财联社加红消息 · 归因文本来源</span>
                </div>
                <div class="list-scroll">
                    <div v-for="(item, index) in news" :key="item.time + index" class="news-item">
                        <span class="news-time">{{ item.time }}</span>
                        <div class="news-title">
                            {{ item.title }}
                            <div class="news-content">{{ item.content }}</div>
                        </div>
                    </div>
                    <div v-if="!news.length" class="empty-tip">暂无电报数据</div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, reactive} from 'vue'
import ChartBox from '../components/ChartBox.vue'
import StatCard from '../components/StatCard.vue'
import {fetchReasonAnalyze} from '../system/ApiX.js'
import {usePolling} from '../system/PollX.js'
import {markUpdated, store} from '../system/Store.js'
import {hbarOption, pieOption} from '../utils/ChartX.js'
import {fmtPct, trendClass} from '../utils/FmtX.js'

const data = reactive({})

const {errorMsg} = usePolling(async () => {
    Object.assign(data, await fetchReasonAnalyze(store.tradeDate))
    markUpdated()
})

const factors = computed(() => data.factors || [])
const sectors = computed(() => data.sectors || [])
const wind = computed(() => data.wind || [])
const reasons = computed(() => data.reasons || [])
const news = computed(() => data.news || [])
const emotion = computed(() => data.emotion || {})

const factorPie = computed(() => pieOption(
    factors.value.map((f) => ({name: f.factor_name, value: f.count, color: f.color})),
))

const factorBar = computed(() => hbarOption(
    factors.value.map((f) => f.factor_name),
    factors.value.map((f) => f.count),
    factors.value.map((f) => f.color),
))
</script>

<style scoped>
.reason-item {
    padding: 8px 2px;
    border-bottom: 1px dashed rgba(36, 48, 74, .8);
}

.reason-item:last-child {
    border-bottom: none;
}

.reason-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 2px;
}

.reason-name {
    font-weight: 600;
    color: var(--tx-main);
}

.reason-time {
    flex: 0 0 44px;
    color: var(--accent);
    font-variant-numeric: tabular-nums;
    font-size: 12px;
}

.chip-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.chip-row b {
    margin-left: 4px;
    font-weight: 700;
}
</style>
