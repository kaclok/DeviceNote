<template>
    <div ref="shellRef" class="map-shell" :class="{immersive}">
        <svg ref="svgRef" class="plant-svg" :class="{dragging, editable}" :viewBox="viewBox"
             :style="{aspectRatio: `${canvas[0]} / ${canvas[1]}`}"
             preserveAspectRatio="xMidYMid meet"
             @wheel.prevent="onWheel"
             @pointerdown="onPointerDown"
             @pointermove="onPointerMove"
             @pointerup="onPointerUp($event)"
             @pointercancel="onPointerUp($event)"
             @dblclick="onWaypointDblClickBox">
            <!-- 旋转组：绕底图中心旋转，浏览模式下可通过右上角按钮 90° 步进旋转 -->
            <g :transform="rotTransform">
            <image :href="imageUrl" x="0" y="0" :width="canvas[0]" :height="canvas[1]"
                   preserveAspectRatio="none" pointer-events="none"/>

            <!-- 寻路结果存在时压暗底图，避免卫星图色彩干扰路线辨识 -->
            <rect v-if="hasRoute && !editable" x="0" y="0" :width="canvas[0]" :height="canvas[1]"
                  fill="rgba(15,23,42,.35)" pointer-events="none" class="route-scrim"/>

            <g class="road-layer" :class="{'road-muted': hasRoute && !editable}">
                <polyline v-for="road in roads" :key="`case-${road.id}`" :points="roadPts.get(road.id)"
                          :data-road-line="road.id"
                          fill="none" :stroke="road.closed ? '#f3b4b4' : 'rgba(15,23,42,.38)'"
                          :stroke-width="casingWidth(road)" stroke-linecap="round" stroke-linejoin="round"
                          pointer-events="none"/>
                <polyline v-for="road in roads" :key="`road-${road.id}`" :points="roadPts.get(road.id)"
                          :data-road-line="road.id"
                          fill="none" :stroke="road.closed ? '#ef4444' : 'rgba(255,255,255,.9)'"
                          :stroke-width="surfaceWidth(road)" stroke-linecap="round" stroke-linejoin="round"
                          :stroke-dasharray="road.closed ? '7 5' : ''" pointer-events="none"/>
                <polyline v-if="roadMode && hoverRoadId" :points="roadPts.get(hoverRoadId)"
                          fill="none" stroke="#1677ff" stroke-width="16" stroke-linecap="round"
                          stroke-linejoin="round" opacity=".18" pointer-events="none"/>
                <template v-if="roadMode">
                    <polyline v-for="road in roads" :key="`hit-${road.id}`"
                              class="road-hit" :points="roadPts.get(road.id)"
                              fill="none" stroke="transparent" stroke-width="22" stroke-linecap="round"
                              @click.stop="onRoadClick(road)"
                              @mouseenter="hoverRoadId = road.id" @mouseleave="hoverRoadId = ''"/>
                </template>
                <polyline v-if="editable && selectedRoad" :points="roadPts.get(selectedRoad.id)"
                          :data-road-line="selectedRoad.id"
                          fill="none" stroke="#8b5cf6" stroke-width="16" stroke-linecap="round"
                          stroke-linejoin="round" opacity=".45" pointer-events="none"/>
                <polyline v-if="editable && selectedRoad" :points="roadPts.get(selectedRoad.id)"
                          :data-road-line="selectedRoad.id"
                          fill="none" stroke="#c4b5fd" stroke-width="2.5" stroke-linecap="round"
                          stroke-linejoin="round" stroke-dasharray="8 6" pointer-events="none"/>
                <template v-if="editable && !roadMode">
                    <polyline v-for="road in roads" :key="`edit-${road.id}`"
                              class="road-hit" :data-road-id="road.id" :data-road-line="road.id"
                              :points="roadPts.get(road.id)"
                              fill="none" stroke="transparent" stroke-width="14" stroke-linecap="round"
                              @mouseenter="hoverRoadId = road.id" @mouseleave="hoverRoadId = ''"/>
                </template>
            </g>

            <g class="route-layer" :class="{'route-mode': hasRoute && !editable}">
                <g v-for="route in sortedRoutes" :key="route.id" :class="{'route-alt': !route.selected}">
                    <template v-if="route.selected">
                        <!-- 深色底衬 + 白色描边 + 彩色主线，保证在任意底图上都清晰 -->
                        <polyline :points="routePts.get(route.id)" fill="none" stroke="#0b1220"
                                  stroke-width="18" stroke-linecap="round" stroke-linejoin="round"
                                  opacity=".5" pointer-events="none"/>
                        <polyline :points="routePts.get(route.id)" fill="none" stroke="#ffffff"
                                  stroke-width="12" stroke-linecap="round" stroke-linejoin="round"
                                  opacity=".96" pointer-events="none"/>
                        <polyline :points="routePts.get(route.id)" fill="none" :stroke="route.color"
                                  stroke-width="7" stroke-linecap="round" stroke-linejoin="round"
                                  class="route-line" @click.stop="emit('select-route', route.id)"/>
                        <circle v-if="route.points?.length" :cx="route.points[0][0]" :cy="route.points[0][1]"
                                r="7" fill="#ffffff" :stroke="route.color" stroke-width="4"
                                pointer-events="none"/>
                        <circle v-if="route.points?.length" :cx="route.points[route.points.length - 1][0]"
                                :cy="route.points[route.points.length - 1][1]"
                                r="8" :fill="route.color" stroke="#ffffff" stroke-width="3"
                                pointer-events="none"/>
                    </template>
                    <polyline v-else :points="routePts.get(route.id)" fill="none" stroke="#94a3b8"
                              stroke-width="3" stroke-dasharray="10 8" opacity=".45"
                              stroke-linecap="round" stroke-linejoin="round"
                              class="route-line" @click.stop="emit('select-route', route.id)"/>
                </g>
            </g>

            <!-- 预览行驶动画：点击路线的播放按钮后，光标沿路线走一次，不循环。
                 外层 g 不绑定 transform，位置由 rAF 直接 setAttribute，避免每帧触发整图重渲染 -->
            <g v-if="!editable && cursorPoints.length > 1"
               :key="`${previewRouteId}-${previewKey}`" class="route-cursor" pointer-events="none">
                <path :d="cursorPathD" fill="none" stroke="none"/>
                <g class="cursor-g">
                    <g :transform="`scale(${markerScale})`">
                        <circle r="13" fill="rgba(22, 119, 255, .22)"/>
                        <circle r="7" fill="#1677ff" stroke="#ffffff" stroke-width="3"/>
                        <circle r="7" fill="none" stroke="#1677ff" stroke-width="2" class="cursor-pulse"/>
                    </g>
                </g>
            </g>

            <g class="poi-layer" :class="{'poi-muted': hasRoute && !editable}">
                <g v-for="node in renderNodes" :key="node.id" class="poi"
                   :class="{'poi-bad': node.status && node.status !== 1, draggable: editable, 'poi-hidden': node.visible === false, 'poi-faded': hasRoute && !editable && !isPinned(node)}"
                   :transform="`translate(${node.x} ${node.y}) scale(${markerScale}) rotate(${-rotation})`"
                   @pointerdown.stop="onNodePointerDown(node, $event)"
                   @click.stop="onNodeClick(node)"
                   @mouseenter="hoverNodeId = node.id"
                   @mouseleave="hoverNodeId = ''">
                    <circle :r="node.type === 'junction' ? 18 : 28" fill="transparent" pointer-events="all"/>
                    <circle :r="node.type === 'junction' ? 9 : 16" :fill="typeColor(node.type)"
                            stroke="#ffffff" stroke-width="2.5"/>
                    <circle v-if="editable && node.visible === false" :r="node.type === 'junction' ? 12 : 20"
                            fill="none" stroke="#94a3b8" stroke-width="1.8" stroke-dasharray="4 3"
                            pointer-events="none"/>
                    <text class="poi-glyph" :class="{'glyph-small': node.type === 'junction'}" y="1"
                          text-anchor="middle" dominant-baseline="middle">{{ glyphOf(node.type) }}</text>
                    <circle v-if="node.status && node.status !== 1" r="19" fill="none"
                            stroke="#ef4444" stroke-width="2.5" stroke-dasharray="4 3" pointer-events="none"/>
                    <circle v-if="node.id === selectedNodeId" r="24" fill="none" stroke="#f59e0b"
                            stroke-width="3" stroke-dasharray="6 4" pointer-events="none"/>
                    <text v-if="showLabel(node)" class="poi-label" y="34" text-anchor="middle">{{ node.name }}</text>
                    <!-- 起终点标识：大号彩色徽标 + 呼吸光环，便于一眼定位 -->
                    <g v-if="node.id === startId" class="endpoint" pointer-events="none">
                        <circle r="30" class="ep-halo ep-start"/>
                        <circle r="25" fill="none" stroke="#00a870" stroke-width="4"/>
                        <g transform="translate(0 -48)">
                            <rect x="-32" y="-20" width="64" height="40" rx="20" fill="#00a870"
                                  stroke="#ffffff" stroke-width="3.5"/>
                            <text y="1" class="ep-text" text-anchor="middle" dominant-baseline="middle">起点</text>
                        </g>
                    </g>
                    <g v-if="node.id === endId" class="endpoint" pointer-events="none">
                        <circle r="30" class="ep-halo ep-end"/>
                        <circle r="25" fill="none" stroke="#ef232a" stroke-width="4"/>
                        <g transform="translate(0 -48)">
                            <rect x="-32" y="-20" width="64" height="40" rx="20" fill="#ef232a"
                                  stroke="#ffffff" stroke-width="3.5"/>
                            <text y="1" class="ep-text" text-anchor="middle" dominant-baseline="middle">终点</text>
                        </g>
                    </g>
                </g>
            </g>

            <g v-if="editable && selectedRoad && waypointList.length" class="waypoint-layer">
                <g v-for="wp in waypointList" :key="`wp-${wp.index}`" class="waypoint"
                   :data-wp-index="wp.index"
                   :transform="`translate(${wp.point[0]} ${wp.point[1]}) scale(${markerScale})`"
                   @pointerdown.stop="onWaypointPointerDown(wp, $event)"
                   @pointerup.stop="onWaypointPointerUp($event)">
                    <circle r="16" fill="transparent" pointer-events="all"/>
                    <circle r="8" fill="#ffffff" stroke="#7c3aed" stroke-width="3"/>
                </g>
            </g>
            </g><!-- /旋转组 -->

            <!-- 拖拽点位的坐标提示：静态元素 + DOM 直操作，避免拖动期间每帧重渲染 -->
            <g ref="nodeTipRef" class="node-tip" style="display:none" pointer-events="none">
                <rect x="-36" y="-44" width="72" height="22" rx="6" fill="rgba(15,23,42,.92)"/>
                <text ref="nodeTipTextRef" y="-28" text-anchor="middle"></text>
            </g>
        </svg>

        <div class="map-controls" @pointerdown.stop>
            <button class="map-btn zoom" title="放大" @click="zoomBy(0.8)">+</button>
            <button class="map-btn zoom" title="缩小" @click="zoomBy(1.25)">−</button>
            <button class="map-btn reset" title="复位" @click="resetView">复位</button>
        </div>

        <!-- 右上角：全屏开关，全屏后同一按钮变为退出（地图旋转已下线，页面改用横屏布局） -->
        <div class="map-fs" @pointerdown.stop>
            <button class="map-btn" :title="fullscreen ? '退出全屏' : '全屏查看'" @click="toggleFullscreen">
                <svg v-if="!fullscreen" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor"
                     stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M9 4v5H4M15 4v5h5M9 20v-5H4M15 20v-5h5"/>
                </svg>
            </button>
        </div>

        <div class="compass">
            <div class="compass-arrow" :style="{transform: `rotate(${-rotation}deg)`}"></div>
            <span>北</span>
        </div>

        <div class="scale-bar">
            <div class="scale-line" :style="{width: `${scaleBarPx}px`}"></div>
            <span>100 米</span>
        </div>

        <div v-if="roadMode" class="road-mode-tip">道路管控中：点击道路可标记禁行或恢复通行</div>
        <div v-if="editable" class="edit-tip">配置模式：拖动点位调整坐标 · 选中道路可增删/拖动控制点 · 方向键微调</div>
        <div v-if="editable && selectedRoad" class="road-edit-tip" @pointerdown.stop>
            <span class="ret-name">{{ selectedRoad.name }}</span>
            <span class="ret-sub">{{ waypointList.length }} 个控制点 · 点线段插入 · 拖紫点调整 · 双击删除</span>
            <button class="ret-close" title="取消选中道路" @click="emit('select-road', '')">×</button>
        </div>

        <div v-if="picked && !editable" class="pick-card" @pointerdown.stop>
            <button class="pick-close" title="关闭" @click="picked = null">×</button>
            <div class="pick-title">{{ picked.name }}</div>
            <div class="pick-sub">
                {{ typeLabel(picked.type) }}
                <span v-if="picked.status" :class="picked.status === 1 ? 'st-ok' : 'st-bad'">
                    · {{ statusText(picked.status) }}
                </span>
            </div>
            <div class="pick-actions">
                <button class="pick-btn start" @click="choosePoint('start')">设为起点</button>
                <button class="pick-btn end" @click="choosePoint('end')">设为终点</button>
            </div>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch} from 'vue'

const props = defineProps({
    nodes: {type: Array, default: () => []},
    roads: {type: Array, default: () => []},
    routes: {type: Array, default: () => []},
    startId: {type: String, default: ''},
    endId: {type: String, default: ''},
    /** 需要自动缩放到视野内的坐标集合（如寻路结果） */
    focusPoints: {type: Array, default: () => []},
    /** 需要播放行驶动画的路线 id（空表示不播放） */
    previewRouteId: {type: String, default: ''},
    /** 每次播放递增，用于同一条路线重复播放时重启动画 */
    previewKey: {type: Number, default: 0},
    roadMode: {type: Boolean, default: false},
    editable: {type: Boolean, default: false},
    selectedNodeId: {type: String, default: ''},
    selectedRoadId: {type: String, default: ''},
    imageUrl: {type: String, default: '/plantmap/plant.png'},
    imageWidth: {type: Number, default: 1280},
    imageHeight: {type: Number, default: 558},
    metersPerPixel: {type: Number, default: 1},
})

const emit = defineEmits([
    'set-start', 'set-end', 'toggle-road', 'select-route', 'select-node', 'select-road',
    'move-node', 'pick-point', 'move-waypoint', 'insert-waypoint', 'remove-waypoint',
    'preview-end',
])

const TYPE_META = {
    gate: {label: '大门', color: '#1c4785', glyph: '门'},
    junction: {label: '路口', color: '#94a3b8', glyph: ''},
    scale: {label: '磅房', color: '#1677ff', glyph: '磅'},
    parking: {label: '停车场', color: '#8b5cf6', glyph: 'P'},
    pickup: {label: '拉货点', color: '#00a870', glyph: '取'},
    delivery: {label: '送货点', color: '#ff8f1f', glyph: '送'},
}
const STATUS_TEXT = {1: '正常', 2: '检修', 3: '停用'}

const shellRef = ref(null)
const svgRef = ref(null)
const svgWidth = ref(720)
const canvas = computed(() => [props.imageWidth, props.imageHeight])
const vb = reactive({x: 0, y: 0, w: props.imageWidth, h: props.imageHeight})
const picked = ref(null)
const dragging = ref(false)
const hoverRoadId = ref('')
let dragState = null
let nodeDrag = null
let waypointDrag = null
// 手机端双指缩放：记录当前按下的触点，两指时进入缩放态
const activePointers = new Map()
let pinchState = null
const nodeTipRef = ref(null)
const nodeTipTextRef = ref(null)

/** 拖动点位的坐标提示：直接写 DOM，不经过响应式（拖动期间每帧调用） */
function showNodeTip(x, y) {
    const tip = nodeTipRef.value
    if (!tip) return
    tip.style.display = ''
    tip.setAttribute('transform', `translate(${x} ${y}) scale(${markerScale.value})`)
    if (nodeTipTextRef.value) nodeTipTextRef.value.textContent = `${x}, ${y}`
}

function hideNodeTip() {
    if (nodeTipRef.value) nodeTipRef.value.style.display = 'none'
}
// pointerup 时 dragState 已清空，click 在其后触发，用独立标志阻止「拖拽后误选点」
let suppressClick = false
let ro = null

const viewBox = computed(() => `${vb.x} ${vb.y} ${vb.w} ${vb.h}`)
const sortedRoutes = computed(() => [...props.routes].sort((a, b) => Number(a.selected) - Number(b.selected)))
/** 正在预览的路线：仅点击播放按钮后才存在，用于单次行驶动画 */
const previewRoute = computed(() => props.routes.find((route) => route.id === props.previewRouteId) || null)
const cursorPoints = computed(() => previewRoute.value?.points || [])
const cursorPathD = computed(() => {
    const points = cursorPoints.value
    if (!points.length) return ''
    return points.map((point, index) => `${index ? 'L' : 'M'}${point[0]} ${point[1]}`).join(' ')
})
/** 动画时长跟路线耗时相关，但限制在 8~20 秒，避免太长或一闪而过 */
const cursorDuration = computed(() => {
    const seconds = Number(previewRoute.value?.seconds) || 300
    return Math.max(8, Math.min(20, seconds / 20)).toFixed(1)
})

// 行驶光标位置由 requestAnimationFrame 直接写 DOM（不经 Vue 响应式），沿路线只走一遍，
// 避免每帧触发整个 SVG 的重渲染
let cursorRaf = 0

function stopCursor() {
    if (cursorRaf) cancelAnimationFrame(cursorRaf)
    cursorRaf = 0
}

function startCursor() {
    stopCursor()
    const root = shellRef.value?.querySelector('.route-cursor')
    const pathEl = root?.querySelector('path')
    const gEl = root?.querySelector('.cursor-g')
    if (!pathEl?.getTotalLength || !gEl) return
    const total = pathEl.getTotalLength()
    if (!total) return
    // 先把光标放到起点，避免第一帧闪现在别处
    const origin = pathEl.getPointAtLength(0)
    gEl.setAttribute('transform', `translate(${origin.x} ${origin.y})`)
    const duration = Number(cursorDuration.value) * 1000
    const start = performance.now()
    const step = (now) => {
        const ratio = Math.min(1, (now - start) / duration)
        const point = pathEl.getPointAtLength(total * ratio)
        // 取前方 1px 处的点计算朝向，让光标沿路线转向；再补偿地图自身旋转角
        const ahead = pathEl.getPointAtLength(Math.min(total, total * ratio + 1))
        const angle = Math.atan2(ahead.y - point.y, ahead.x - point.x) * 180 / Math.PI - rotation.value
        gEl.setAttribute('transform', `translate(${point.x} ${point.y}) rotate(${angle})`)
        if (ratio < 1) {
            cursorRaf = requestAnimationFrame(step)
        } else {
            cursorRaf = 0
            emit('preview-end')
        }
    }
    cursorRaf = requestAnimationFrame(step)
}

watch(() => [props.previewRouteId, props.previewKey], () => {
    stopCursor()
    if (!props.previewRouteId || cursorPoints.value.length < 2) return
    nextTick(startCursor)
})
const hoverNodeId = ref('')

/** 折线 points 字符串缓存：roads/routes 不变时复用，避免每次渲染重复拼接 130+ 条字符串 */
const roadPts = computed(() => {
    const map = new Map()
    for (const road of props.roads) map.set(road.id, toPoints(road.points))
    return map
})
const routePts = computed(() => {
    const map = new Map()
    for (const route of props.routes) map.set(route.id, toPoints(route.points))
    return map
})
const selectedRoad = computed(() => props.roads.find((road) => road.id === props.selectedRoadId))
/** 可拖拽的道路控制点（去掉首尾节点，首尾由点位自身维护） */
const waypointList = computed(() => {
    const points = selectedRoad.value?.points || []
    const list = []
    for (let i = 1; i < points.length - 1; i++) list.push({index: i, point: points[i]})
    return list
})

/** 编辑模式下展示全部点位（隐藏点用虚线标记），浏览/寻路模式只展示 visible 点位 */
const renderNodes = computed(() => (props.editable
    ? props.nodes
    : props.nodes.filter((node) => node.visible !== false)))

/**
 * 点位、控制点、标注不随缩放放大：反向缩放，使其在屏幕上保持固定大小。
 * 以屏幕像素为准（而不仅是相对底图比例），手机窄屏下点位也不会缩成一两个像素。
 */
const MARKER_RADIUS_PX = 11
const markerScale = computed(() => {
    const px = svgWidth.value || props.imageWidth || 1
    return Math.max(0.05, (MARKER_RADIUS_PX * vb.w) / (16 * px))
})

/** 已生成寻路结果：进入「路线优先」视觉模式，弱化底图与无关点位 */
const hasRoute = computed(() => props.routes.length > 0)

const LABEL_ORDER = {gate: 0, scale: 1, parking: 2, pickup: 3, delivery: 4, junction: 5}

function isPinned(node) {
    return node.id === props.selectedNodeId || node.id === props.startId || node.id === props.endId
}

function rankOf(node) {
    return isPinned(node) ? -1 : (LABEL_ORDER[node.type] ?? 9)
}

/** 按标注优先级排序的点位：只在点位列表/起终点变化时重排，pan/zoom 时复用顺序 */
const rankedNodes = computed(() => [...renderNodes.value].sort((a, b) => rankOf(a) - rankOf(b)))

/**
 * 文字标签防重叠：按类型优先级依次放置，与已放标签相撞的隐藏。
 * 缩放/平移都会重新计算，放大后标签自然变多。
 */
const hiddenLabelIds = computed(() => {
    const svgWidthPx = svgWidth.value || props.imageWidth || 1
    const pxPerUnit = svgWidthPx / (vb.w || 1)
    // 标注在屏幕上大小恒定：markerScale * pxPerUnit 即「每单位对应的屏幕像素」
    const screenUnit = markerScale.value * pxPerUnit
    const placed = []
    const hidden = new Set()

    for (const node of rankedNodes.value) {
        if (!node.name) continue
        const sx = (Number(node.x) - vb.x) * pxPerUnit
        const sy = (Number(node.y) - vb.y) * pxPerUnit
        if (sx < -100 || sy < -100 || sx > svgWidthPx + 100) continue
        const half = (node.name.length * 15 * screenUnit) / 2
        const box = {l: sx - half, r: sx + half, t: sy + 26 * screenUnit, b: sy + 42 * screenUnit}
        if (placed.some((p) => box.l < p.r && box.r > p.l && box.t < p.b && box.b > p.t)) hidden.add(node.id)
        else placed.push(box)
    }
    return hidden
})

function showLabel(node) {
    if (node.id === hoverNodeId.value) return true
    // 路线模式下只保留起终点等关键点位标签，其余标注隐藏以突出路线
    if (hasRoute.value && !props.editable && !isPinned(node)) return false
    return isPinned(node) || !hiddenLabelIds.value.has(node.id)
}

const scaleBarPx = computed(() => {
    const mpp = Number(props.metersPerPixel) || 1
    const units = 100 / mpp
    return Math.max(26, Math.min(180, (units / vb.w) * svgWidth.value))
})

// ===== 全屏查看 =====
const fullscreen = ref(false)
// 浏览器不支持元素全屏（如 iOS Safari）时，退化为页面内满屏
const immersive = ref(false)

function setFullscreen(on) {
    fullscreen.value = on
    if (!on) immersive.value = false
}

async function toggleFullscreen() {
    const el = shellRef.value
    if (!el) return
    if (fullscreen.value) {
        if (document.fullscreenElement) {
            try {
                await document.exitFullscreen()
            } catch (e) {
                // 已被浏览器退出，忽略
            }
        }
        setFullscreen(false)
        return
    }
    const request = el.requestFullscreen || el.webkitRequestFullscreen
    if (request) {
        try {
            await request.call(el)
            setFullscreen(true)
            return
        } catch (e) {
            // 不支持时走下面的页面内满屏
        }
    }
    immersive.value = true
    fullscreen.value = true
}

function onFullscreenChange() {
    const on = document.fullscreenElement === shellRef.value
    if (on) setFullscreen(true)
    else if (!immersive.value) setFullscreen(false)
}

function onFullscreenKeydown(event) {
    if (event.key === 'Escape' && immersive.value) setFullscreen(false)
}

onMounted(() => {
    ro = new ResizeObserver((entries) => {
        svgWidth.value = entries[0].contentRect.width
    })
    if (shellRef.value) ro.observe(shellRef.value)
    window.addEventListener('keydown', onKeyDown)
    window.addEventListener('keydown', onFullscreenKeydown)
    document.addEventListener('fullscreenchange', onFullscreenChange)
    resetView()
})

onBeforeUnmount(() => {
    ro?.disconnect()
    stopCursor()
    window.removeEventListener('keydown', onKeyDown)
    window.removeEventListener('keydown', onFullscreenKeydown)
    document.removeEventListener('fullscreenchange', onFullscreenChange)
    if (document.fullscreenElement === shellRef.value) document.exitFullscreen?.()
})

function typeMeta(type) {
    return TYPE_META[type] || {label: '地点', color: '#64748b', glyph: '点'}
}

function typeColor(type) {
    return typeMeta(type).color
}

function glyphOf(type) {
    return typeMeta(type).glyph
}

function typeLabel(type) {
    return typeMeta(type).label
}

function statusText(status) {
    return STATUS_TEXT[status] || status
}

function toPoints(points) {
    return (points || []).map((point) => point.join(',')).join(' ')
}

function casingWidth(road) {
    if (road.level === 'main') return 12
    if (road.level === 'secondary') return 8
    return 5
}

function surfaceWidth(road) {
    if (road.level === 'main') return 8
    if (road.level === 'secondary') return 5
    return 3
}

function syncViewBoxSize() {
    vb.w = props.imageWidth
    vb.h = props.imageHeight
}

function clampView() {
    const width = props.imageWidth
    const height = props.imageHeight
    const minW = 240
    const maxW = width * 1.15
    vb.w = Math.max(minW, Math.min(maxW, vb.w))
    vb.h = vb.w * (height / width)
    vb.x = Math.max(-120, Math.min(width + 120 - vb.w, vb.x))
    vb.y = Math.max(-120, Math.min(height + 120 - vb.h, vb.y))
}

function onWheel(event) {
    const rect = svgRef.value?.getBoundingClientRect()
    if (!rect) return
    const ratioX = (event.clientX - rect.left) / rect.width
    const ratioY = (event.clientY - rect.top) / rect.height
    const px = vb.x + ratioX * vb.w
    const py = vb.y + ratioY * vb.h
    const factor = event.deltaY > 0 ? 1.12 : 0.89
    const nextW = vb.w * factor
    const k = nextW / vb.w
    vb.x = px - (px - vb.x) * k
    vb.y = py - (py - vb.y) * k
    vb.w = nextW
    vb.h = nextW * (props.imageHeight / props.imageWidth)
    clampView()
}

function zoomBy(factor) {
    const cx = vb.x + vb.w / 2
    const cy = vb.y + vb.h / 2
    const nextW = vb.w * factor
    vb.w = nextW
    vb.h = nextW * (props.imageHeight / props.imageWidth)
    vb.x = cx - vb.w / 2
    vb.y = cy - vb.h / 2
    clampView()
}

function resetView() {
    syncViewBoxSize()
    vb.x = 0
    vb.y = 0
    rotation.value = 0
}

// ===== 地图旋转（已下线）=====
// 寻路页改用横屏布局后不再需要「竖屏地图转 90°」；rotation 恒为 0，
// 旋转组、罗盘与徽标反向自转的结构保留，日后要恢复只需加回一个切换按钮。
const rotation = ref(0)
const rotTransform = computed(() => (rotation.value
    ? `rotate(${rotation.value} ${props.imageWidth / 2} ${props.imageHeight / 2})`
    : null))

/** 将视野缩放到一组坐标（寻路结果），四周留出边距，手机竖屏下也能看清整条路线 */
function fitToPoints(points) {
    if (!Array.isArray(points) || !points.length) return
    let minX = Infinity
    let minY = Infinity
    let maxX = -Infinity
    let maxY = -Infinity
    for (const point of points) {
        const x = Number(point?.[0])
        const y = Number(point?.[1])
        if (!Number.isFinite(x) || !Number.isFinite(y)) continue
        minX = Math.min(minX, x)
        maxX = Math.max(maxX, x)
        minY = Math.min(minY, y)
        maxY = Math.max(maxY, y)
    }
    if (!Number.isFinite(minX)) return

    const ratio = props.imageWidth / props.imageHeight
    // 起终点徽标在屏幕上尺寸固定，留足边距避免贴边被裁掉
    const pad = Math.max(70, (maxX - minX) * 0.15, (maxY - minY) * 0.15)
    let width = Math.max(maxX - minX + pad * 2, 200)
    let height = maxY - minY + pad * 2
    width = Math.max(width, height * ratio)
    height = width / ratio

    // 允许略微缩小到整图之外，给起终点徽标留出边距
    vb.w = Math.min(props.imageWidth * 1.15, width)
    vb.h = vb.w / ratio
    vb.x = (minX + maxX) / 2 - vb.w / 2
    vb.y = (minY + maxY) / 2 - vb.h / 2
    clampView()
}

// 浅监听即可：focusPoints 由父组件 computed 返回新数组引用，无需 deep 遍历整棵坐标树
watch(() => props.focusPoints, (points) => {
    if (props.editable) return
    rotation.value = 0 // 包围盒按未旋转坐标系计算，寻路前先回正
    fitToPoints(points)
})

// 配置模式下的拖拽坐标换算不支持旋转，进入编辑前强制回正
watch(() => props.editable, (on) => {
    if (on) rotation.value = 0
})

// SVGPoint 复用，避免拖动/点击时频繁创建
let svgPoint = null

/** 当前屏幕 → 底图坐标的逆矩阵；拖动期间视野不动时可缓存复用 */
function svgInvMatrix() {
    const matrix = svgRef.value?.getScreenCTM?.()
    return matrix ? matrix.inverse() : null
}

function clientToSvg(event, invMatrix) {
    const svg = svgRef.value
    if (!svg?.createSVGPoint) return null
    const matrix = invMatrix || svgInvMatrix()
    if (!matrix) return null
    if (!svgPoint) svgPoint = svg.createSVGPoint()
    svgPoint.x = event.clientX
    svgPoint.y = event.clientY
    const target = svgPoint.matrixTransform(matrix)
    return {x: target.x, y: target.y}
}

function onPointerDown(event) {
    if (event.button !== 0 || props.roadMode) return
    activePointers.set(event.pointerId, {x: event.clientX, y: event.clientY})
    if (activePointers.size === 2) {
        startPinch()
        return
    }
    // rect 在拖动期间不变（元素尺寸不随 viewBox 变），按下时取一次，避免 move 中每帧强制布局
    dragState = {
        sx: event.clientX, sy: event.clientY, x: vb.x, y: vb.y, moved: false,
        rect: svgRef.value?.getBoundingClientRect(),
    }
    dragging.value = true
    capturePointer(event)
}

/** 双指按下：以两指中点为锚点缩放地图 */
function startPinch() {
    const it = activePointers.values()
    const p1 = it.next().value
    const p2 = it.next().value
    if (!p1 || !p2) return
    const rect = svgRef.value?.getBoundingClientRect()
    if (!rect) return
    const dist = Math.hypot(p1.x - p2.x, p1.y - p2.y) || 1
    pinchState = {
        dist,
        rect,
        center: {x: (p1.x + p2.x) / 2, y: (p1.y + p2.y) / 2},
        vb: {x: vb.x, y: vb.y, w: vb.w},
    }
    dragState = null
    nodeDrag = null
    waypointDrag = null
    dragging.value = true
}

/** 指针捕获：拖拽移出画布后仍能继续；合成事件下可能抛错，忽略即可 */
function capturePointer(event) {
    try {
        svgRef.value?.setPointerCapture?.(event.pointerId)
    } catch (e) {
        // ignore
    }
}

function onNodePointerDown(node, event) {
    if (!props.editable || event.button !== 0 || props.roadMode) return
    // 拖动点位时视野不动，CTM 逆矩阵在按下时缓存一次即可
    const invMatrix = svgInvMatrix()
    if (!invMatrix) return
    const start = clientToSvg(event, invMatrix)
    if (!start) return
    nodeDrag = {
        id: node.id, startX: start.x, startY: start.y, x: node.x, y: node.y,
        lastX: node.x, lastY: node.y, moved: false, invMatrix, el: event.currentTarget,
    }
    dragging.value = true
    capturePointer(event)
}

function onWaypointPointerDown(wp, event) {
    if (!props.editable || event.button !== 0) return
    const invMatrix = svgInvMatrix()
    if (!invMatrix || !selectedRoad.value) return
    const start = clientToSvg(event, invMatrix)
    if (!start) return
    waypointDrag = {
        roadId: selectedRoad.value.id,
        index: wp.index,
        startX: start.x,
        startY: start.y,
        x: wp.point[0],
        y: wp.point[1],
        lastX: wp.point[0],
        lastY: wp.point[1],
        moved: false,
        invMatrix,
        el: event.currentTarget,
        // 该道路的所有折线层（底衬/主线/高亮/命中层），拖动中直接重绘
        lines: svgRef.value?.querySelectorAll(`[data-road-line="${selectedRoad.value.id}"]`),
    }
    dragging.value = true
    capturePointer(event)
}

/** 结束控制点拖动：拖动中只改了 DOM，松手时才一次性同步到响应式数据 */
function endWaypointDrag() {
    if (!waypointDrag) return
    if (waypointDrag.moved) {
        emit('move-waypoint', {
            roadId: waypointDrag.roadId,
            index: waypointDrag.index,
            x: waypointDrag.lastX,
            y: waypointDrag.lastY,
        })
        suppressClick = true
    }
    waypointDrag = null
    dragging.value = false
}

function onWaypointPointerUp() {
    endWaypointDrag()
}

/** 指针捕获后 dblclick 落在 svg 上，用坐标命中控制点 */
function onWaypointDblClickBox(event) {
    if (!props.editable || !selectedRoad.value) return
    const el = document.elementFromPoint(event.clientX, event.clientY)
    const hit = el?.closest?.('[data-wp-index]')
    if (!hit) return
    emit('remove-waypoint', {
        roadId: selectedRoad.value.id,
        index: Number(hit.getAttribute('data-wp-index')),
    })
}

function onPointerMove(event) {
    // 复用已有点位对象，避免每帧分配新对象
    const tracked = activePointers.get(event.pointerId)
    if (tracked) {
        tracked.x = event.clientX
        tracked.y = event.clientY
    }
    if (pinchState && activePointers.size >= 2) {
        const it = activePointers.values()
        const p1 = it.next().value
        const p2 = it.next().value
        if (!p1 || !p2) return
        const dist = Math.hypot(p1.x - p2.x, p1.y - p2.y) || 1
        const {rect, vb: startVb, center} = pinchState
        const cx = (p1.x + p2.x) / 2
        const cy = (p1.y + p2.y) / 2
        // 两指初始按下的中心点对应的底图坐标保持不变
        const ax = startVb.x + ((center.x - rect.left) / rect.width) * startVb.w
        const ay = startVb.y + ((center.y - rect.top) / rect.height) * (startVb.w * (props.imageHeight / props.imageWidth))
        const nextW = Math.max(240, Math.min(props.imageWidth * 1.15, startVb.w * (pinchState.dist / dist)))
        vb.w = nextW
        vb.h = nextW * (props.imageHeight / props.imageWidth)
        vb.x = ax - ((cx - rect.left) / rect.width) * vb.w
        vb.y = ay - ((cy - rect.top) / rect.height) * vb.h
        clampView()
        return
    }
    if (waypointDrag) {
        const current = clientToSvg(event, waypointDrag.invMatrix)
        if (!current) return
        const dx = current.x - waypointDrag.startX
        const dy = current.y - waypointDrag.startY
        if (Math.abs(dx) + Math.abs(dy) > 1.5) waypointDrag.moved = true
        const nextX = Math.round((waypointDrag.x + dx) * 10) / 10
        const nextY = Math.round((waypointDrag.y + dy) * 10) / 10
        waypointDrag.lastX = nextX
        waypointDrag.lastY = nextY
        // 拖动期间直接改 DOM：控制点圆 + 该道路所有折线层，不触发 Vue 重渲染
        waypointDrag.el?.setAttribute('transform', `translate(${nextX} ${nextY}) scale(${markerScale.value})`)
        const road = selectedRoad.value
        if (road && waypointDrag.lines) {
            const points = road.points.map((p, i) => (i === waypointDrag.index ? `${nextX},${nextY}` : `${p[0]},${p[1]}`)).join(' ')
            for (const line of waypointDrag.lines) line.setAttribute('points', points)
        }
        return
    }
    if (nodeDrag) {
        const current = clientToSvg(event, nodeDrag.invMatrix)
        if (!current) return
        const dx = current.x - nodeDrag.startX
        const dy = current.y - nodeDrag.startY
        if (Math.abs(dx) + Math.abs(dy) > 1.5) nodeDrag.moved = true
        const nextX = Math.round((nodeDrag.x + dx) * 10) / 10
        const nextY = Math.round((nodeDrag.y + dy) * 10) / 10
        nodeDrag.lastX = nextX
        nodeDrag.lastY = nextY
        // 拖动期间直接改 DOM 并更新坐标提示，pointerup 时才同步响应式数据
        nodeDrag.el?.setAttribute('transform', `translate(${nextX} ${nextY}) scale(${markerScale.value})`)
        showNodeTip(nextX, nextY)
        return
    }
    if (!dragState) return
    const rect = dragState.rect
    if (!rect) return
    const dx = event.clientX - dragState.sx
    const dy = event.clientY - dragState.sy
    if (Math.abs(dx) + Math.abs(dy) > 4) dragState.moved = true
    vb.x = dragState.x - dx * (vb.w / rect.width)
    vb.y = dragState.y - dy * (vb.h / rect.height)
    clampView()
}

function onPointerUp(event) {
    activePointers.delete(event.pointerId)
    if (pinchState) {
        if (activePointers.size < 2) {
            pinchState = null
            dragging.value = false
            suppressClick = true
        }
        return
    }
    if (waypointDrag) {
        endWaypointDrag()
        return
    }
    if (nodeDrag) {
        if (!nodeDrag.moved) {
            emit('select-node', nodeDrag.id)
        } else {
            // 拖动期间只动了 DOM，松手时一次性把最终坐标同步给父组件
            emit('move-node', {id: nodeDrag.id, x: nodeDrag.lastX, y: nodeDrag.lastY})
            suppressClick = true
        }
        nodeDrag = null
        hideNodeTip()
        dragging.value = false
        return
    }
    if (dragState && !dragState.moved) {
        // 指针捕获会把 click 重定向到 svg，这里用坐标命中判断是否点到了道路
        const road = roadAtPoint(event)
        if (road) handleRoadPoint(road, event)
        else if (props.editable) {
            const point = clientToSvg(event)
            if (point) emit('pick-point', {
                x: Math.round(point.x * 10) / 10,
                y: Math.round(point.y * 10) / 10,
            })
        }
    }
    dragging.value = false
    suppressClick = !!dragState?.moved
    dragState = null
}

function onNodeClick(node) {
    if (suppressClick) {
        suppressClick = false
        return
    }
    if (props.roadMode) return
    if (props.editable) {
        emit('select-node', node.id)
        return
    }
    picked.value = node
}

function choosePoint(which) {
    if (!picked.value) return
    emit(which === 'start' ? 'set-start' : 'set-end', picked.value.id)
    picked.value = null
}

function onRoadClick(road) {
    if (!props.roadMode) return
    emit('toggle-road', road.id)
}

/** 编辑模式：未选中时点击道路=选中；已选中再点线段=在最近线段插入控制点 */
function handleRoadPoint(road, event) {
    if (!props.editable || props.roadMode) return
    if (props.selectedRoadId !== road.id) {
        emit('select-road', road.id)
        return
    }
    const point = clientToSvg(event)
    if (!point || !Array.isArray(road.points) || road.points.length < 2) return
    const target = nearestPointOnPolyline(road.points, point)
    emit('insert-waypoint', {
        roadId: road.id,
        index: target.index,
        x: Math.round(target.x * 10) / 10,
        y: Math.round(target.y * 10) / 10,
    })
}

/** 指针捕获下 click 会被重定向到 svg，用坐标命中道路命中层 */
function roadAtPoint(event) {
    const el = document.elementFromPoint(event.clientX, event.clientY)
    const hit = el?.closest?.('[data-road-id]')
    if (!hit) return null
    return props.roads.find((road) => road.id === hit.getAttribute('data-road-id')) || null
}

/** 找到折线上离指定点最近的线段，返回插入下标与投影坐标 */
function nearestPointOnPolyline(points, target) {
    let best = {index: 1, x: target.x, y: target.y, distance: Infinity}
    for (let i = 0; i < points.length - 1; i++) {
        const [x1, y1] = points[i]
        const [x2, y2] = points[i + 1]
        const dx = x2 - x1
        const dy = y2 - y1
        const lengthSq = dx * dx + dy * dy
        const ratio = lengthSq ? Math.max(0, Math.min(1,
            ((target.x - x1) * dx + (target.y - y1) * dy) / lengthSq)) : 0
        const px = x1 + ratio * dx
        const py = y1 + ratio * dy
        const distance = Math.hypot(target.x - px, target.y - py)
        if (distance < best.distance) best = {index: i + 1, x: px, y: py, distance}
    }
    return best
}

/** 方向键微调选中点位：1px；Shift + 方向键 5px */
function onKeyDown(event) {
    if (!props.editable || !props.selectedNodeId) return
    const tag = event.target?.tagName
    if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || event.target?.isContentEditable) return
    const step = event.shiftKey ? 5 : 1
    const delta = {
        ArrowLeft: [-step, 0], ArrowRight: [step, 0],
        ArrowUp: [0, -step], ArrowDown: [0, step],
    }[event.key]
    if (!delta) return
    const node = props.nodes.find((item) => item.id === props.selectedNodeId)
    if (!node) return
    event.preventDefault()
    emit('move-node', {
        id: node.id,
        x: Math.round((Number(node.x) + delta[0]) * 10) / 10,
        y: Math.round((Number(node.y) + delta[1]) * 10) / 10,
    })
}
</script>

<style scoped>
.map-shell {
    position: relative;
    overflow: hidden;
    background: #0f172a;
    border-radius: 0 0 10px 10px;
    user-select: none;
}

.plant-svg {
    display: block;
    width: 100%;
    cursor: grab;
    touch-action: none;
}

.plant-svg.editable {
    cursor: crosshair;
}

.plant-svg.dragging {
    cursor: grabbing;
}

.poi {
    cursor: pointer;
}

.poi.draggable {
    cursor: grab;
}

.poi.draggable:active {
    cursor: grabbing;
}

.poi-hidden {
    opacity: .45;
}

/* 路线优先模式：无关点位淡出，鼠标悬停时恢复，减少视觉干扰 */
.poi-faded {
    opacity: .28;
    transition: opacity .18s ease;
}

.poi-faded:hover {
    opacity: 1;
}

.poi-muted .poi-badge {
    opacity: .55;
}

.road-muted {
    /* 有路线时压低路网但保持可见：过低的透明度会让点位看起来悬空 */
    opacity: .5;
    transition: opacity .18s ease;
}

.waypoint {
    cursor: grab;
}

.waypoint:active {
    cursor: grabbing;
}

.node-tip text {
    font-size: 12px;
    font-weight: 600;
    fill: #f8fafc;
}

.poi-glyph {
    font-size: 15px;
    font-weight: 700;
    fill: #ffffff;
    pointer-events: none;
}

.poi-glyph.glyph-small {
    font-size: 10px;
}

.poi-label {
    font-size: 15px;
    font-weight: 600;
    fill: #ffffff;
    paint-order: stroke;
    stroke: rgba(15, 23, 42, .75);
    stroke-width: 4px;
    stroke-linejoin: round;
    pointer-events: none;
}

/* 起终点：呼吸光环 + 白描边彩色徽标 */
.ep-halo {
    fill: none;
    stroke-width: 4;
    opacity: 0;
    animation: ep-pulse 2s ease-out infinite;
}

.ep-halo.ep-start {
    stroke: #00a870;
}

.ep-halo.ep-end {
    stroke: #ef232a;
}

.cursor-pulse {
    transform-box: fill-box;
    transform-origin: center;
    animation: cursor-pulse 1.6s ease-out infinite;
}

@keyframes cursor-pulse {
    0% {
        transform: scale(1);
        opacity: .9;
    }
    100% {
        transform: scale(2.4);
        opacity: 0;
    }
}

.ep-text {
    font-size: 18px;
    font-weight: 700;
    fill: #ffffff;
    letter-spacing: 1px;
}

@keyframes ep-pulse {
    0% {
        r: 24px;
        opacity: .85;
    }
    70% {
        r: 44px;
        opacity: 0;
    }
    100% {
        r: 44px;
        opacity: 0;
    }
}

.poi-bad .poi-label {
    fill: #fecaca;
}

.route-line {
    cursor: pointer;
    pointer-events: stroke;
}

.road-hit {
    cursor: crosshair;
    pointer-events: stroke;
}

.map-controls {
    position: absolute;
    /* 让出顶部全屏按钮的位置，两组按钮上下排成一列 */
    top: 56px;
    right: 14px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    z-index: 3;
}

.map-btn {
    width: 34px;
    height: 34px;
    border: 1px solid #d5dfec;
    border-radius: 8px;
    background: rgba(255, 255, 255, .96);
    color: #334155;
    font-size: 17px;
    line-height: 1;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(31, 58, 95, .25);
}

.map-btn:hover {
    border-color: #1677ff;
    color: #1677ff;
}

.map-btn.reset {
    font-size: 11px;
    color: #64748b;
}

.map-btn svg {
    display: block;
    width: 18px;
    height: 18px;
    margin: 0 auto;
    pointer-events: none;
}

/* 全屏/旋转按钮固定在右上角；缩放按钮在手机上仍在右下，避免遮挡终点徽标 */
.map-fs {
    position: absolute;
    top: 14px;
    right: 14px;
    display: flex;
    gap: 6px;
    z-index: 4;
}

/* 原生全屏或页面内满屏：地图铺满整屏 */
.map-shell:fullscreen,
.map-shell:-webkit-full-screen,
.map-shell.immersive {
    width: 100vw;
    height: 100vh;
    aspect-ratio: auto !important;
    border-radius: 0;
    background: #0f172a;
}

.map-shell.immersive {
    position: fixed;
    inset: 0;
    z-index: 9999;
}

/* 全屏时让 SVG 撑满整屏，由 preserveAspectRatio 居中留边 */
.map-shell:fullscreen .plant-svg,
.map-shell:-webkit-full-screen .plant-svg,
.map-shell.immersive .plant-svg {
    width: 100%;
    height: 100%;
    aspect-ratio: auto !important;
}

/* 手机上缩放按钮移到右下，避免遮挡右上角的终点徽标；按钮整体收紧少占版面 */
@media (max-width: 760px) {
    .map-controls {
        top: auto;
        bottom: 40px;
        right: 8px;
        flex-direction: row;
        gap: 4px;
    }

    .map-btn {
        width: 30px;
        height: 30px;
        border-radius: 7px;
        font-size: 15px;
        box-shadow: 0 2px 8px rgba(31, 58, 95, .22);
    }

    .map-btn.reset {
        font-size: 10px;
        padding: 0 6px;
        width: auto;
        min-width: 30px;
    }

    .map-btn svg {
        width: 15px;
        height: 15px;
    }

    .map-fs {
        top: 8px;
        right: 8px;
        display: flex;
        gap: 4px;
    }

    .compass {
        top: 10px;
        left: 10px;
        transform: scale(.8);
        transform-origin: top left;
    }
}

.compass {
    position: absolute;
    top: 16px;
    left: 16px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 2px;
    color: #e2e8f0;
    font-size: 11px;
    z-index: 2;
    pointer-events: none;
    text-shadow: 0 1px 3px rgba(0, 0, 0, .7);
}

.compass-arrow {
    width: 0;
    height: 0;
    border-left: 6px solid transparent;
    border-right: 6px solid transparent;
    border-bottom: 20px solid #93c5fd;
    transition: transform .3s;
}

.scale-bar {
    position: absolute;
    left: 16px;
    bottom: 14px;
    color: #e2e8f0;
    font-size: 11px;
    z-index: 2;
    pointer-events: none;
    text-shadow: 0 1px 3px rgba(0, 0, 0, .7);
}

.scale-line {
    height: 6px;
    border: 1px solid #e2e8f0;
    border-top: none;
    margin-bottom: 2px;
    transition: width .12s ease;
}

.road-mode-tip,
.edit-tip {
    position: absolute;
    top: 14px;
    left: 50%;
    transform: translateX(-50%);
    padding: 6px 14px;
    border-radius: 999px;
    font-size: 12px;
    z-index: 3;
    pointer-events: none;
}

.road-mode-tip {
    background: rgba(239, 68, 68, .16);
    border: 1px solid rgba(239, 68, 68, .45);
    color: #fecaca;
}

.edit-tip {
    background: rgba(22, 119, 255, .18);
    border: 1px solid rgba(96, 165, 250, .5);
    color: #dbeafe;
}

.road-edit-tip {
    position: absolute;
    top: 48px;
    left: 50%;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 5px 8px 5px 12px;
    border-radius: 999px;
    background: rgba(76, 29, 149, .82);
    border: 1px solid rgba(196, 181, 253, .6);
    color: #ede9fe;
    font-size: 12px;
    white-space: nowrap;
    z-index: 3;
}

.ret-name {
    font-weight: 700;
}

.ret-sub {
    color: #d8cef8;
}

.ret-close {
    width: 18px;
    height: 18px;
    border: none;
    border-radius: 50%;
    background: rgba(255, 255, 255, .18);
    color: #f5f3ff;
    font-size: 13px;
    line-height: 1;
    cursor: pointer;
}

.ret-close:hover {
    background: rgba(255, 255, 255, .32);
}

.pick-card {
    position: absolute;
    left: 16px;
    bottom: 46px;
    width: 220px;
    padding: 12px 14px 13px;
    border-radius: 10px;
    background: rgba(255, 255, 255, .98);
    border: 1px solid #dbe5f1;
    box-shadow: 0 12px 30px rgba(24, 52, 90, .16);
    z-index: 4;
}

.pick-close {
    position: absolute;
    top: 4px;
    right: 6px;
    border: none;
    background: none;
    color: #94a3b8;
    font-size: 16px;
    cursor: pointer;
}

.pick-title {
    font-size: 14px;
    font-weight: 700;
    color: #1e293b;
    padding-right: 16px;
}

.pick-sub {
    margin-top: 3px;
    font-size: 12px;
    color: #7b8ca6;
}

.st-ok {
    color: #00a870;
}

.st-bad {
    color: #ef232a;
    font-weight: 600;
}

.pick-actions {
    display: flex;
    gap: 8px;
    margin-top: 10px;
}

.pick-btn {
    flex: 1;
    padding: 6px 0;
    border-radius: 7px;
    border: 1px solid transparent;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
}

.pick-btn.start {
    color: #00875a;
    background: rgba(0, 168, 112, .1);
    border-color: rgba(0, 168, 112, .25);
}

.pick-btn.end {
    color: #c62828;
    background: rgba(239, 35, 42, .08);
    border-color: rgba(239, 35, 42, .22);
}
</style>
