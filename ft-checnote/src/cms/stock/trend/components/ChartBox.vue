<template>
    <div class="chart-box" :style="{height: height + 'px'}">
        <div ref="el" class="chart-canvas"></div>
        <div v-if="isEmpty" class="chart-empty">{{ emptyText }}</div>
        <div v-if="loading" v-loading="true" element-loading-background="rgba(15,20,32,.65)" class="chart-mask"></div>
    </div>
</template>

<script setup lang="js">
import {onActivated, onBeforeUnmount, onMounted, nextTick, ref, watch} from 'vue'
import {echarts} from '@/plugins/EchartsCore.js'

// option 由页面用 computed 生成：依赖变化即返回新引用，因此 watch 不需要 deep
const props = defineProps({
    option: {type: Object, required: true},
    height: {type: Number, default: 320},
    loading: {type: Boolean, default: false},
    emptyText: {type: String, default: '暂无数据'},
    isEmpty: {type: Boolean, default: false},
})

// 透传 echarts 交互事件（当前用途：K 线 dataZoom 拖到最左触发动态加载历史）
const emit = defineEmits(['dataZoom'])

const el = ref(null)
let chart = null
// 自持 ResizeObserver：框架的 v-listen-resize 判定写反了，不可用
let observer = null

function render() {
    if (!chart) return
    chart.setOption(props.option, {notMerge: true, lazyUpdate: true})
}

onMounted(async () => {
    await nextTick()
    chart = echarts.init(el.value, null, {renderer: 'canvas'})
    chart.on('datazoom', (e) => emit('dataZoom', e))
    render()
    observer = new ResizeObserver(() => chart && chart.resize())
    observer.observe(el.value)
})

watch(() => props.option, render)

// keep-alive 回来时容器曾 display:none，尺寸可能为 0，需要补一次 resize
onActivated(async () => {
    await nextTick()
    chart && chart.resize()
})

onBeforeUnmount(() => {
    observer && observer.disconnect()
    observer = null
    chart && chart.dispose()
    chart = null
})
</script>

<style scoped>
.chart-box {
    position: relative;
    width: 100%;
}

.chart-canvas {
    width: 100%;
    height: 100%;
}

.chart-empty,
.chart-mask {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--tx-muted);
    font-size: 13px;
    background: transparent;
}

.chart-mask {
    background: rgba(15, 20, 32, .55);
}
</style>
