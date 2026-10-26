<template>
    <div class="page">
        <section class="planner">
            <div class="mode-row">
                <span class="mode-label">寻路方式</span>
                <el-radio-group v-model="mode" size="small">
                    <el-radio-button label="material">按物资寻路</el-radio-button>
                    <el-radio-button label="free">自由寻路</el-radio-button>
                </el-radio-group>
                <span class="mode-tip">
                    {{ mode === 'material'
                        ? (materialFlow === 'out' ? '装卸点 → 该物资绑定磅房 → 出场大门' : '进厂大门 → 该物资绑定磅房 → 装卸点')
                        : '任意两点间寻路，不经过磅房' }}
                </span>
            </div>

            <div v-if="mode === 'free'" class="planner-fields">
                <div class="pair-box">
                    <div class="pair-rail">
                        <i class="rail-dot start"></i>
                        <i class="rail-line"></i>
                        <i class="rail-dot end"></i>
                    </div>
                    <div class="pair-body">
                        <div class="pair-row">
                            <span class="pair-label">起点</span>
                            <el-select v-model="freeStartId" filterable placeholder="选择起点" class="field-select">
                                <el-option-group v-for="group in poiGroupOptions" :key="group.type"
                                                 :label="`${group.label}（${group.items.length}）`">
                                    <el-option v-for="node in group.items" :key="node.id"
                                               :label="node.optionLabel" :value="node.id"
                                               :disabled="node.startDisabled">
                                        <span class="opt-body">
                                            <i class="opt-dot" :style="{background: group.color}"></i>
                                            <span class="opt-name">{{ node.optionLabel }}</span>
                                            <span class="opt-type">{{ node.typeLabel }}</span>
                                        </span>
                                    </el-option>
                                </el-option-group>
                            </el-select>
                        </div>
                        <div class="pair-row">
                            <span class="pair-label">终点</span>
                            <el-select v-model="freeEndId" filterable placeholder="选择终点" class="field-select">
                                <el-option-group v-for="group in poiGroupOptions" :key="group.type"
                                                 :label="`${group.label}（${group.items.length}）`">
                                    <el-option v-for="node in group.items" :key="node.id"
                                               :label="node.optionLabel" :value="node.id"
                                               :disabled="node.endDisabled">
                                        <span class="opt-body">
                                            <i class="opt-dot" :style="{background: group.color}"></i>
                                            <span class="opt-name">{{ node.optionLabel }}</span>
                                            <span class="opt-type">{{ node.typeLabel }}</span>
                                        </span>
                                    </el-option>
                                </el-option-group>
                            </el-select>
                        </div>
                    </div>
                    <button class="swap-btn" title="互换起点与终点" @click="swapPoints">⇄</button>
                </div>

                <el-button type="primary" size="large" class="search-btn" @click="startSearch">开始寻路</el-button>
            </div>

            <div v-else class="planner-fields">
                <div class="field">
                    <label>物资</label>
                    <div class="field-line">
                        <el-select v-model="materialId" filterable class="field-select">
                            <el-option-group label="拉货（出厂）">
                                <el-option v-for="item in outMaterials" :key="item.id" :label="item.name"
                                           :value="item.id"/>
                            </el-option-group>
                            <el-option-group label="送货（入厂）">
                                <el-option v-for="item in inMaterials" :key="item.id" :label="item.name"
                                           :value="item.id"/>
                            </el-option-group>
                        </el-select>
                        <button class="voice-btn" :class="{playing: speaking}" :disabled="!voiceReady"
                                :title="materialVoice ? '播放物资语音介绍' : '该物资尚未配置语音介绍'"
                                @click="playMaterialVoice">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                                 stroke-linecap="round" stroke-linejoin="round">
                                <path d="M4 9v6h4l5 4V5L8 9H4Z"/>
                                <path d="M16.5 8.5a5 5 0 0 1 0 7"/>
                                <path d="M19 6a8.5 8.5 0 0 1 0 12"/>
                            </svg>
                            <span>{{ speaking ? '播放中' : '语音' }}</span>
                        </button>
                    </div>
                </div>

                <div class="field">
                    <label>方向</label>
                    <el-select v-model="materialFlow" class="field-select flow-select">
                        <el-option label="进场（大门 → 磅房 → 装卸点）" value="in"/>
                        <el-option label="出场（装卸点 → 磅房 → 大门）" value="out"/>
                    </el-select>
                </div>

                <div class="pair-box">
                    <div class="pair-rail">
                        <i class="rail-dot start"></i>
                        <i class="rail-line"></i>
                        <i class="rail-dot end"></i>
                    </div>
                    <div class="pair-body">
                        <div class="pair-row">
                            <span class="pair-label">起点</span>
                            <el-select v-if="materialFlow === 'in'" v-model="materialStartId" filterable
                                       class="field-select"
                                       :placeholder="materialStartGates.length ? '选择进厂大门' : '该物资没有可进厂的大门'">
                                <el-option v-for="gate in materialStartGates" :key="gate.id"
                                           :label="nodeLabel(gate)" :value="gate.id">
                                    <span class="opt-body">
                                        <i class="opt-dot" style="background:#1c4785"></i>
                                        <span class="opt-name">{{ nodeLabel(gate) }}</span>
                                        <span class="opt-type">大门</span>
                                    </span>
                                </el-option>
                            </el-select>
                            <div v-else class="fixed-point">
                                {{ materialPoint ? materialPoint.name : '该物资尚未配置装卸点' }}
                            </div>
                        </div>
                        <div class="pair-row">
                            <span class="pair-label">终点</span>
                            <el-select v-if="materialFlow === 'out'" v-model="materialEndGateId" filterable
                                       class="field-select"
                                       :placeholder="materialEndGates.length ? '选择出场大门' : '该物资没有可出场的大门'">
                                <el-option v-for="gate in materialEndGates" :key="gate.id"
                                           :label="nodeLabel(gate)" :value="gate.id">
                                    <span class="opt-body">
                                        <i class="opt-dot" style="background:#1c4785"></i>
                                        <span class="opt-name">{{ nodeLabel(gate) }}</span>
                                        <span class="opt-type">大门</span>
                                    </span>
                                </el-option>
                            </el-select>
                            <div v-else class="fixed-point">
                                {{ materialPoint ? materialPoint.name : '该物资尚未配置装卸点' }}
                            </div>
                        </div>
                    </div>
                </div>

                <div class="field">
                    <label>途经磅房</label>
                    <el-select v-model="scaleChoice" class="field-select" placeholder="自动选择可用磅房">
                        <el-option v-if="usableScales.length > 1" label="自动（每座可用磅房各出一条方案）" value=""/>
                        <el-option v-for="scale in usableScales" :key="scale.id"
                                   :label="`${scale.name}${scale.area ? ' · ' + scale.area : ''}`"
                                   :value="scale.id"/>
                    </el-select>
                </div>

                <el-button type="primary" size="large" class="search-btn" @click="startSearch">开始寻路</el-button>
            </div>

            <div class="planner-hint">
                点击地图上的地点也可设置起终点；道路维修时可在右上角「道路管控」中点击道路封闭。
                磅房、物资绑定与点位坐标存于 device-note 的 nav schema，可在「地图配置」中调整。
            </div>
        </section>

        <div v-if="plantState.loadError" class="error-tip">
            后端配置不可用，当前展示的是内置默认配置；修改状态或保存会失败，请检查 device-note 服务（8092）。
        </div>
        <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>

        <div class="workspace">
            <section ref="mapCardRef" class="map-card">
                <div class="map-head">
                    <div class="map-head-main">
                        <div class="map-title">厂区平面图</div>
                        <div class="map-sub">
                            {{ roadMode ? '道路管控模式：点击道路可标记或恢复通行' : '卫星底图 · 点位与路网可配置 · 滚轮缩放 / 拖拽平移' }}
                        </div>
                    </div>
                    <div class="legend">
                        <button v-for="item in legend" :key="item.type" class="legend-item legend-toggle"
                                :class="{off: hiddenTypes.has(item.type)}" :title="`点击${hiddenTypes.has(item.type) ? '显示' : '隐藏'}${item.label}`"
                                @click="toggleType(item.type)">
                            <i :style="{background: item.color}"></i>{{ item.label }}
                        </button>
                        <span class="legend-item"><i class="legend-route"></i>路线</span>
                    </div>
                    <el-switch v-model="roadMode" size="small" active-text="道路管控"/>
                </div>

                <PlantMap
                    :nodes="visibleNodes"
                    :roads="roadsWithState"
                    :routes="mapRoutes"
                    :start-id="startId"
                    :end-id="endId"
                    :road-mode="roadMode"
                    :image-url="plantMeta.imageUrl"
                    :image-width="plantMeta.imageWidth"
                    :image-height="plantMeta.imageHeight"
                    :meters-per-pixel="plantMeta.metersPerPixel"
                    :focus-points="focusPoints"
                    :preview-route-id="previewId"
                    :preview-key="previewKey"
                    @set-start="onSetStart"
                    @set-end="onSetEnd"
                    @select-route="selectRoute"
                    @preview-end="previewId = ''"
                    @toggle-road="onToggleRoad"
                />
            </section>

            <aside class="side">
                <section class="side-card">
                    <div class="side-head">
                        <span class="side-title">寻路方案</span>
                        <span class="side-sub" v-if="routes.length">
                            {{ routes.length }} 条 · {{ routeTitle }}
                        </span>
                    </div>

                    <div v-if="autoSwitched" class="switch-notice">
                        绑定磅房「{{ materialScale?.name }}」当前{{ STATUS_TEXT[materialScaleStatus] }}，
                        本次按备用磅「{{ effectiveScale.name }}」计算。
                        <el-button link type="danger" @click="confirmSwitch">确认切至 {{ effectiveScale.name }}</el-button>
                    </div>

                    <div v-if="!routes.length" class="empty-tip">
                        选择起点和终点后点击「开始寻路」
                    </div>

                    <div v-for="route in routes" :key="route.id" class="route-card"
                         :class="{active: route.id === activeId}" @click="selectRoute(route.id)">
                        <div class="route-top">
                            <span class="route-badge" :style="{background: route.color}">{{ route.rank }}</span>
                            <span class="route-time">{{ fmtDuration(route.seconds) }}</span>
                            <span class="route-dist">{{ fmtDistance(route.distance) }}</span>
                            <span v-if="route.viaScaleName" class="route-via">经 {{ route.viaScaleName }}</span>
                            <div class="route-top-right">
                                <span v-if="route.shortest" class="route-tag">距离最短</span>
                                <button class="preview-btn" :class="{playing: previewId === route.id}"
                                        :title="previewId === route.id ? '正在预览行驶路线' : '预览行驶路线'"
                                        @click.stop="previewRoute(route)">
                                    <svg viewBox="0 0 12 14" aria-hidden="true">
                                        <path d="M2 1.6v10.8c0 .6.65.95 1.15.65l8.1-5.4a.76.76 0 0 0 0-1.3L3.15.95C2.65.65 2 1 2 1.6Z"/>
                                    </svg>
                                    <span>预览</span>
                                </button>
                            </div>
                        </div>
                        <div class="route-roads">{{ route.roadSummary }}</div>
                        <div class="route-meta">
                            <span>途经大门：{{ route.gateText || '无（全程厂内道路）' }}</span>
                            <button class="link-btn" @click.stop="toggleDetail(route.id)">
                                {{ expandedId === route.id ? '收起详情' : '路线详情' }}
                            </button>
                        </div>
                        <ol v-if="expandedId === route.id" class="route-steps">
                            <li v-for="(step, index) in route.steps" :key="index">{{ step }}</li>
                        </ol>
                    </div>
                </section>
            </aside>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, nextTick, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import PlantMap from '../components/PlantMap.vue'
import {
    STATUS_TEXT,
    TYPE_COLOR,
    TYPE_LABEL,
    boundScale,
    boundScaleIds,
    gateMaterialsOf,
    gateModeOf,
    loadConfig,
    materialList,
    nodeList,
    nodeMap,
    plantMeta,
    plantState,
    poiGroups,
    poiNodes,
    roadsWithState,
    scaleById,
    scaleList,
    scaleStatusOf,
    setBinding,
    suggestedScaleFor,
    toggleRoad,
} from '../system/PlantStore.js'
import {fmtDistance, fmtDuration} from '../utils/FmtX.js'
import {buildNetwork, kShortestPaths, mergeDetails, routeDetail} from '../utils/RouteX.js'
import {speak, speechSupported, stopSpeak} from '../utils/SpeechX.js'

const ROUTE_COLORS = ['#1677ff', '#00a870', '#ff8f1f']
const ROUTE_RANKS = ['推荐', '备选一', '备选二']
const LEGEND = [
    {type: 'gate', label: '大门', color: '#1c4785'},
    {type: 'scale', label: '磅房', color: '#1677ff'},
    {type: 'parking', label: '停车场', color: '#8b5cf6'},
    {type: 'pickup', label: '拉货点', color: '#00a870'},
    {type: 'delivery', label: '送货点', color: '#ff8f1f'},
]

const mode = ref('material')
const mapCardRef = ref(null)
const freeStartId = ref('gate_w')
const freeEndId = ref('s1')
const materialStartId = ref('gate_w')
// 物资寻路方向：in 进场（大门 → 磅房 → 装卸点）、out 出场（装卸点 → 磅房 → 大门）
const materialFlow = ref('in')
const materialEndGateId = ref('')
const materialId = ref(materialList.value[0]?.id || '')
const routes = ref([])
const activeId = ref('')
// 行驶动画预览：只有点击路线上的播放按钮才播放一次，切换路线不自动播放
const previewId = ref('')
const previewKey = ref(0)
const expandedId = ref('')
const errorMsg = ref('')
const roadMode = ref(false)
const legend = LEGEND

const outMaterials = computed(() => materialList.value.filter((item) => item.direction === 'out'))
const inMaterials = computed(() => materialList.value.filter((item) => item.direction === 'in'))

const startId = computed(() => {
    if (mode.value !== 'material') return freeStartId.value
    // 出场寻路：起点固定为该物资的装卸点
    return materialFlow.value === 'out' ? effectivePointId.value : materialStartId.value
})
const endId = computed(() => {
    if (mode.value !== 'material') return freeEndId.value
    // 出场寻路：终点为允许该物资出场的大门
    return materialFlow.value === 'out' ? materialEndGateId.value : effectivePointId.value
})
const startName = computed(() => nodeMap.value.get(startId.value)?.name || '-')
const endName = computed(() => nodeMap.value.get(endId.value)?.name || '-')
const routeTitle = computed(() => {
    if (mode.value !== 'material') return `${startName.value} → ${endName.value}`
    if (scaleChoice.value) {
        const scale = scaleById.value.get(scaleChoice.value)
        if (scale?.nodeId === startId.value || scale?.nodeId === endId.value) {
            return `${startName.value} → ${endName.value}`
        }
        return `${startName.value} → ${scale?.name || '磅房'} → ${endName.value}`
    }
    return `${startName.value} → ${endName.value} · ${usableScales.value.length} 座磅房可选`
})
const mapRoutes = computed(() => routes.value.map((route) => ({...route, selected: route.id === activeId.value})))
// 图例点击控制某类点位在地图上的显示/隐藏（默认全部显示）
const hiddenTypes = ref(new Set())
const visibleNodes = computed(() => poiNodes.value.filter((node) => !hiddenTypes.value.has(node.type)))

function toggleType(type) {
    const next = new Set(hiddenTypes.value)
    if (next.has(type)) next.delete(type)
    else next.add(type)
    hiddenTypes.value = next
}

const material = computed(() => materialList.value.find((item) => item.id === materialId.value))
const materialVoice = computed(() => (material.value?.voiceText || '').trim())
const voiceReady = speechSupported()
const speaking = ref(false)
// '' 表示自动：该物资绑定的每座正常磅房各生成一条最优方案
const scaleChoice = ref('')
const usableScales = computed(() => boundScaleIds(material.value)
    .map((id) => scaleById.value.get(id))
    .filter((scale) => scale && scaleStatusOf(scale.id) === 1))

/** 默认途经磅房：仅一座可用磅房时直接选中它；多座时默认 ''（自动） */
function normalizeScaleChoice() {
    const list = usableScales.value
    if (list.length === 1) {
        scaleChoice.value = list[0].id
    } else if (!list.some((scale) => scale.id === scaleChoice.value)) {
        scaleChoice.value = ''
    }
}

/**
 * 允许当前物资进厂的大门：
 * - 大门白名单为空（不限物资）或包含该物资
 * - 大门通行方向不是「仅出厂」（out），仅出厂大门不能作为起点
 */
const materialStartGates = computed(() => {
    const matId = material.value?.id
    if (!matId) return []
    return nodeList.value.filter((node) => {
        if (node.type !== 'gate') return false
        if (gateModeOf(node.id) === 'out') return false
        const allowed = gateMaterialsOf(node.id)
        return allowed.length === 0 || allowed.includes(matId)
    })
})

/** 切换物资或大门配置变化时，若当前起点不在允许列表中，则重置为第一个允许的大门 */
function normalizeMaterialStart() {
    const gates = materialStartGates.value
    if (!gates.length) {
        materialStartId.value = ''
        return
    }
    if (!gates.some((gate) => gate.id === materialStartId.value)) {
        materialStartId.value = gates[0].id
    }
}

/**
 * 允许当前物资出场的大门：
 * - 大门白名单为空（不限物资）或包含该物资
 * - 大门通行方向不是「仅进厂」（in），仅进厂大门不能作为终点
 */
const materialEndGates = computed(() => {
    const matId = material.value?.id
    if (!matId) return []
    return nodeList.value.filter((node) => {
        if (node.type !== 'gate') return false
        if (gateModeOf(node.id) === 'in') return false
        const allowed = gateMaterialsOf(node.id)
        return allowed.length === 0 || allowed.includes(matId)
    })
})

/** 出场寻路的终点大门：当前选择失效时回退到第一个允许出场的大门 */
function normalizeMaterialEnd() {
    const gates = materialEndGates.value
    if (!gates.length) {
        materialEndGateId.value = ''
        return
    }
    if (!gates.some((gate) => gate.id === materialEndGateId.value)) {
        materialEndGateId.value = gates[0].id
    }
}

const materialPoint = computed(() => nodeMap.value.get(material.value?.pointNodeId || ''))

/** 播放当前物资的语音介绍；再次点击可停止 */
function playMaterialVoice() {
    if (speaking.value) {
        stopSpeak()
        speaking.value = false
        return
    }
    if (!materialVoice.value) {
        ElMessage.warning('该物资尚未配置语音介绍，可在「磅房与绑定」中编辑')
        return
    }
    const started = speak(materialVoice.value, {onEnd: () => { speaking.value = false }})
    if (started) speaking.value = true
    else ElMessage.warning('当前浏览器不支持语音朗读')
}
const materialScaleId = computed(() => material.value?.scaleId || '')
const materialScale = computed(() => scaleById.value.get(materialScaleId.value))
const materialScaleStatus = computed(() => scaleStatusOf(materialScaleId.value))
const fallbackScale = computed(() => (material.value && materialScaleStatus.value !== 1
    ? suggestedScaleFor(material.value)
    : null))
const effectiveScale = computed(() => (materialScaleStatus.value === 1 ? materialScale.value : fallbackScale.value))
const effectivePointId = computed(() => material.value?.pointNodeId || '')
const autoSwitched = computed(() => !!(fallbackScale.value && effectiveScale.value))

// 大门通行方向：in 仅进厂（不能作终点/驶出）、out 仅出厂（不能作起点/驶入）
const GATE_MODE_TEXT = {in: '仅进', out: '仅出'}

function isGateIn(node) {
    return node?.type === 'gate' && gateModeOf(node.id) === 'in'
}

function isGateOut(node) {
    return node?.type === 'gate' && gateModeOf(node.id) === 'out'
}

function nodeLabel(node) {
    const suffix = node.type === 'gate' ? GATE_MODE_TEXT[gateModeOf(node.id)] : ''
    return suffix ? `${node.name}（${suffix}）` : node.name
}

// 下拉选项在子组件插槽里渲染，颜色/文案在此提前算好，避免插槽内调用 setup 函数
const poiGroupOptions = computed(() => poiGroups.value.map((group) => ({
    type: group.type,
    label: group.label,
    color: TYPE_COLOR[group.type] || '#64748b',
    items: group.items.map((node) => ({
        ...node,
        optionLabel: nodeLabel(node),
        typeLabel: TYPE_LABEL[node.type] || '',
        startDisabled: isStartDisabled(node),
        endDisabled: isEndDisabled(node),
        gateOutDisabled: isGateOut(node),
    })),
})))

// 寻路结果生成后，让地图自动缩放到路线范围（手机竖屏下尤其需要）
const focusPoints = computed(() => {
    const route = routes.value.find((item) => item.id === activeId.value) || routes.value[0]
    return route?.points || []
})

function isEndDisabled(node) {
    if (node.id === freeStartId.value) return true
    if (isGateIn(node)) return true
    return node.type === 'scale' && node.status && node.status !== 1
}

function isStartDisabled(node) {
    if (node.id === freeEndId.value) return true
    return isGateOut(node)
}

function summarizeRoads(roads) {
    const groups = []
    for (const road of roads) {
        const name = road.group || road.name
        if (groups[groups.length - 1] !== name) groups.push(name)
    }
    return groups.join(' → ')
}

function closedRoadSet() {
    return new Set(plantState.roads.filter((road) => road.enabled === 0).map((road) => road.id))
}

function toRouteCards(details) {
    const distances = details.map((item) => item.distance)
    const minDistance = Math.min(...distances, Infinity)
    return details.map((item, index) => ({
        ...item,
        id: `route-${index}`,
        rank: ROUTE_RANKS[index] || `备选${index}`,
        color: ROUTE_COLORS[index] || '#64748b',
        shortest: Math.abs(item.distance - minDistance) < 0.5,
        roadSummary: summarizeRoads(item.roads),
        gateText: item.gates.join('、'),
    }))
}

function searchFree(notify = true) {
    errorMsg.value = ''
    const from = freeStartId.value
    const to = freeEndId.value
    if (!from || !to) {
        routes.value = []
        errorMsg.value = '请选择起点和终点'
        return
    }
    if (from === to) {
        routes.value = []
        errorMsg.value = '起点和终点不能相同'
        return
    }

    const endNode = nodeMap.value.get(to)
    if (endNode?.type === 'scale') {
        const scale = scaleList.value.find((item) => item.nodeId === to)
        const status = scaleStatusOf(scale?.id)
        if (status !== 1) {
            routes.value = []
            errorMsg.value = `终点「${endNode.name}」当前${STATUS_TEXT[status]}，请先切换磅房或改选目的地`
            return
        }
    }

    const network = buildNetwork(plantState, closedRoadSet())
    const paths = kShortestPaths(network, from, to, 5)
    if (!paths.length) {
        routes.value = []
        errorMsg.value = '当前路网不可达：可能被封闭道路隔断，或受大门通行方向限制，请检查后重试'
        return
    }

    const details = paths.slice(0, 3).map((path) => routeDetail(network, path.nodes, path.edges))
    routes.value = toRouteCards(details)
    activeId.value = routes.value[0]?.id || ''
    expandedId.value = ''
    if (notify && !routes.value.length) ElMessage.warning('未找到可行路线')
}

function searchMaterial() {
    errorMsg.value = ''
    routes.value = []
    const item = material.value
    const point = nodeMap.value.get(item?.pointNodeId)
    const exiting = materialFlow.value === 'out'

    if (!item || !point) {
        errorMsg.value = '该物资尚未配置取/送货点'
        return
    }

    // 进场：起点为进厂大门，终点为装卸点；出场：起点为装卸点，终点为出场大门
    const gateId = exiting ? materialEndGateId.value : materialStartId.value
    if (!gateId) {
        errorMsg.value = exiting
            ? (materialEndGates.value.length ? '请选择出场大门' : `「${item.name}」没有可出场的大门`)
            : (materialStartGates.value.length ? '请选择进厂大门' : `「${item.name}」没有可进厂的大门`)
        return
    }
    const fromId = exiting ? point.id : gateId
    const toId = exiting ? gateId : point.id

    // 指定磅房时只用该磅房；自动模式下用该物资全部正常磅房
    const targets = scaleChoice.value
        ? usableScales.value.filter((scale) => scale.id === scaleChoice.value)
        : usableScales.value
    if (!targets.length) {
        errorMsg.value = `「${item.name}」没有可用磅房，请先在「磅房与绑定」中处理`
        return
    }

    // 按物资寻路时，大门白名单会限制该物资车辆可通行的大门
    const network = buildNetwork(plantState, closedRoadSet(), item.id)
    // 起点或终点恰好是磅房时，该段视为零距离，直接走另一段
    const zeroLeg = (id) => ({nodes: [id], edges: [], cost: 0})
    const combos = []
    for (const scale of targets) {
        const firstLegs = fromId === scale.nodeId
            ? [zeroLeg(scale.nodeId)]
            : kShortestPaths(network, fromId, scale.nodeId, 2)
        const secondLegs = toId === scale.nodeId
            ? [zeroLeg(scale.nodeId)]
            : kShortestPaths(network, scale.nodeId, toId, 2)
        for (const first of firstLegs) {
            const firstDetail = routeDetail(network, first.nodes, first.edges)
            for (const second of secondLegs) {
                const secondDetail = routeDetail(network, second.nodes, second.edges)
                combos.push({
                    detail: mergeDetails(firstDetail, secondDetail, scale.name),
                    cost: first.cost + second.cost,
                    viaScaleId: scale.id,
                    viaScaleName: scale.name,
                })
            }
        }
    }
    if (!combos.length) {
        errorMsg.value = exiting
            ? `当前路网不可达：从「${point.name}」无法经所选磅房到达「${endName.value}」`
            : `当前路网不可达：从「${startName.value}」无法经所选磅房到达「${point.name}」`
        return
    }
    combos.sort((a, b) => a.cost - b.cost)

    const seen = new Set()
    const unique = combos.filter((combo) => {
        const key = `${combo.viaScaleId}|${combo.detail.points.map((pointItem) => pointItem.join(',')).join(';')}`
        if (seen.has(key)) return false
        seen.add(key)
        return true
    })

    // 自动模式下先保证每座可用磅房都有一条方案，再按时间补足，最多 3 条
    let picked
    if (!scaleChoice.value && unique.length > 1) {
        picked = []
        const seenScales = new Set()
        for (const combo of unique) {
            if (seenScales.has(combo.viaScaleId)) continue
            seenScales.add(combo.viaScaleId)
            picked.push(combo)
            if (picked.length >= 3) break
        }
        for (const combo of unique) {
            if (picked.length >= 3) break
            if (!picked.includes(combo)) picked.push(combo)
        }
        picked.sort((a, b) => a.cost - b.cost)
    } else {
        picked = unique.slice(0, 3)
    }

    routes.value = toRouteCards(picked.map((combo) => ({
        ...combo.detail,
        viaScaleId: combo.viaScaleId,
        viaScaleName: combo.viaScaleName,
    })))
    activeId.value = routes.value[0]?.id || ''
    expandedId.value = ''
}

function search() {
    if (mode.value === 'material') searchMaterial()
    else searchFree()
}

/** 点「开始寻路」：手机竖屏下把地图与方案自动滚入视野，省去手动下滑 */
function startSearch() {
    search()
    if (window.innerWidth > 760 || !routes.value.length) return
    nextTick(() => mapCardRef.value?.scrollIntoView({behavior: 'smooth', block: 'start'}))
}

function swapPoints() {
    const previousStart = freeStartId.value
    freeStartId.value = freeEndId.value
    freeEndId.value = previousStart
    searchFree()
}

function onSetStart(id) {
    if (mode.value === 'free' && id === freeEndId.value) {
        ElMessage.warning('起点不能与终点相同')
        return
    }
    if (isGateOut(nodeMap.value.get(id))) {
        ElMessage.warning(`「${nodeMap.value.get(id)?.name}」为仅出厂大门，不能作为起点`)
        return
    }
    if (mode.value === 'material') {
        // 出场寻路起点固定为物资装卸点：点到装卸点则切换到该物资
        if (materialFlow.value === 'out') {
            const matched = materialList.value.find((item) => item.pointNodeId === id)
            if (matched) {
                if (matched.id === materialId.value) searchMaterial()
                else materialId.value = matched.id
                return
            }
            ElMessage.warning('出场寻路的起点固定为物资装卸点，请在下方选择出场大门')
            return
        }
        if (!materialStartGates.value.some((gate) => gate.id === id)) {
            const node = nodeMap.value.get(id)
            ElMessage.warning(`「${node?.name || id}」不允许「${material.value?.name || ''}」车辆进厂，不能作为起点`)
            return
        }
        materialStartId.value = id
    } else freeStartId.value = id
    search()
}

function onSetEnd(id) {
    if (isGateIn(nodeMap.value.get(id))) {
        ElMessage.warning(`「${nodeMap.value.get(id)?.name}」为仅进厂大门，不能作为终点`)
        return
    }
    if (mode.value === 'material') {
        // 出场寻路终点必须是允许该物资出场的大门
        if (materialFlow.value === 'out') {
            if (!materialEndGates.value.some((gate) => gate.id === id)) {
                const node = nodeMap.value.get(id)
                ElMessage.warning(`「${node?.name || id}」不允许「${material.value?.name || ''}」车辆出场，不能作为终点`)
                return
            }
            materialEndGateId.value = id
            searchMaterial()
            return
        }
        const matched = materialList.value.find((item) => item.pointNodeId === id)
        if (matched) {
            materialId.value = matched.id
            searchMaterial()
            return
        }
        mode.value = 'free'
    }
    if (id === freeStartId.value) {
        ElMessage.warning('终点不能与起点相同')
        return
    }
    freeEndId.value = id
    searchFree()
}

function toggleDetail(id) {
    expandedId.value = expandedId.value === id ? '' : id
}

/** 切换选中路线：不自动播放动画；若正在预览其他路线则停止 */
function selectRoute(id) {
    activeId.value = id
    if (previewId.value && previewId.value !== id) previewId.value = ''
}

/** 播放某条路线的行驶动画（单次），再次点击可重播 */
function previewRoute(route) {
    activeId.value = route.id
    previewId.value = route.id
    previewKey.value += 1
}

function onToggleRoad(roadId) {
    const road = plantState.roads.find((item) => item.id === roadId)
    const willClose = road?.enabled !== 0
    toggleRoad(roadId)
    if (willClose) ElMessage.warning(`已封闭「${road?.name || roadId}」，路线已重新计算`)
    else ElMessage.success(`已恢复「${road?.name || roadId}」通行`)
}

function confirmSwitch() {
    if (!material.value || !effectiveScale.value) return
    switchBinding(material.value, effectiveScale.value.id)
}

function switchBinding(targetMaterial, targetScaleId) {
    if (!targetMaterial || !targetScaleId) return
    const previousScale = boundScale(targetMaterial)
    if (previousScale?.id === targetScaleId) return
    const target = scaleById.value.get(targetScaleId)
    setBinding(targetMaterial.id, targetScaleId)

    if (material.value?.id === targetMaterial.id) {
        if (materialStartId.value === previousScale?.nodeId) materialStartId.value = target.nodeId
        searchMaterial()
    }
    ElMessage.success(`「${targetMaterial.name}」已切换到 ${target.name}，已写入数据库`)
}

watch(routes, () => {
    // 重新寻路后清空预览状态，新结果不自动播放动画
    previewId.value = ''
})

watch(() => plantState.roads.map((road) => road.enabled).join(','), () => {
    if (routes.value.length || errorMsg.value) search()
})

watch(() => plantState.scales.map((scale) => `${scale.id}:${scale.status}`).join(','), () => {
    normalizeScaleChoice()
    if (mode.value === 'material') searchMaterial()
})

// 大门通行方向或物资白名单变化时，重新归一化起终点并重算路线
watch(() => plantState.gates.map((gate) => `${gate.nodeId}:${gate.gateMode}:${(gate.allowedMaterialIds || []).join('|')}`).join(','), () => {
    if (mode.value === 'material') {
        normalizeMaterialStart()
        normalizeMaterialEnd()
        searchMaterial()
    }
})

watch(mode, () => {
    stopSpeak()
    speaking.value = false
    errorMsg.value = ''
    search()
})

watch(materialId, () => {
    stopSpeak()
    speaking.value = false
    // 换物资时重置磅房选择：单座自动选中该磅房，多座回到「自动」
    scaleChoice.value = ''
    normalizeMaterialStart()
    normalizeMaterialEnd()
    normalizeScaleChoice()
    if (mode.value === 'material') searchMaterial()
})

// 切换进场/出场：起终点角色互换，重新归一化后立即重算
watch(materialFlow, () => {
    normalizeMaterialStart()
    normalizeMaterialEnd()
    normalizeScaleChoice()
    if (mode.value === 'material') searchMaterial()
})

watch(scaleChoice, () => {
    if (mode.value === 'material') searchMaterial()
})

onMounted(async () => {
    await loadConfig()
    normalizeMaterialStart()
    normalizeMaterialEnd()
    normalizeScaleChoice()
    search()
})

onBeforeUnmount(() => {
    stopSpeak()
})
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.planner {
    background: #ffffff;
    border: 1px solid #e3eaf3;
    border-radius: 12px;
    padding: 14px 18px 12px;
    box-shadow: 0 4px 16px rgba(31, 58, 95, .05);
}

.mode-row {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 14px;
    padding-bottom: 12px;
    border-bottom: 1px dashed #e6edf6;
}

.mode-label {
    font-size: 13px;
    font-weight: 600;
    color: #475569;
}

.mode-tip {
    font-size: 12px;
    color: #94a3b8;
}

/* 寻路条件竖向堆叠，仿高德导航面板 */
.planner-fields {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    max-width: 620px;
}

.field {
    display: flex;
    align-items: center;
    gap: 10px;
}

.field label {
    flex: 0 0 auto;
    width: 56px;
    font-size: 13px;
    font-weight: 600;
    color: #475569;
    white-space: nowrap;
}

.field-select,
.field-select.wide,
.flow-select {
    flex: 1;
    width: auto;
    min-width: 0;
}

/* 起点/终点一组：左侧绿/红点 + 虚线连接，右侧可互换 */
.pair-box {
    display: flex;
    align-items: stretch;
    gap: 10px;
}

.pair-rail {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: space-between;
    flex: 0 0 auto;
    width: 14px;
    padding: 11px 0;
}

.rail-dot {
    flex: 0 0 auto;
    width: 10px;
    height: 10px;
    border-radius: 50%;
}

.rail-dot.start {
    background: #00a870;
    box-shadow: 0 0 0 3px rgba(0, 168, 112, .16);
}

.rail-dot.end {
    background: #ef4444;
    box-shadow: 0 0 0 3px rgba(239, 68, 68, .16);
}

.rail-line {
    flex: 1;
    width: 2px;
    margin: 4px 0;
    background-image: linear-gradient(#cbd5e1 60%, transparent 0);
    background-size: 2px 8px;
}

.pair-body {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.pair-row {
    display: flex;
    align-items: center;
    gap: 10px;
    min-width: 0;
}

.pair-label {
    flex: 0 0 auto;
    width: 32px;
    font-size: 13px;
    font-weight: 600;
    color: #475569;
    white-space: nowrap;
}

.pair-row .field-select,
.pair-row .fixed-point {
    flex: 1;
    width: auto;
    min-width: 0;
}

.pair-box .swap-btn {
    align-self: center;
}

/* 固定起点/终点（物资装卸点）：用只读样式与可选下拉区分 */
.fixed-point {
    display: flex;
    align-items: center;
    flex: 1;
    width: auto;
    height: 32px;
    padding: 0 11px;
    border: 1px dashed #cbd5e1;
    border-radius: 8px;
    background: #f8fafc;
    color: #475569;
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.field-line {
    flex: 1;
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 8px;
}

.field-line .field-select {
    flex: 1;
    min-width: 0;
}

/* 物资语音介绍：司机不认识学名时点喇叭听一遍 */
.voice-btn {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    flex: 0 0 auto;
    height: 32px;
    padding: 0 12px;
    border: 1px solid #bcd6ff;
    border-radius: 8px;
    background: #f2f7ff;
    color: #1677ff;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    white-space: nowrap;
    transition: background .15s ease, color .15s ease, border-color .15s ease;
}

.voice-btn svg {
    width: 15px;
    height: 15px;
}

.voice-btn:hover:not(:disabled) {
    background: #e6f0ff;
    border-color: #8ec0ff;
}

.voice-btn.playing {
    background: #1677ff;
    border-color: #1677ff;
    color: #ffffff;
}

.voice-btn:disabled {
    opacity: .45;
    cursor: not-allowed;
}

.swap-btn {
    width: 34px;
    height: 34px;
    flex: 0 0 auto;
    border: 1px solid #d5dfec;
    border-radius: 8px;
    background: #f8fafc;
    color: #475569;
    font-size: 16px;
    cursor: pointer;
}

.swap-btn:hover {
    border-color: #1677ff;
    color: #1677ff;
}

.search-btn {
    width: 100%;
    height: 38px;
}

.planner-hint {
    margin-top: 10px;
    font-size: 12px;
    color: #94a3b8;
    line-height: 1.6;
}

.error-tip {
    padding: 10px 14px;
    border-radius: 8px;
    background: #fef2f2;
    border: 1px solid #fbcaca;
    color: #b91c1c;
    font-size: 13px;
}

.workspace {
    display: flex;
    align-items: flex-start;
    gap: 14px;
}

.map-card {
    flex: 1;
    min-width: 0;
    background: #ffffff;
    border: 1px solid #e3eaf3;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 16px rgba(31, 58, 95, .05);
}

.map-head {
    display: flex;
    align-items: center;
    gap: 18px;
    padding: 12px 16px;
    border-bottom: 1px solid #edf2f8;
}

.map-head-main {
    flex: 0 0 auto;
}

.map-title {
    font-size: 15px;
    font-weight: 700;
    color: #1e293b;
}

.map-sub {
    margin-top: 2px;
    font-size: 12px;
    color: #8fa0b8;
}

.legend {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 12px;
}

.legend-item {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 12px;
    color: #64748b;
    white-space: nowrap;
}

.legend-toggle {
    padding: 0;
    border: 0;
    background: none;
    font-family: inherit;
    font-size: 12px;
    color: inherit;
    cursor: pointer;
    transition: opacity .15s ease;
}

.legend-toggle:hover {
    color: #1e293b;
}

.legend-toggle.off {
    opacity: .35;
}

.legend-toggle.off i {
    box-shadow: 0 0 0 1px #cbd5e1;
}

.legend-item i {
    width: 9px;
    height: 9px;
    border-radius: 50%;
}

.legend-item i.legend-route {
    width: 16px;
    height: 3px;
    border-radius: 2px;
    background: #1677ff;
}

.side {
    flex: 0 0 380px;
    width: 380px;
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.side-card {
    background: #ffffff;
    border: 1px solid #e3eaf3;
    border-radius: 12px;
    padding: 14px 16px;
    box-shadow: 0 4px 16px rgba(31, 58, 95, .05);
}

.side-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 10px;
    margin-bottom: 10px;
}

.side-title {
    font-size: 14px;
    font-weight: 700;
    color: #1e293b;
}

.side-sub {
    font-size: 11px;
    color: #94a3b8;
    text-align: right;
}

.switch-notice {
    padding: 9px 11px;
    margin-bottom: 10px;
    border-radius: 8px;
    background: #fff7e6;
    border: 1px solid #f7d9a8;
    color: #b45309;
    font-size: 12px;
    line-height: 1.7;
}

.route-card {
    padding: 11px 12px;
    border: 1px solid #e6edf6;
    border-radius: 10px;
    margin-bottom: 9px;
    cursor: pointer;
    transition: border-color .15s ease, box-shadow .15s ease;
}

.route-card:last-child {
    margin-bottom: 0;
}

.route-card:hover {
    border-color: #b9d3ff;
}

.route-card.active {
    border-color: #1677ff;
    background: #f6faff;
    box-shadow: 0 4px 14px rgba(22, 119, 255, .12);
}

.route-top {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 9px;
}

.route-badge {
    padding: 2px 8px;
    border-radius: 6px;
    color: #ffffff;
    font-size: 11px;
    font-weight: 700;
}

.route-time {
    font-size: 17px;
    font-weight: 700;
    color: #0f172a;
}

.route-dist {
    font-size: 12px;
    color: #64748b;
}

.route-via {
    padding: 1px 7px;
    border-radius: 5px;
    background: #eef5ff;
    color: #2f6cc0;
    font-size: 11px;
    font-weight: 600;
    white-space: nowrap;
}

.route-tag {
    margin-left: auto;
    padding: 1px 6px;
    border-radius: 4px;
    background: #fff7e6;
    color: #d46b08;
    font-size: 10px;
}

/* 路线顶栏右侧：距离最短标签 + 预览播放按钮 */
.route-top-right {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-left: auto;
}

.route-top-right .route-tag {
    margin-left: 0;
}

.preview-btn {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 3px 9px 3px 8px;
    border: 1px solid #cfe0ff;
    border-radius: 999px;
    background: #f2f8ff;
    color: #1677ff;
    font-size: 11px;
    font-weight: 600;
    line-height: 1;
    cursor: pointer;
    transition: background .15s ease, border-color .15s ease, color .15s ease;
}

.preview-btn svg {
    width: 9px;
    height: 11px;
    fill: currentColor;
}

.preview-btn:hover {
    border-color: #1677ff;
    background: #e3f0ff;
}

.preview-btn.playing {
    background: #1677ff;
    border-color: #1677ff;
    color: #ffffff;
}

.route-roads {
    margin-top: 6px;
    font-size: 12px;
    color: #475569;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.route-meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-top: 5px;
    font-size: 11px;
    color: #94a3b8;
}

.link-btn {
    border: none;
    background: none;
    padding: 0;
    color: #1677ff;
    font-size: 11px;
    cursor: pointer;
}

.route-steps {
    margin: 8px 0 0;
    padding: 8px 0 0 18px;
    border-top: 1px dashed #e6edf6;
    font-size: 12px;
    color: #475569;
    line-height: 1.7;
}

.empty-tip {
    padding: 18px 4px;
    text-align: center;
    font-size: 12px;
    color: #94a3b8;
}

@media (max-width: 1180px) {
    .workspace {
        flex-direction: column;
    }

    .side {
        width: 100%;
        flex: 1 1 auto;
    }
}

/* 竖屏手机：司机主要用手机查看，选择器与按钮改为整行、大触控区 */
@media (max-width: 760px) {
    .page {
        gap: 10px;
    }

    .planner {
        padding: 12px;
        border-radius: 10px;
    }

    .mode-row {
        flex-wrap: nowrap;
        gap: 8px;
        margin-bottom: 10px;
        padding-bottom: 10px;
    }

    .mode-label {
        flex: 0 0 auto;
        width: auto;
    }

    /* 手机端省掉说明行：方向下拉里已写明进场/出场路径 */
    .mode-tip {
        display: none;
    }

    .planner-fields {
        max-width: none;
        gap: 10px;
    }

    .field {
        flex-direction: column;
        align-items: stretch;
        gap: 5px;
        width: 100%;
    }

    .field label {
        width: auto;
    }

    .field-select,
    .field-select.wide,
    .flow-select,
    .fixed-point,
    .field :deep(.el-select) {
        flex: none;
        width: 100%;
    }

    .field-line {
        width: 100%;
    }

    /* 物资行含语音按钮：下拉必须可收缩，否则会把按钮挤出容器 */
    .field-line .field-select {
        flex: 1;
        width: auto;
    }

    .pair-label {
        width: 28px;
    }

    /* 手机端触控：下拉/单选/按钮统一放大到 40px 以上 */
    .field :deep(.el-select__wrapper) {
        min-height: 40px;
        font-size: 14px;
    }

    .field :deep(.el-radio-button__inner) {
        padding: 9px 14px;
        font-size: 13px;
    }

    .field :deep(.el-switch) {
        height: 24px;
    }

    .fixed-point {
        height: 40px;
        font-size: 14px;
    }

    .voice-btn {
        height: 40px;
        padding: 0 14px;
    }

    .swap-btn {
        align-self: center;
        width: 40px;
        height: 40px;
    }

    .search-btn {
        width: 100%;
        height: 46px;
        font-size: 15px;
    }

    .planner-hint {
        display: none;
    }

    .workspace {
        gap: 10px;
        align-items: stretch;
    }

    .map-card {
        width: 100%;
    }

    /* 地图头部压成单行：标题 + 图例横向滚动 + 道路管控，给地图让出更多高度 */
    .map-head {
        flex-wrap: nowrap;
        align-items: center;
        gap: 8px;
        padding: 8px 10px;
    }

    .map-head-main {
        flex: 0 0 auto;
    }

    .map-title {
        font-size: 13px;
    }

    .map-sub {
        display: none;
    }

    .legend {
        flex: 1;
        justify-content: flex-start;
        flex-wrap: nowrap;
        overflow-x: auto;
        width: auto;
        gap: 10px;
        padding-bottom: 0;
        scrollbar-width: none;
        -webkit-overflow-scrolling: touch;
    }

    .legend::-webkit-scrollbar {
        display: none;
    }

    .map-head :deep(.el-switch) {
        flex: 0 0 auto;
        margin-left: auto;
    }

    .side {
        width: 100%;
        flex: 1 1 auto;
    }

    .side-card {
        padding: 12px;
        border-radius: 10px;
    }

    .route-card {
        padding: 12px;
    }

    .route-time {
        font-size: 18px;
    }

    .preview-btn {
        padding: 6px 11px 6px 9px;
        font-size: 12px;
    }

    .link-btn {
        padding: 4px 0;
        font-size: 12px;
    }
}

/* 小屏手机（≤360px）：地图头部只留图例，给图例更多可视宽度 */
@media (max-width: 360px) {
    .map-head-main {
        display: none;
    }
}
</style>
