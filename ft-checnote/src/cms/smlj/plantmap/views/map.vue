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
                    {{ mode === 'material' ? '起点 → 该物资绑定磅房 → 取/送货点' : '任意两点间寻路，不经过磅房' }}
                </span>
            </div>

            <div v-if="mode === 'free'" class="planner-fields">
                <div class="field">
                    <label>起点</label>
                    <el-select v-model="freeStartId" filterable placeholder="选择起点" class="field-select">
                        <el-option-group v-for="group in poiGroupOptions" :key="group.type"
                                         :label="`${group.label}（${group.items.length}）`">
                            <el-option v-for="node in group.items" :key="node.id" :label="node.optionLabel"
                                       :value="node.id" :disabled="node.startDisabled">
                                <span class="opt-body">
                                    <i class="opt-dot" :style="{background: group.color}"></i>
                                    <span class="opt-name">{{ node.optionLabel }}</span>
                                    <span class="opt-type">{{ node.typeLabel }}</span>
                                </span>
                            </el-option>
                        </el-option-group>
                    </el-select>
                </div>

                <button class="swap-btn" title="互换起点与终点" @click="swapPoints">⇄</button>

                <div class="field">
                    <label>终点</label>
                    <el-select v-model="freeEndId" filterable placeholder="选择终点" class="field-select wide">
                        <el-option-group v-for="group in poiGroupOptions" :key="group.type"
                                         :label="`${group.label}（${group.items.length}）`">
                            <el-option v-for="node in group.items" :key="node.id" :label="node.optionLabel"
                                       :value="node.id" :disabled="node.endDisabled">
                                <span class="opt-body">
                                    <i class="opt-dot" :style="{background: group.color}"></i>
                                    <span class="opt-name">{{ node.optionLabel }}</span>
                                    <span class="opt-type">{{ node.typeLabel }}</span>
                                </span>
                            </el-option>
                        </el-option-group>
                    </el-select>
                </div>

                <el-button type="primary" size="large" class="search-btn" @click="search()">开始寻路</el-button>
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
                    <label>起点</label>
                    <el-select v-model="materialStartId" filterable class="field-select">
                        <el-option-group v-for="group in poiGroupOptions" :key="group.type"
                                         :label="`${group.label}（${group.items.length}）`">
                            <el-option v-for="node in group.items" :key="node.id" :label="node.optionLabel"
                                       :value="node.id" :disabled="node.gateOutDisabled">
                                <span class="opt-body">
                                    <i class="opt-dot" :style="{background: group.color}"></i>
                                    <span class="opt-name">{{ node.optionLabel }}</span>
                                    <span class="opt-type">{{ node.typeLabel }}</span>
                                </span>
                            </el-option>
                        </el-option-group>
                    </el-select>
                </div>

                <div class="via-chip">
                    <span class="via-label">途经磅房</span>
                    <span class="via-value" :class="{bad: effectiveScale && scaleStatusOf(effectiveScale.id) !== 1}">
                        {{ effectiveScale?.name || '无可用磅房' }}
                    </span>
                    <span v-if="effectiveScale" class="status-pill" :class="`st-${scaleStatusOf(effectiveScale.id)}`">
                        {{ STATUS_TEXT[scaleStatusOf(effectiveScale.id)] }}
                    </span>
                </div>

                <el-button type="primary" size="large" class="search-btn" @click="search()">开始寻路</el-button>
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
            <section class="map-card">
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
                    @set-start="onSetStart"
                    @set-end="onSetEnd"
                    @select-route="activeId = $event"
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
                         :class="{active: route.id === activeId}" @click="activeId = route.id">
                        <div class="route-top">
                            <span class="route-badge" :style="{background: route.color}">{{ route.rank }}</span>
                            <span class="route-time">{{ fmtDuration(route.seconds) }}</span>
                            <span class="route-dist">{{ fmtDistance(route.distance) }}</span>
                            <span v-if="route.shortest" class="route-tag">距离最短</span>
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
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue'
import {ElMessage} from 'element-plus'
import PlantMap from '../components/PlantMap.vue'
import {
    STATUS_TEXT,
    TYPE_COLOR,
    TYPE_LABEL,
    boundScale,
    gateModeOf,
    loadConfig,
    materialList,
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
const freeStartId = ref('gate_w')
const freeEndId = ref('s1')
const materialStartId = ref('gate_w')
const materialId = ref(materialList.value[0]?.id || '')
const routes = ref([])
const activeId = ref('')
const expandedId = ref('')
const errorMsg = ref('')
const roadMode = ref(false)
const legend = LEGEND

const outMaterials = computed(() => materialList.value.filter((item) => item.direction === 'out'))
const inMaterials = computed(() => materialList.value.filter((item) => item.direction === 'in'))

const startId = computed(() => (mode.value === 'material' ? materialStartId.value : freeStartId.value))
const endId = computed(() => (mode.value === 'material' ? effectivePointId.value : freeEndId.value))
const startName = computed(() => nodeMap.value.get(startId.value)?.name || '-')
const endName = computed(() => nodeMap.value.get(endId.value)?.name || '-')
const routeTitle = computed(() => {
    if (mode.value !== 'material') return `${startName.value} → ${endName.value}`
    const scaleName = effectiveScale.value?.name || '待选磅房'
    return `${startName.value} → ${scaleName} → ${endName.value}`
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
    const scale = effectiveScale.value
    const point = nodeMap.value.get(item?.pointNodeId)

    if (!item || !point) {
        errorMsg.value = '该物资尚未配置取/送货点'
        return
    }
    if (!scale) {
        errorMsg.value = `「${item.name}」没有可用磅房，请先在「磅房与绑定」中处理`
        return
    }
    if (!materialStartId.value) {
        errorMsg.value = '请选择起点'
        return
    }

    // 按物资寻路时，大门白名单会限制该物资车辆可通行的大门
    const network = buildNetwork(plantState, closedRoadSet(), item.id)
    const firstLegs = kShortestPaths(network, materialStartId.value, scale.nodeId, 2)
    const secondLegs = kShortestPaths(network, scale.nodeId, point.id, 2)
    if (!firstLegs.length || !secondLegs.length) {
        errorMsg.value = `当前路网不可达：从「${startName.value}」无法经「${scale.name}」到达「${point.name}」`
        return
    }

    const combos = []
    for (const first of firstLegs) {
        const firstDetail = routeDetail(network, first.nodes, first.edges)
        for (const second of secondLegs) {
            const secondDetail = routeDetail(network, second.nodes, second.edges)
            combos.push({
                detail: mergeDetails(firstDetail, secondDetail, scale.name),
                cost: first.cost + second.cost,
            })
        }
    }
    combos.sort((a, b) => a.cost - b.cost)

    const seen = new Set()
    const unique = combos.filter((combo) => {
        const key = combo.detail.points.map((pointItem) => pointItem.join(',')).join(';')
        if (seen.has(key)) return false
        seen.add(key)
        return true
    })

    routes.value = toRouteCards(unique.slice(0, 3).map((combo) => combo.detail))
    activeId.value = routes.value[0]?.id || ''
    expandedId.value = ''
}

function search() {
    if (mode.value === 'material') searchMaterial()
    else searchFree()
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
    if (mode.value === 'material') materialStartId.value = id
    else freeStartId.value = id
    search()
}

function onSetEnd(id) {
    if (isGateIn(nodeMap.value.get(id))) {
        ElMessage.warning(`「${nodeMap.value.get(id)?.name}」为仅进厂大门，不能作为终点`)
        return
    }
    if (mode.value === 'material') {
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

watch(() => plantState.roads.map((road) => road.enabled).join(','), () => {
    if (routes.value.length || errorMsg.value) search()
})

watch(() => plantState.scales.map((scale) => `${scale.id}:${scale.status}`).join(','), () => {
    if (mode.value === 'material') searchMaterial()
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
    if (mode.value === 'material') searchMaterial()
})

onMounted(async () => {
    await loadConfig()
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

.planner-fields {
    display: flex;
    align-items: flex-end;
    gap: 14px;
    flex-wrap: wrap;
}

.field {
    display: flex;
    align-items: center;
    gap: 8px;
}

.field label {
    font-size: 13px;
    font-weight: 600;
    color: #475569;
    white-space: nowrap;
}

.field-select {
    width: 220px;
}

.field-select.wide {
    width: 240px;
}

.field-line {
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
    margin-bottom: 1px;
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
    height: 34px;
}

.via-chip {
    display: flex;
    align-items: center;
    gap: 8px;
    height: 34px;
    padding: 0 12px;
    border-radius: 8px;
    background: #f8fafc;
    border: 1px solid #e6edf6;
}

.via-label {
    font-size: 12px;
    color: #94a3b8;
}

.via-value {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
}

.via-value.bad {
    color: #b91c1c;
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

.route-tag {
    margin-left: auto;
    padding: 1px 6px;
    border-radius: 4px;
    background: #fff7e6;
    color: #d46b08;
    font-size: 10px;
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

.status-pill {
    padding: 2px 9px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
}

.status-pill.st-1 {
    color: #00875a;
    background: #e8f8f1;
}

.status-pill.st-2 {
    color: #b45309;
    background: #fff7e6;
}

.status-pill.st-3 {
    color: #b91c1c;
    background: #fef2f2;
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
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 10px;
        padding-bottom: 10px;
    }

    .mode-label {
        width: 100%;
    }

    .mode-tip {
        width: 100%;
        line-height: 1.5;
    }

    .planner-fields {
        flex-direction: column;
        align-items: stretch;
        gap: 10px;
    }

    .field {
        flex-direction: column;
        align-items: stretch;
        gap: 5px;
        width: 100%;
    }

    .field-select,
    .field-select.wide,
    .field :deep(.el-select) {
        width: 100%;
    }

    .field-line {
        width: 100%;
    }

    .voice-btn {
        height: 38px;
        padding: 0 14px;
    }

    .swap-btn {
        align-self: flex-end;
        margin: -4px 0;
    }

    .search-btn {
        width: 100%;
        height: 44px;
        font-size: 15px;
    }

    .planner-hint {
        display: none;
    }

    .via-chip {
        width: 100%;
        height: 38px;
    }

    .workspace {
        gap: 10px;
    }

    .map-head {
        flex-wrap: wrap;
        gap: 8px;
        padding: 10px 12px;
    }

    .map-head-main {
        flex: 1 1 100%;
    }

    .map-sub {
        display: none;
    }

    .legend {
        justify-content: flex-start;
        flex-wrap: nowrap;
        overflow-x: auto;
        width: 100%;
        gap: 14px;
        padding-bottom: 2px;
    }

    .side {
        width: 100%;
        flex: 1 1 auto;
    }

    .route-card {
        padding: 10px;
    }

    .route-time {
        font-size: 16px;
    }
}
</style>
