<template>
    <div class="page" :class="{'rail-collapsed': isPhone && !railOpen, land: landLayout, narrow: narrowLayout}"
         :style="{'--map-top': mapTop + 'px', '--map-left': mapLeft + 'px'}">
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

            <!-- 收起时整块 .planner 被隐藏，这里不再单独 v-show（否则两处状态要同步） -->
            <div class="planner-body">
            <div v-if="mode === 'free'" class="planner-fields">
                <div class="field-row">
                    <div class="field">
                        <label><i class="lbl-dot start"></i>起点</label>
                        <el-select v-model="freeStartId" :filterable="!isPhone" :teleported="!isPhone" placeholder="选择起点" class="field-select">
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

                    <button class="swap-btn" title="互换起点与终点" @click="swapPoints">⇄</button>

                    <div class="field">
                        <label><i class="lbl-dot end"></i>终点</label>
                        <el-select v-model="freeEndId" :filterable="!isPhone" :teleported="!isPhone" placeholder="选择终点" class="field-select">
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

                <el-button type="primary" size="large" class="search-btn" @click="startSearch">开始寻路</el-button>
            </div>

            <div v-else class="planner-fields">
                <div class="field-row">
                    <div class="field material-field">
                        <label>物资</label>
                        <div class="field-line">
                            <el-select v-model="materialId" :filterable="!isPhone" :teleported="!isPhone" class="field-select">
                                <el-option-group label="拉货">
                                    <el-option v-for="item in outMaterials" :key="item.id" :label="item.name"
                                               :value="item.id"/>
                                </el-option-group>
                                <el-option-group label="送货">
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
                    <div class="field flow-field">
                        <label>方向</label>
                        <el-select v-model="materialFlow" :teleported="!isPhone" class="field-select flow-select">
                            <!-- 字段里只显示「进场/出场」，完整路径放在下拉项右侧，避免窄栏里被截成「进场（…」 -->
                            <el-option label="进场" value="in">
                                <span class="opt-body">
                                    <span class="opt-name">进场</span>
                                    <span class="opt-type">大门 → 磅房 → 装卸点</span>
                                </span>
                            </el-option>
                            <el-option label="出场" value="out">
                                <span class="opt-body">
                                    <span class="opt-name">出场</span>
                                    <span class="opt-type">装卸点 → 磅房 → 大门</span>
                                </span>
                            </el-option>
                        </el-select>
                    </div>
                </div>

                <div class="field-row">
                    <div class="field">
                        <label><i class="lbl-dot start"></i>起点</label>
                        <el-select v-if="materialFlow === 'in'" v-model="materialStartId" :filterable="!isPhone" :teleported="!isPhone"
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
                    <div class="field">
                        <label><i class="lbl-dot end"></i>终点</label>
                        <el-select v-if="materialFlow === 'out'" v-model="materialEndGateId" :filterable="!isPhone" :teleported="!isPhone"
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
                    <div class="field">
                        <label><span class="full">途经</span>磅房</label>
                        <el-select v-model="scaleChoice" :teleported="!isPhone" class="field-select" placeholder="自动选择可用磅房">
                            <el-option v-if="usableScales.length > 1" label="自动（每座可用磅房各出一条方案）" value=""/>
                            <el-option v-for="scale in usableScales" :key="scale.id"
                                       :label="`${scale.name}${scale.area ? ' · ' + scale.area : ''}`"
                                       :value="scale.id"/>
                        </el-select>
                    </div>
                </div>

                <el-button type="primary" size="large" class="search-btn" @click="startSearch">开始寻路</el-button>
            </div>

            <div class="planner-hint">
                点击地图上的地点也可设置起终点；道路封闭管控请到「配置 / 地图配置」的道路页签操作。
                磅房、物资绑定与点位坐标存于 device-note 的 nav schema，可在「地图配置」中调整。
            </div>
            </div>
        </section>

        <!-- 收起态：左栏整块隐藏后，地图左上角浮出这枚按钮把左栏放回来 -->
        <button v-if="isPhone && !railOpen" class="rail-open" @click="railOpen = true">
            <i class="tri"></i>展开
        </button>

        <div v-if="plantState.loadError" class="error-tip">
            后端配置不可用，当前展示的是内置默认配置；修改状态或保存会失败，请检查 device-note 服务（8092）。
        </div>
        <div v-if="errorMsg" class="error-tip">{{ errorMsg }}</div>

        <div class="workspace">
            <section ref="mapCardRef" class="map-card">
                <div class="map-head">
                    <div class="map-head-main">
                        <div class="map-title">厂区平面图</div>
                        <div class="map-sub">卫星底图 · 点位与路网可配置 · 滚轮缩放 / 拖拽平移</div>
                    </div>
                    <div class="legend">
                        <button v-for="item in legend" :key="item.type" class="legend-item legend-toggle"
                                :class="{off: hiddenTypes.has(item.type)}" :title="`点击${hiddenTypes.has(item.type) ? '显示' : '隐藏'}${item.label}`"
                                @click="toggleType(item.type)">
                            <i :style="{background: item.color}"></i>{{ item.label }}
                        </button>
                        <span class="legend-item"><i class="legend-route"></i>路线</span>
                    </div>
                </div>

                <PlantMap
                    :nodes="visibleNodes"
                    :roads="roadsWithState"
                    :routes="mapRoutes"
                    :start-id="startId"
                    :end-id="endId"
                    :image-url="plantMeta.imageUrl"
                    :image-width="plantMeta.imageWidth"
                    :image-height="plantMeta.imageHeight"
                    :meters-per-pixel="plantMeta.metersPerPixel"
                    :focus-points="focusPoints"
                    :preview-route-id="previewId"
                    :preview-key="previewKey"
                    :playable="true"
                    :playing="playing"
                    @set-start="onSetStart"
                    @set-end="onSetEnd"
                    @select-route="selectRoute"
                    @preview-end="previewId = ''"
                    @toggle-play="togglePlay"
                />
            </section>

            <aside class="side">
                <section class="side-card" :class="{'is-empty': !routes.length}">
                    <div class="side-head">
                        <span class="side-title">寻路方案</span>
                        <!-- 起终点摘要只在桌面显示；手机端这个位置让给收起/展开按钮 -->
                        <span class="side-sub" v-if="routes.length && !isPhone">
                            {{ routes.length }} 条 · {{ routeTitle }}
                        </span>
                        <!-- 收起/展开按钮放在方案卡右上角：条件卡会随页面滚走，按钮留在方案区顶部
                             （再配 sticky 头，方案较多时也不会被滚出视野） -->
                        <button v-if="isPhone" class="mode-collapse" @click="railOpen = !railOpen">
                            <i class="tri" :class="{open: railOpen}"></i>{{ railOpen ? '收起' : '展开' }}
                        </button>
                    </div>
                    <div class="speed-tip">厂区道路统一限速 25 km/h，用时为估算值</div>

                    <div v-if="autoSwitched" class="switch-notice">
                        绑定磅房「{{ materialScale?.name }}」当前{{ STATUS_TEXT[materialScaleStatus] }}，
                        本次按备用磅「{{ effectiveScale.name }}」计算。
                        <el-button link type="danger" @click="confirmSwitch">确认切至 {{ effectiveScale.name }}</el-button>
                    </div>

                    <div v-if="!routes.length" class="empty-tip">
                        选择起点和终点后点击「开始寻路」
                    </div>

                    <div v-for="route in routes" :key="route.id" class="route-card"
                         :class="{active: route.id === activeId}"
                         :style="{'--route-color': route.color}" @click="selectRoute(route.id)">
                        <div class="route-top">
                            <span class="route-badge" :style="{background: route.color}">{{ route.rank }}</span>
                            <span class="route-time">{{ fmtDuration(route.seconds) }}</span>
                            <span class="route-dist">{{ fmtDistance(route.distance) }}</span>
                            <!-- 手机端在此折行：上行=时长+里程，下行=途经磅房+标签+预览；桌面端隐藏 -->
                            <i class="route-brk"></i>
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
    ORIENT_LANDSCAPE,
    ORIENT_PORTRAIT,
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
    pageOrientationOf,
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
} from '../system/PlantStore.js'
import {fmtDistance, fmtDuration} from '../utils/FmtX.js'
import {buildNetwork, kShortestPaths, mergeDetails, routeDetail} from '../utils/RouteX.js'
import {isSpeaking, primeSpeech, speak, speechSupported, stopSpeak} from '../utils/SpeechX.js'

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
const legend = LEGEND
// 手机端：左栏（寻路条件 + 寻路方案）整体展开/收起，收起后地图独占整屏
const railOpen = ref(true)
// 手机端地图固定铺满导航栏以下时的顶部偏移（topbar + 横向导航高度，动态测量）
const mapTop = ref(106)
// 手机横屏时左侧图标导航的宽度（地图/面板整体右移让位）
const mapLeft = ref(0)

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
 * - 大门通行方向不是「仅出厂」（out）也不是「禁行」（none）
 */
const materialStartGates = computed(() => {
    const matId = material.value?.id
    if (!matId) return []
    return nodeList.value.filter((node) => {
        if (node.type !== 'gate') return false
        if (gateModeOf(node.id) === 'out' || gateModeOf(node.id) === 'none') return false
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
 * - 大门通行方向不是「仅进厂」（in）也不是「禁行」（none）
 */
const materialEndGates = computed(() => {
    const matId = material.value?.id
    if (!matId) return []
    return nodeList.value.filter((node) => {
        if (node.type !== 'gate') return false
        if (gateModeOf(node.id) === 'in' || gateModeOf(node.id) === 'none') return false
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
    if (!started) {
        ElMessage.warning('当前浏览器不支持语音朗读')
        return
    }
    speaking.value = true
    // 发起后仍未出声，多半是手机静音开关或媒体音量为 0
    setTimeout(() => {
        if (speaking.value && !isSpeaking()) {
            speaking.value = false
            ElMessage.warning('没有播放出声音，请检查手机静音开关和媒体音量')
        }
    }, 900)
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

// 大门通行方向：in 仅进厂（不能作终点）、out 仅出厂（不能作起点）、none 禁行（起点终点都不可用）
const GATE_MODE_TEXT = {in: '仅进', out: '仅出', none: '禁行'}

function isGateIn(node) {
    return node?.type === 'gate' && gateModeOf(node.id) === 'in'
}

function isGateOut(node) {
    return node?.type === 'gate' && gateModeOf(node.id) === 'out'
}

function isGateNone(node) {
    return node?.type === 'gate' && gateModeOf(node.id) === 'none'
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
    if (isGateIn(node) || isGateNone(node)) return true
    return node.type === 'scale' && node.status && node.status !== 1
}

function isStartDisabled(node) {
    if (node.id === freeEndId.value) return true
    return isGateOut(node) || isGateNone(node)
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
            : kShortestPaths(network, fromId, scale.nodeId, 3)
        const secondLegs = toId === scale.nodeId
            ? [zeroLeg(scale.nodeId)]
            : kShortestPaths(network, scale.nodeId, toId, 3)
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

/** 点「开始寻路」：手机端地图常驻全屏无需滚动；窄窗口 PC 端自动把地图滚入视野 */
function startSearch() {
    search()
    if (isPhone.value || !routes.value.length) return
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
    if (isGateNone(nodeMap.value.get(id))) {
        ElMessage.warning(`「${nodeMap.value.get(id)?.name}」为禁行大门，不允许通行`)
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
    if (isGateNone(nodeMap.value.get(id))) {
        ElMessage.warning(`「${nodeMap.value.get(id)?.name}」为禁行大门，不允许通行`)
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

/** 是否正在播放行驶预览（按钮图标与地图光标都由 previewId 驱动） */
const playing = computed(() => !!previewId.value)

/** 播放 / 停止当前选中路线：没有选中就退回第一条；播放中再点一次即停 */
function togglePlay() {
    if (previewId.value) {
        previewId.value = ''
        return
    }
    const route = routes.value.find((item) => item.id === activeId.value) || routes.value[0]
    if (!route) return
    previewRoute(route)
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

// 手机端（竖屏 ≤760px，或横屏且高 ≤560px）关闭下拉搜索：filterable 会渲染可聚焦输入框，
// iOS（Safari/Edge/Chrome 同为 WebKit）聚焦到字号 <16px 的输入框会整页放大，还会弹软键盘
const isPhone = ref(false)
// 是否套用横屏车机版式（.page.land）：按切页配置 + 视口算，见 landscapeLayout()
const landLayout = ref(false)
// 是否套用窄屏手机版式（.page.narrow）：同上，见 narrowCanvas()
const narrowLayout = ref(false)

// 判据直接复用 CSS 的两条媒体查询：设备旋转时 innerWidth/innerHeight 可能晚一拍才更新，
// 用 matchMedia 可保证 JS 与样式表永远认同一个视口状态（否则网格会拿旧偏移量布局）
const PHONE_MQ = '(max-width: 760px), (orientation: landscape) and (max-height: 560px), (orientation: portrait)'
const LANDSCAPE_MQ = '(orientation: landscape) and (max-height: 560px)'
// 本页在 nav.page_display 里的标识（与路由 name 一致）
const PAGE_CODE = 'home_map'

/**
 * 是否套用横屏车机版式（.page.land）：由「配置 / 磅房与绑定」里本页的显示方向决定——
 * 1 始终横屏：竖着拿时也算横屏（home.vue 会把整层壳旋转 90°）；
 * 2 始终竖屏：永远竖屏版式；
 * 0 跟随手机横竖屏：视口横着才用横屏版式（大屏仍走桌面版式，所以还要看高度）。
 */
function landscapeLayout() {
    const orientation = pageOrientationOf(PAGE_CODE)
    if (orientation === ORIENT_PORTRAIT) return false
    if (window.matchMedia('(orientation: portrait)').matches) return orientation === ORIENT_LANDSCAPE
    return window.matchMedia(LANDSCAPE_MQ).matches
}

/**
 * 是否套用窄屏手机版式（.page.narrow）：这批规则原先写在 @media (max-width:760px) 里。
 * 改成类名是因为「始终竖屏」的切页在横屏视口下会被 home.vue 反向旋转成竖画布——
 * 那时画布只有 393 宽，但媒体查询看到的仍是 852，媒体查询表达不了这种「画布窄、视口宽」。
 * 强制横屏（.land）自带一整套横屏版式，不叠加 narrow（调用方已用 !landscape 短路）。
 */
function narrowCanvas() {
    if (pageOrientationOf(PAGE_CODE) === ORIENT_PORTRAIT) return true
    return window.matchMedia('(max-width: 760px)').matches
}

function syncPhoneLayout() {
    const landscape = landscapeLayout()
    landLayout.value = landscape
    narrowLayout.value = !landscape && narrowCanvas()
    isPhone.value = window.matchMedia(PHONE_MQ).matches
    const topbar = document.querySelector('.topbar')
    const nav = document.querySelector('.sidenav')
    if (landscape) {
        // 横屏外壳：顶栏压扁、导航变左侧图标条 ⇒ 页面从顶栏底边铺到屏幕底，左侧让出图标条宽度
        mapTop.value = Math.round(topbar?.offsetHeight || 42)
        mapLeft.value = Math.round(nav?.offsetWidth || 64)
        return
    }
    mapLeft.value = 0
    if (isPhone.value) {
        // 竖屏：导航在顶部横排，量出实际高度，地图从导航底边铺到屏幕底
        mapTop.value = Math.round((topbar?.offsetHeight || 52) + (nav?.offsetHeight || 54))
    }
}

// 旋转/缩放后补一次测量：iOS 上 offsetHeight 可能晚一拍才反映新布局
let syncTimer = 0

function onViewportChange() {
    syncPhoneLayout()
    clearTimeout(syncTimer)
    syncTimer = setTimeout(syncPhoneLayout, 320)
}

// 首帧就把偏移算对：mapTop 默认值是竖屏量级(106)，横屏若不提前同步会先按 106 铺一次再跳
syncPhoneLayout()

/*
 * 移动浏览器（iOS Safari/Edge、Android Chrome）只在**文档真的能滚动**时才收起地址栏。
 * 本页是 fixed 铺满的（横屏车机版式 / 竖屏强制旋转），文档本身没有可滚动高度，地址栏就永远杵在那。
 * 做法：往 body 末尾垫一个 60px 的「滚距」元素（在 fixed 版式之外，不参与页面布局），
 * 再把文档往下滚这 60px —— 地址栏随之收走，而页面纹丝不动（fixed 定位不受文档滚动影响）。
 * ⚠️ 每次挂载都要清理：这是直接写 body 的副作用，不清理会漏到其它页面。
 */
const ADDR_SCROLL = 60
let addrSpacer = null
let addrTimer = 0

function nudgeAddressBar() {
    if (!isPhone.value) return
    if (!addrSpacer) {
        addrSpacer = document.createElement('div')
        addrSpacer.className = 'addr-nudge'
        addrSpacer.setAttribute('aria-hidden', 'true')
        addrSpacer.style.cssText = `height:${ADDR_SCROLL}px;pointer-events:none`
        document.body.appendChild(addrSpacer)
    }
    window.scrollTo(0, ADDR_SCROLL)
}

function clearAddressBarNudge() {
    if (!addrSpacer) return
    addrSpacer.remove()
    addrSpacer = null
    window.scrollTo(0, 0)
}

onMounted(async () => {
    syncPhoneLayout()
    window.addEventListener('resize', onViewportChange)
    window.addEventListener('orientationchange', onViewportChange)
    primeSpeech()
    await loadConfig()
    // 配置到货后才拿到本页的显示方向，重量一次偏移量（首帧按默认值量过一遍）
    syncPhoneLayout()
    normalizeMaterialStart()
    normalizeMaterialEnd()
    normalizeScaleChoice()
    search()
    // 地址栏：首屏后与布局稳定后各试一次（iOS 有时第一帧还没算完滚动高度）
    nextTick(nudgeAddressBar)
    addrTimer = setTimeout(nudgeAddressBar, 320)
})

onBeforeUnmount(() => {
    clearTimeout(syncTimer)
    clearTimeout(addrTimer)
    window.removeEventListener('resize', onViewportChange)
    window.removeEventListener('orientationchange', onViewportChange)
    clearAddressBarNudge()
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

/* 手机端模式行右侧：面板收起按钮 */
.mode-collapse {
    flex: 0 0 auto;
    margin-left: auto;
    height: 30px;
    padding: 0 10px;
    border: 1px solid #d5dfec;
    border-radius: 8px;
    background: #f8fafc;
    color: #475569;
    font-size: 12px;
    cursor: pointer;
}

.mode-collapse:hover {
    border-color: #1677ff;
    color: #1677ff;
}

/* 展开/收起的三角：默认 ▶（把左栏拉出来的方向）；展开态转 180° 变 ◀（把左栏收回去的方向）。
 * ⚠️ 之前是 rotate(90deg) 变 ▼，可左栏是往左收的，箭头朝下与动作方向不一致。 */
.tri {
    display: inline-block;
    width: 0;
    height: 0;
    margin-right: 4px;
    vertical-align: 1px;
    border-left: 6px solid currentColor;
    border-top: 4px solid transparent;
    border-bottom: 4px solid transparent;
    transition: transform .15s;
}

.tri.open {
    transform: rotate(180deg);
}

/* 收起态：左栏整体隐藏，地图左上角浮出这枚按钮把左栏放回来（位置按横竖屏各自覆盖） */
/* 收起态：左栏整块移除。与版式无关——竖屏下 .planner / .side 都是 fixed 浮层，
   不隐藏就会盖在地图上（类 rail-collapsed 加在 .page 自身，故用复合选择器） */
.page.rail-collapsed .planner,
.page.rail-collapsed .side {
    display: none;
}

.rail-open {
    position: fixed;
    z-index: 26;
    display: inline-flex;
    align-items: center;
    height: 30px;
    padding: 0 12px 0 10px;
    border: 1px solid rgba(148, 163, 184, .55);
    border-radius: 999px;
    background: rgba(15, 23, 42, .74);
    color: #e2e8f0;
    font-family: inherit;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    -webkit-backdrop-filter: blur(6px);
    backdrop-filter: blur(6px);
    box-shadow: 0 4px 14px rgba(15, 23, 42, .35);
}

.rail-open:hover {
    border-color: #1677ff;
    color: #ffffff;
}

/* 寻路条件竖向堆叠，仿高德导航面板 */
.planner-fields {
    display: flex;
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
    max-width: 500px;
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

/* 合并行：物资+语音+方向 一行、起点+终点+途经磅房 一行。
   全部成员 min-width:0 可收缩，容器再窄也只压缩下拉、永不重叠；
   flex-wrap 仅兜底固定宽成员（桌面端方向 220px 放不下时才落到下一行） */
.field-row {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
}

.field-row > .field {
    flex: 1;
    min-width: 0;
    gap: 8px;
}

.field-row > .field label {
    width: auto;
    flex: 0 0 auto;
    gap: 0;
}

/* 物资字段：下拉按「最长物资名」给宽，不再把整行吃满（语音按钮紧随其后） */
.field-row > .field.material-field {
    flex: 1 1 auto;
    min-width: 0;
    max-width: 300px;
}

/* 方向只显示「进场/出场」两个字：150px 够用，剩余宽度留给物资行（手机横屏块里另有更紧的一套） */
.field-row .flow-field {
    flex: 0 1 150px;
    min-width: 0;
}

/* 起点/终点标签前的小色点（合行后左侧竖轨放不下，改为行内圆点） */
.lbl-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    margin-right: 5px;
    vertical-align: 1px;
    border-radius: 50%;
}

.lbl-dot.start {
    background: #00a870;
    box-shadow: 0 0 0 2px rgba(0, 168, 112, .16);
}

.lbl-dot.end {
    background: #ef4444;
    box-shadow: 0 0 0 2px rgba(239, 68, 68, .16);
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

/* 播放/停止当前路线预览：图标在 三角 ↔ 方块 之间切换（状态源就是 previewId） */
.map-card :deep(.map-btn.play) {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 0;
}

.map-card :deep(.map-btn.play svg) {
    width: 15px;
    height: 15px;
}

.map-card :deep(.map-btn.play.playing) {
    border-color: #1677ff;
    background: #eaf3ff;
    color: #1677ff;
}

.map-card :deep(.map-btn:disabled) {
    opacity: .45;
    cursor: not-allowed;
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

.speed-tip {
    margin: -6px 0 10px;
    font-size: 11px;
    color: #94a3b8;
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
    /* 左侧色条 = 该方案在地图上的线色（替代原来的 推荐/备选 文字徽标） */
    border: 1px solid #e6edf6;
    border-left: 3px solid var(--route-color, #e6edf6);
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

/* 手机端折行锚点，桌面端不折行 */
.route-brk {
    display: none;
}

/* 「推荐 / 备选一 / 备选二」文字徽标不再展示（三种版式一致）：
 * 与地图线色的对应关系由 .route-card 左侧 3px 色条承担，卡片更安静也省宽度 */
.route-badge {
    display: none;
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
.page.narrow {
    gap: 10px;
}

.page.narrow .planner {
    padding: 12px;
    border-radius: 10px;
}

.page.narrow .mode-row {
    flex-wrap: nowrap;
    gap: 8px;
    margin-bottom: 8px;
    padding-bottom: 8px;
}

.page.narrow .mode-label {
    flex: 0 0 auto;
    width: auto;
}

/* 手机端省掉说明行：方向下拉里已写明进场/出场路径 */
.page.narrow .mode-tip {
    display: none;
}

.page.narrow .planner-fields {
    max-width: none;
    gap: 8px;
}

/* 标签行内左置（不再单独占一行），压缩面板纵向空间 */
.page.narrow .field {
    flex-direction: row;
    align-items: center;
    gap: 8px;
    width: 100%;
}

.page.narrow .field label {
    width: auto;
}

.page.narrow .field-select,
.page.narrow .field-select.wide,
.page.narrow .flow-select,
.page.narrow .fixed-point,
.page.narrow .field :deep(.el-select) {
    flex: none;
    width: 100%;
}

.page.narrow .field-line {
    width: 100%;
}

/* 物资行含语音按钮：下拉必须可收缩，否则会把按钮挤出容器 */
.page.narrow .field-line .field-select {
    flex: 1;
    width: auto;
}

/* 合行窄屏压缩：标签 12px、方向下拉固定窄宽、「途经」二字隐藏成「磅房」 */
.page.narrow .field-row {
    gap: 6px;
}

.page.narrow .field-row > .field {
    gap: 6px;
}

/* 压掉上方通用的 flex:none/width:100%（合行内必须可收缩等分） */
.page.narrow .field-row > .field .field-select,
.page.narrow .field-row > .field .fixed-point,
.page.narrow .field-row > .field .field-line {
    flex: 1;
    width: auto;
    min-width: 0;
}

.page.narrow .field-row > .field label {
    font-size: 12px;
}

.page.narrow .field-row .flow-field {
    flex: 0 0 auto;
}

.page.narrow .field-row .flow-field .field-select {
    flex: 0 0 118px;
}

.page.narrow .field-row .full {
    display: none;
}

/* 语音按钮收成纯图标，把宽度让给物资下拉；整行仍保持一行 */
.page.narrow .voice-btn span {
    display: none;
}

/* 手机端触控：控件统一 34px 高（比 40px 紧凑，仍可点按） */
.page.narrow .field :deep(.el-select__wrapper) {
    min-height: 34px;
    font-size: 13px;
}

.page.narrow .field :deep(.el-radio-button__inner) {
    padding: 7px 12px;
    font-size: 13px;
}

.page.narrow .field :deep(.el-switch) {
    height: 24px;
}

.page.narrow .fixed-point {
    height: 34px;
    font-size: 13px;
}

.page.narrow .voice-btn {
    height: 34px;
    padding: 0 10px;
}

.page.narrow .swap-btn {
    align-self: center;
    width: 34px;
    height: 34px;
}

.page.narrow .search-btn {
    width: 100%;
    height: 34px;
    font-size: 13px;
}

.page.narrow .planner-hint {
    display: none;
}

.page.narrow .workspace {
    gap: 10px;
    align-items: stretch;
}

.page.narrow .map-card {
    width: 100%;
}

/* 地图头部压成单行：标题 + 图例横向滚动，给地图让出更多高度 */
.page.narrow .map-head {
    flex-wrap: nowrap;
    align-items: center;
    gap: 8px;
    padding: 8px 10px;
}

.page.narrow .map-head-main {
    flex: 0 0 auto;
}

.page.narrow .map-title {
    font-size: 13px;
}

.page.narrow .map-sub {
    display: none;
}

.page.narrow .legend {
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

.page.narrow .legend::-webkit-scrollbar {
    display: none;
}

.page.narrow .map-head :deep(.el-switch) {
    flex: 0 0 auto;
    margin-left: auto;
}

.page.narrow .side {
    width: 100%;
    flex: 1 1 auto;
}

.page.narrow .side-card {
    padding: 12px;
    border-radius: 10px;
}

.page.narrow .route-card {
    padding: 12px;
}

.page.narrow .route-time {
    font-size: 18px;
}

.page.narrow .preview-btn {
    padding: 6px 11px 6px 9px;
    font-size: 12px;
}

.page.narrow .link-btn {
    padding: 4px 0;
    font-size: 12px;
}


/* 小屏手机（≤360px）：地图头部只留图例，给图例更多可视宽度 */
@media (max-width: 360px) {
    .map-head-main {
        display: none;
    }
}

/* ===== 手机端仿导航 App 布局：地图铺满整个屏幕，条件面板悬浮顶部（可收起），方案卡片贴底横滑 ===== */
/* 地图固定铺满导航栏以下整个屏幕 */
.page.narrow .map-card {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    top: var(--map-top, 106px);
    border: none;
    border-radius: 0;
    box-shadow: none;
}

/* 地图头部（标题/图例）收起：屏幕让给地图 */
.page.narrow .map-head {
    display: none;
}

/* 模式行去掉文字标签，防止窄屏溢出 */
.page.narrow .mode-label {
    display: none;
}

.page.narrow .map-card :deep(.map-shell) {
    height: 100%;
    border-radius: 0;
}

.page.narrow .map-card :deep(.plant-svg) {
    height: 100%;
    aspect-ratio: auto;
}

/* 悬浮控件上移，避开底部方案卡片条 */
.page.narrow .map-card :deep(.map-controls) {
    bottom: 152px;
}

.page.narrow .map-card :deep(.scale-bar) {
    bottom: 126px;
}

.page.narrow .map-card :deep(.pick-card) {
    bottom: 196px;
}

/* 收起态见文件末尾「竖屏收起」块：小屏横屏也会命中本块，
       若把 top/bottom 覆盖写在这里会踩到横屏那套（6xx×3xx 两套媒体查询同时命中） */
/* 条件面板：半透明悬浮在地图上方 */
.page.narrow .planner {
    position: fixed;
    left: 8px;
    right: 8px;
    top: calc(var(--map-top, 106px) + 6px);
    z-index: 20;
    padding: 10px 12px;
    max-height: calc(100vh - var(--map-top, 106px) - 160px);
    max-height: calc(100dvh - var(--map-top, 106px) - 160px);
    overflow-y: auto;
    background: rgba(255, 255, 255, .97);
    -webkit-backdrop-filter: blur(10px);
    backdrop-filter: blur(10px);
    border: none;
    border-radius: 14px;
    box-shadow: 0 10px 30px rgba(31, 58, 95, .22);
}

/* 错误提示浮在方案卡片条上方 */
.page.narrow .error-tip {
    position: fixed;
    left: 10px;
    right: 10px;
    bottom: 132px;
    z-index: 24;
}

/* 方案区：固定贴底，卡片横向滑动；空态整体不可见也不挡地图触摸 */
.page.narrow .side {
    position: fixed;
    left: 8px;
    right: 8px;
    bottom: 10px;
    z-index: 22;
    width: auto;
    pointer-events: none;
}

.page.narrow .side-card {
    position: relative;
    display: flex;
    align-items: stretch;
    gap: 10px;
    overflow-x: auto;
    /* 滚动容器自身必须可命中：iOS Safari 按命中测试构建触摸滚动链，
    pointer-events:none 的滚动容器真机上拖不动（桌面仿真走滚轮路径无感） */
    pointer-events: auto;
    scroll-snap-type: x proximity;
    overscroll-behavior-x: contain;
    touch-action: pan-x;
    padding: 2px;
    background: transparent;
    border: none;
    box-shadow: none;
    scrollbar-width: none;
}

/* 无方案时整体放行触摸，不挡地图 */
.page.narrow .side-card.is-empty {
    pointer-events: none;
}

.page.narrow .side-card::-webkit-scrollbar {
    display: none;
}

.page.narrow .side-head,
.page.narrow .speed-tip,
.page.narrow .empty-tip {
    display: none;
}

/* 磅房切换提示浮在卡片条上方 */
.page.narrow .switch-notice {
    position: absolute;
    left: 0;
    right: 0;
    bottom: calc(100% + 6px);
    margin: 0;
    pointer-events: auto;
}

/* 高德式紧凑卡片：竖屏约可见 2.5 张，两行结构（时长+里程 / 磅房+标签+预览） */
.page.narrow .route-card {
    flex: 0 0 auto;
    width: 37vw;
    max-width: 150px;
    margin: 0;
    padding: 7px 8px;
    scroll-snap-align: start;
    pointer-events: auto;
    background: rgba(255, 255, 255, .97);
    border-radius: 12px;
    box-shadow: 0 8px 24px rgba(31, 58, 95, .2);
}

/* 只留：时长 / 里程 / 途经磅房 / 预览；推荐备选标签、道路摘要、详情链接不展示 */
.page.narrow .route-badge,
.page.narrow .route-roads,
.page.narrow .route-meta,
.page.narrow .route-steps {
    display: none;
}

.page.narrow .route-top {
    flex-wrap: wrap;
    gap: 2px 5px;
}

/* 折行：上行 时长+里程，下行 磅房+标签+预览 */
.page.narrow .route-brk {
    display: block;
    flex-basis: 100%;
    height: 0;
}

.page.narrow .route-time {
    font-size: 14px;
    white-space: nowrap;
}

.page.narrow .route-dist {
    font-size: 10.5px;
    white-space: nowrap;
}

.page.narrow .route-via {
    padding: 1px 4px;
    font-size: 9.5px;
}

.page.narrow .route-tag {
    padding: 1px 3px;
    font-size: 9px;
}

/* 预览压成纯图标圆钮（悬停提示仍在） */
.page.narrow .preview-btn {
    padding: 3px;
    border-radius: 50%;
}

.page.narrow .preview-btn span {
    display: none;
}

.page.narrow .preview-btn svg {
    width: 9px;
    height: 11px;
}


/* ===== 横屏：车机式（CarPlay / CarLife）导航布局 =====
 * 页面整体铺满顶栏以下区域，两列网格：左栏 = 寻路条件 + 候选方案（与整页共用一条垂直滚动条），
 * 右列 = 地图吃满剩余空间，图例/控件以浮层压在地图上。
 * ⚠️ 是否套用本套版式**不再由媒体查询决定**（切页显示方向可配置后，竖屏视口下只有
 *    「始终横屏」才该用横屏版式，媒体查询表达不了这个条件），改由 .page.land 这个类名驱动，
 *    类名在 syncPhoneLayout() 里按 config + 视口算出来（见 landscapeLayout）。
 * ⚠️ 本块必须排在竖屏块之后：后者（max-width:760px）在窄竖屏里同样可能命中，靠顺序覆盖。
 */
.page.land {
    --rail-w: clamp(266px, 40%, 380px);
    position: fixed;
    left: var(--map-left, 0);
    right: 0;
    top: var(--map-top, 42px);
    bottom: 0;
    display: grid;
    grid-template-columns: var(--rail-w) minmax(0, 1fr);
    /* 行 1 = 条件面板（按内容），行 2 = 方案（吃掉剩余高度）：两块都在 .page 这一个
       滚动容器里，共用一条垂直滚动条。
       ⚠️ 行 2 必须写 1fr，不能两条都 max-content：地图是跨两行的定高项，网格会把它
       的高度「分摊」进两条 max-content 轨道（跨轨道项的尺寸分配），结果
       ① 条件面板被拉到跟地图一样高 ⇒ 开始寻路按钮下方一大片白；
       ② 两行加起来恒等于可视高 ⇒ 永远没有可滚距离（看着像「滚不动」）。
       1fr 让剩余空间只落在行 2，条件面板始终是内容高度；
       行 2 的 1fr 带 auto 下限（= 方案内容高度），方案一多整页照常出现滚动。 */
    grid-template-rows: max-content 1fr;
    align-content: start;
    gap: 8px;
    padding: 8px;
    background: #0b1220;
    overflow-x: hidden;
    overflow-y: auto;
    /* 不要加 -webkit-overflow-scrolling: touch：祖先 .layout 有 transform（强制横屏），
       该属性在这个结构下会让触摸滚动失效；iOS 13 起原生滚动已足够跟手 */
    overscroll-behavior-y: contain;
}

/* 拆掉工作区这层盒子，让地图与方案栏直接成为 .page 的网格项 */
.page.land .workspace {
    display: contents;
}

/* ---------- 左栏上：寻路条件 ---------- */
.page.land .planner {
    grid-column: 1;
    grid-row: 1;
    /* 条件面板永远只有内容那么高：剩余高度归行 2（方案面板），
       否则卡片被拉高后按钮下方会留一大片空白 */
    align-self: start;
    position: static;
    left: auto;
    right: auto;
    top: auto;
    max-height: none;
    overflow: visible;
    padding: 5px 8px;
    border: none;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: none;
    -webkit-backdrop-filter: none;
    backdrop-filter: none;
}

.page.land .mode-row {
    flex-wrap: nowrap;
    gap: 5px;
    margin-bottom: 4px;
    padding-bottom: 4px;
}

/* 左栏窄：省掉「寻路方式」文字与底部提示，只留分段按钮 + 收起 */
.page.land .mode-label,
.page.land .mode-tip {
    display: none;
}

.page.land .mode-row :deep(.el-radio-button__inner) {
    padding: 5px 9px;
    font-size: 12px;
}

.page.land .mode-collapse {
    height: 26px;
    padding: 0 8px;
    font-size: 11px;
}

/* 标签移到控件上方（竖排单列），窄栏里下拉才有足够宽度可读 */
.page.land .planner-fields {
    max-width: none;
    gap: 4px;
}

.page.land .field-row {
    flex-wrap: nowrap;
    align-items: stretch;
    gap: 6px;
}

.page.land .field-row > .field {
    flex: 1;
    flex-direction: column;
    align-items: stretch;
    gap: 1px;
}

.page.land .field-row > .field label {
    width: 100%;
    font-size: 10px;
    line-height: 1.15;
    color: #64748b;
}

/* 物资：物资名最长 3 字（烧碱/盐酸/电石渣），150px 已到顶 —— 之前吃满整行 189px 太浪费 */
.page.land .field-row > .field.material-field {
    flex: 1 1 auto;
    min-width: 0;
    max-width: 150px;
}

/* 方向只显示「进场/出场」两个汉字：110px 封顶（iPhone Pro Max 横屏时它曾被撑到 169px） */
.page.land .field-row .flow-field {
    flex: 1 1 auto;
    min-width: 64px;
    max-width: 110px;
}

/* 压掉竖屏的整行铺满（合行内必须可收缩等分）； 列方向下不能沿用 flex:1（那是纵向拉伸），统一改成按内容高度。 ⚠️ 必须带 `> .field` 才够竖屏那条 `.field-row > .field .xxx` 的优先级（小屏横屏两块会同时命中） */
.page.land .field-row > .field .field-select,
.page.land .field-row > .field .fixed-point,
.page.land .field-row > .field .field-line {
    flex: 0 0 auto;
    width: 100%;
    min-width: 0;
}

.page.land .field-row .field-line {
    display: flex;
    align-items: center;
    gap: 5px;
}

/* 方向下拉固定占 96px 字段宽；竖屏那条把它钉成 118px，这里要压回来 */
.page.land .field-row .flow-field .field-select {
    flex: 0 0 auto;
    width: 100%;
}

/* 物资下拉跟着字段宽度走（字段已被 material-field 限到 150px），语音方形按钮固定 26px */
.page.land .field-row .field-line .field-select {
    flex: 1 1 auto;
    width: auto;
    min-width: 0;
}

/* 控件统一压到 26px：左栏每省 4px，方案区就多 4px（靠捏合/点按仍够用） */
.page.land .field :deep(.el-select__wrapper) {
    min-height: 26px;
    padding: 0 6px;
    font-size: 12px;
}

.page.land .fixed-point {
    height: 26px;
    padding: 0 6px;
    font-size: 12px;
}

/* 语音按钮收成方形图标，把宽度让给物资下拉 */
.page.land .voice-btn {
    width: 26px;
    height: 26px;
    padding: 0;
    justify-content: center;
}

.page.land .voice-btn span {
    display: none;
}

.page.land .voice-btn svg {
    width: 15px;
    height: 15px;
}

/* 互换按钮贴到下拉那一行的底部 */
.page.land .swap-btn {
    align-self: flex-end;
    width: 26px;
    height: 26px;
    font-size: 13px;
}

.page.land .search-btn {
    height: 28px;
    margin-top: 2px;
    font-size: 13px;
}

.page.land .planner-hint {
    display: none;
}

/* 收起态：左栏整列移除，地图独占整屏（隐藏规则见上方 .page.rail-collapsed 通用块） */
.page.land.rail-collapsed {
    grid-template-columns: minmax(0, 1fr);
}

.page.land.rail-collapsed .map-card {
    grid-column: 1 / -1;
}

/* 顶部图例胶囊让开左上角的「展开」浮钮（浮钮与图例同高同顶边，中间留 6px） */
.page.land.rail-collapsed .map-head {
    left: 72px;
}

/* 没有左栏可让位，错误提示贴回地图左边 */
.page.land.rail-collapsed .error-tip {
    left: 16px;
}

/* 浮钮压在地图左上角，与图例胶囊同顶边同高（.page 自带 8px 内边距，这里要加回来） */
.page.land .rail-open {
    top: calc(var(--map-top, 42px) + 14px);
    left: calc(var(--map-left, 64px) + 14px);
    height: 24px;
    padding: 0 10px 0 9px;
    font-size: 11.5px;
}

/* ---------- 左栏下：候选方案（竖向滚动） ---------- */
.page.land .side {
    grid-column: 1;
    grid-row: 2;
    position: static;
    left: auto;
    right: auto;
    bottom: auto;
    width: auto;
    /* 不要写 min-height: 0：行 2 的 auto 下限因此变成 0，方案一多面板会冲出轨道，
       整页的可滚高度就算少（表现为「滚不到最后一条」） */
    min-height: auto;
    z-index: auto;
    pointer-events: auto;
}

.page.land .side-card {
    display: flex;
    flex-direction: column;
    gap: 6px;
    /* 面板只占内容高度：方案少于剩余高度时不再被行 2 的行高拉长
       （写 1 1 auto 时最后一条方案下面会留一大片白色空白）。
       多余高度留给 .page 的深色底，视觉上就是「内容到此为止」；
       内容高于可视区时行 2 的 auto 下限照常把 .page 撑出滚动 */
    flex: 0 0 auto;
    /* 方案全量展开：不再自己圈滚动区，由 .page 统一滚（与条件面板共用一条滚动条） */
    height: auto;
    padding: 8px;
    overflow: visible;
    background: #ffffff;
    border: 1px solid #e3eaf3;
    border-radius: 12px;
    box-shadow: none;
    pointer-events: auto;
    scroll-snap-type: none;
    touch-action: auto;
    scrollbar-width: thin;
}

.page.land .side-card.is-empty {
    pointer-events: auto;
}

/* 方案卡头部（标题 + 收起按钮）吸顶：方案多或窗口矮时，滚到下半屏也点得到「收起」； 负外边距抵消 .side-card 的 8px 内边距，吸顶时整条横向铺白盖住滚过下方的卡片 */
.page.land .side-head {
    position: sticky;
    top: 0;
    z-index: 3;
    display: flex;
    align-items: center;
    gap: 8px;
    margin: -8px -8px 2px;
    padding: 4px 8px;
    background: #ffffff;
    border-radius: 12px 12px 0 0;
}

.page.land .side-title {
    font-size: 12px;
}

.page.land .side-sub {
    max-width: 62%;
    font-size: 10.5px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

/* 限速提示：横屏也要留（去掉了方案徽标，这里是唯一的口径说明）。 ⚠️ display 必须显式写 block：竖屏块里同名的 .speed-tip 是 display:none， 竖屏强制旋转时两条媒体查询同时命中，只写字号会被压回 none */
.page.land .speed-tip {
    display: block;
    margin: 0 0 2px;
    font-size: 10.5px;
    line-height: 1.5;
    color: #94a3b8;
}

.page.land .empty-tip {
    display: block;
    padding: 10px 4px;
    font-size: 11.5px;
    line-height: 1.6;
}

/* 磅房切换提示原本浮在卡片条上方，横屏改为列表内的一行 */
.page.land .switch-notice {
    position: static;
    margin: 0 0 6px;
    padding: 6px 8px;
    font-size: 11px;
    line-height: 1.6;
}

.page.land .route-card {
    flex: 0 0 auto;
    width: auto;
    max-width: none;
    margin: 0;
    padding: 7px 9px;
    background: #ffffff;
    border-radius: 10px;
    box-shadow: none;
}

.page.land .route-top {
    flex-wrap: nowrap;
    gap: 6px;
    overflow: hidden;
}

/* 中文可在任意字间断行：窄栏里必须钉死 nowrap + 不参与收缩， 否则「3 分钟」「1.18 公里」会被压成竖排单字 */
.page.land .route-badge,
.page.land .route-time,
.page.land .route-dist,
.page.land .route-via,
.page.land .route-tag {
    flex: 0 0 auto;
    white-space: nowrap;
}

/* 横屏一行放得下，去掉竖屏的折行锚点 */
.page.land .route-brk {
    display: none;
}

.page.land .route-time {
    font-size: 14px;
}

.page.land .route-dist {
    font-size: 10.5px;
}

.page.land .route-via {
    max-width: 42%;
    padding: 1px 5px;
    font-size: 9.5px;
    overflow: hidden;
    text-overflow: ellipsis;
}

.page.land .route-top-right {
    margin-left: auto;
    gap: 4px;
    flex: 0 0 auto;
}

/* 横屏卡片一行内要放下 时长/里程/磅房/标签，预览压成圆形图标省宽度 */
.page.land .preview-btn {
    flex: 0 0 auto;
    width: 24px;
    height: 24px;
    padding: 0;
    border-radius: 50%;
    justify-content: center;
}

.page.land .preview-btn span {
    display: none;
}

.page.land .preview-btn svg {
    width: 8px;
    height: 10px;
}

/* 只保留一行道路摘要，够区分方案又不撑高卡片 */
.page.land .route-roads {
    display: -webkit-box;
    margin-top: 4px;
    font-size: 11px;
    -webkit-line-clamp: 1;
}

.page.land .route-meta,
.page.land .route-steps {
    display: none;
}

/* ---------- 右列：地图 ---------- */
.page.land .map-card {
    grid-column: 2;
    grid-row: 1 / span 2;
    /* 左栏变长时地图不跟着拉长：sticky 钉在可视区顶部，高度恒为「可视高 - 顶栏 - 上下留白」。
       --vp-h 由 home.vue 给（真横屏 100dvh；竖屏强制旋转 100dvw，因为那时页面被转了 90°） */
    position: sticky;
    top: 0;
    align-self: start;
    height: calc(var(--vp-h, 100dvh) - var(--map-top, 42px) - 16px);
    left: auto;
    right: auto;
    bottom: auto;
    min-width: 0;
    border: none;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: none;
}

.page.land .map-card :deep(.map-shell) {
    height: 100%;
    border-radius: 0;
}

.page.land .map-card :deep(.plant-svg) {
    height: 100%;
    aspect-ratio: auto;
}

/* 图例悬浮成一枚胶囊压在地图顶部，不再占一行高度 */
.page.land .map-head {
    position: absolute;
    top: 6px;
    left: 6px;
    right: 6px;
    z-index: 6;
    display: flex;
    padding: 0;
    border: none;
    background: none;
    pointer-events: none;
}

.page.land .map-head-main {
    display: none;
}

.page.land .legend {
    flex: 1;
    justify-content: flex-start;
    flex-wrap: nowrap;
    gap: 10px;
    padding: 5px 10px;
    border-radius: 999px;
    background: rgba(15, 23, 42, .62);
    -webkit-backdrop-filter: blur(6px);
    backdrop-filter: blur(6px);
    pointer-events: auto;
    overflow-x: auto;
    scrollbar-width: none;
}

.page.land .legend::-webkit-scrollbar {
    display: none;
}

.page.land .legend-item {
    font-size: 10.5px;
    color: #e2e8f0;
}

.page.land .legend-toggle:hover {
    color: #ffffff;
}

/* 下拉面板：E2 的浮层用 client 坐标（floating-ui）定位，页面被旋转 90° 后这套坐标 与布局坐标系不是同一个（实测面板被甩到 left:803px 的屏幕外）。手机端一律改成 内联渲染（模板里 :teleported="!isPhone"）再用 CSS 定死「字段正下方、至少同宽」， 面板就会跟着页面一起旋转。 ⚠️ 不要覆盖 display：开合由组件自己控制，写死会让面板一直显示 */
.page.land .planner :deep(.el-select__popper) {
    position: absolute !important;
    top: 100% !important;
    /* 必须同时清掉 bottom/right：浮层翻转朝上时组件会写 inset: auto auto 38px 0px，
       留着 bottom 会和 top:100% 一起把高度压成 0（实测面板只剩 2px 高，等于不显示） */
    right: auto !important;
    bottom: auto !important;
    left: 0 !important;
    transform: none !important;
    width: max-content !important;
    min-width: 100% !important;
    max-width: 260px !important;
    margin: 4px 0 0;
    max-height: 180px;
    overflow-y: auto;
}

/* 复位 / 播放横排贴地图右上角（全屏按钮已下线，不再需要给它留 42px 位） */
.page.land .map-card :deep(.map-controls) {
    top: 42px;
    bottom: auto;
    right: 8px;
    flex-direction: row;
    gap: 4px;
}

.page.land .map-card :deep(.map-btn) {
    width: 30px;
    height: 30px;
    border-radius: 7px;
    font-size: 15px;
    box-shadow: 0 2px 8px rgba(31, 58, 95, .22);
}

.page.land .map-card :deep(.map-btn.reset) {
    width: auto;
    min-width: 30px;
    padding: 0 6px;
    font-size: 10px;
}

.page.land .map-card :deep(.map-btn svg) {
    width: 15px;
    height: 15px;
}

.page.land .map-card :deep(.compass) {
    top: 42px;
    left: 8px;
    transform: scale(.8);
    transform-origin: top left;
}

.page.land .map-card :deep(.scale-bar) {
    left: 8px;
    bottom: 8px;
}

.page.land .map-card :deep(.pick-card) {
    left: 8px;
    bottom: 30px;
    width: 200px;
  }

.page.land .map-card :deep(.road-mode-tip),
.page.land .map-card :deep(.edit-tip) {
    top: 44px;
    font-size: 11px;
}

.page.land .map-card :deep(.road-edit-tip) {
    top: 76px;
}

/* 错误提示压在地图底部，不占网格位 */
.page.land .error-tip {
    position: absolute;
    left: calc(var(--rail-w) + 16px);
    right: 16px;
    bottom: 12px;
    z-index: 25;
    padding: 6px 10px;
    font-size: 11px;
    line-height: 1.5;
}

/* 小屏（≤760px，如 iPhone SE 横放 / 竖屏强制横屏）：卡片一行放不下「距离最短」，
   优先保住 时长/里程/磅房/预览 */
@media (max-width: 760px) {
    .page.land .route-tag {
        display: none;
    }
}
</style>
