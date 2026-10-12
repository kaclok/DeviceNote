<template>
    <div class="app-shell">
        <header class="app-header">
            <div class="brand">
                <span class="brand-dot"></span>
                <span class="brand-title">股票行情复盘</span>
                <span class="brand-sub">涨停统计 · 板块聚合 · 涨停归因</span>
            </div>

            <div class="status-badge" :class="`st-${store.marketStatus}`">
                {{ MARKET_STATUS_TEXT[store.marketStatus] || '加载中' }}
            </div>
            <div v-if="store.updatedAt" class="updated">更新于 {{ store.updatedAt }}</div>
            <div class="clock">{{ clock.date }} {{ clock.week }} {{ clock.time }}</div>

            <div class="header-spacer"></div>

            <div class="field">
                <span class="field-label">交易日</span>
                <el-date-picker
                    v-model="store.tradeDate" type="date" size="small"
                    style="width: 140px" value-format="YYYY-MM-DD"
                    :clearable="false" :teleported="true"
                    :disabled-date="disableFuture" placeholder="交易日"/>
            </div>

            <div class="field">
                <span class="field-label">刷新</span>
                <el-select v-model="store.intervalSec" size="small" style="width: 100px">
                    <el-option v-for="opt in INTERVAL_OPTIONS" :key="opt.value"
                               :label="opt.label" :value="opt.value"/>
                </el-select>
            </div>

            <el-button size="small" type="primary" plain :loading="metaLoading" @click="refresh">
                立即刷新
            </el-button>
            <span v-if="refreshCountdown > 0" class="refresh-countdown">{{ refreshCountdown }}s 后刷新</span>
        </header>

        <div class="app-body">
            <aside class="app-side">
                <el-menu :default-active="$route.path" router class="side-menu">
                    <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
                        {{ item.title }}
                    </el-menu-item>
                </el-menu>
                <div class="side-foot">
                    <div>数据源 levistock</div>
                    <div>东财 · 财联社 · 开盘红</div>
                    <div class="side-foot-api">:7300</div>
                </div>
            </aside>

            <main class="app-main">
                <router-view v-slot="{Component}">
                    <keep-alive>
                        <component :is="Component"/>
                    </keep-alive>
                </router-view>
            </main>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, onMounted, ref, watch} from 'vue'
import {useRoute} from 'vue-router'
import {fetchOverview, fetchTradeMeta} from '../system/ApiX.js'
import {refreshCountdown, useClock} from '../system/PollX.js'
import {MARKET_STATUS_TEXT, markUpdated, store, triggerRefresh} from '../system/Store.js'
import {fmtDate} from '../utils/FmtX.js'
import {routers} from '../router/Router.js'

const INTERVAL_OPTIONS = [
    {label: '关闭', value: 0},
    {label: '10 秒', value: 10},
    {label: '30 秒', value: 30},
    {label: '60 秒', value: 60},
]

const route = useRoute()
const clock = useClock()
const metaLoading = ref(false)

// 菜单直接取自路由表子节点，新增页面无需改两处
const menus = computed(() => {
    const home = routers.find((r) => r.name === 'home')
    return (home?.children || [])
        .filter((child) => child.meta?.title)
        .map((child) => ({
            path: `/${home.path.replace(/^\//, '')}/${child.path}`,
            title: child.meta.title,
        }))
})

// 交易日日历：禁选未来日期（历史任选，非交易日由各接口自行返回空数据）
const disableFuture = (d) => d.getTime() > Date.now()

/** 顶栏市场状态：来自总览接口，各业务页据此判断是否轮询 */
async function loadStatus() {
    try {
        const data = await fetchOverview(store.tradeDate)
        store.marketStatus = data.market_status
        store.isTradeDay = data.is_trade_day
        markUpdated()
    } catch (error) {
        console.warn('[行情] 市场状态获取失败：', error?.message || error)
    }
}

async function initMeta() {
    metaLoading.value = true
    try {
        const meta = await fetchTradeMeta()
        const days = meta.trade_days || []
        store.tradeDays = days
        store.isTradeDay = meta.is_trade_day
        // 数据源不保证顺序，取最大日期作默认：当日开盘→当日；否则→最近一个交易日（后端 trade_meta 保证列表末尾为上一个交易日）
        const latest = days.reduce((max, day) => (String(day) > String(max) ? day : max), '')
        store.tradeDate = fmtDate(latest || meta.today)
    } catch (error) {
        console.warn('[行情] 交易日加载失败：', error?.message || error)
    } finally {
        metaLoading.value = false
    }
}

function refresh() {
    triggerRefresh()
    loadStatus()
}

onMounted(async () => {
    await initMeta()
    await loadStatus()
})

// 切交易日：重算市场状态（各业务页由 usePolling 内部的 watcher 自行重取）
watch(() => store.tradeDate, loadStatus)

// 路由变化时同步页面标题
watch(() => route.name, () => {
    document.title = `${route.meta?.title ? route.meta.title + ' · ' : ''}股票行情复盘`
}, {immediate: true})
</script>
