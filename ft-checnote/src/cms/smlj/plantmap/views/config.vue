<template>
    <div class="page">
        <section class="toolbar">
            <div class="toolbar-main">
                <div class="toolbar-title">地图配置</div>
                <div class="toolbar-sub">
                    拖动地图上的点位即可修正坐标；配置整体保存到 device-note 的 nav schema。
                </div>
            </div>
            <span v-if="dirty" class="dirty-tag">有未保存修改</span>
            <span v-if="plantState.loadError" class="error-tag">后端不可用，当前为内置默认配置</span>
            <el-button size="small" :loading="plantState.saving" @click="reload">重新加载</el-button>
            <el-button size="small" type="primary" :loading="plantState.saving" @click="save">保存到数据库</el-button>
        </section>

        <div class="workspace">
            <section class="map-card">
                <div class="map-head">
                    <span class="map-title">厂区平面图</span>
                    <span class="map-sub">滚轮缩放 · 拖拽平移 · 拖动点位/道路控制点调整</span>
                    <el-switch v-model="roadMode" size="small" class="map-road-switch" active-text="道路管控"/>
                </div>
                <PlantMap
                    :nodes="plantState.nodes"
                    :roads="roadsWithState"
                    :road-mode="roadMode"
                    :image-url="plantState.plant.imageUrl"
                    :image-width="plantState.plant.imageWidth"
                    :image-height="plantState.plant.imageHeight"
                    :meters-per-pixel="plantState.plant.metersPerPixel"
                    editable
                    fullscreenable
                    :selected-node-id="selectedId"
                    :selected-road-id="selectedRoadId"
                    @move-node="onMoveNode"
                    @select-node="onSelectNode"
                    @select-road="selectRoad"
                    @pick-point="onPickPoint"
                    @move-waypoint="onMoveWaypoint"
                    @insert-waypoint="onInsertWaypoint"
                    @remove-waypoint="onRemoveWaypoint"
                    @toggle-road="onToggleRoad"
                />
            </section>

            <aside class="side">
                <el-tabs v-model="tab" class="tabs">
                    <el-tab-pane label="点位" name="node">
                        <div class="pane">
                            <div class="pane-actions">
                                <el-button size="small" type="primary" plain @click="startAdd">新增点位</el-button>
                            </div>


                            <div v-if="adding" class="add-form">
                                <div class="add-title">新增点位</div>
                                <el-form label-width="64px" size="small">
                                    <el-form-item label="名称">
                                        <el-input v-model="draft.name" placeholder="如 9#磅房"/>
                                    </el-form-item>
                                    <el-form-item label="编码">
                                        <el-input v-model="draft.code" placeholder="如 B9"/>
                                    </el-form-item>
                                    <el-form-item label="类型">
                                        <el-select v-model="draft.type" style="width: 100%">
                                            <el-option v-for="item in TYPE_OPTIONS" :key="item.value"
                                                       :label="item.label" :value="item.value"/>
                                        </el-select>
                                    </el-form-item>
                                    <el-form-item label="坐标">
                                        <span class="coord-text">
                                            {{ draft.x === null ? '请在地图上点击取点' : `${draft.x}, ${draft.y}` }}
                                        </span>
                                    </el-form-item>
                                </el-form>
                                <div class="add-actions">
                                    <el-button size="small" @click="cancelAdd">取消</el-button>
                                    <el-button size="small" type="primary" @click="commitAdd">添加</el-button>
                                </div>
                            </div>

                            <div class="list">
                                <div v-for="node in sortedNodes" :key="node.id" class="row"
                                     :class="{active: node.id === selectedId}" @click="onSelectNode(node.id)">
                                    <span class="dot" :style="{background: typeColor(node.type)}"></span>
                                    <div class="row-main">
                                        <div class="row-title">
                                            {{ node.name }}
                                            <span class="row-code">{{ node.code }}</span>
                                            <span v-if="node.visible === false" class="row-hidden">仅寻路</span>
                                        </div>
                                        <div class="row-sub">
                                            {{ TYPE_LABEL[node.type] }} · {{ fmtCoord(node) }}
                                        </div>
                                    </div>
                                    <el-button link type="danger" size="small" @click.stop="removeNode(node)">
                                        删除
                                    </el-button>
                                </div>
                            </div>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane label="道路" name="road">
                        <div class="pane">
                            <div class="add-form">
                                <div class="add-title">新增道路</div>
                                <el-form label-width="64px" size="small">
                                    <el-form-item label="起点">
                                        <el-select v-model="roadDraft.from" filterable style="width: 100%">
                                            <el-option v-for="node in plantState.nodes" :key="node.id"
                                                       :label="node.name" :value="node.id"/>
                                        </el-select>
                                    </el-form-item>
                                    <el-form-item label="终点">
                                        <el-select v-model="roadDraft.to" filterable style="width: 100%">
                                            <el-option v-for="node in plantState.nodes" :key="node.id"
                                                       :label="node.name" :value="node.id"/>
                                        </el-select>
                                    </el-form-item>
                                    <el-form-item label="名称">
                                        <el-input v-model="roadDraft.name" placeholder="留空自动生成"/>
                                    </el-form-item>
                                    <el-form-item label="等级">
                                        <el-select v-model="roadDraft.level" style="width: 100%">
                                            <el-option label="主干道" value="main"/>
                                            <el-option label="次干道" value="secondary"/>
                                            <el-option label="支路" value="branch"/>
                                        </el-select>
                                    </el-form-item>
                                </el-form>
                                <div class="add-actions">
                                    <el-button size="small" type="primary" @click="addRoad">添加道路</el-button>
                                </div>
                            </div>

                            <div class="edit-hint road-hint">
                                点击道路选中；选中后再点线段可插入控制点；拖动紫色控制点调整走向；双击控制点删除。
                            </div>

                            <div class="list">
                                <div v-for="road in plantState.roads" :key="road.id" class="row"
                                     :class="{active: road.id === selectedRoadId}" @click="selectRoad(road.id)">
                                    <div class="row-main">
                                        <div class="row-title">
                                            {{ road.name }}
                                            <span class="row-code">{{ ROAD_LEVEL_TEXT[road.level] || road.level }}</span>
                                        </div>
                                        <div class="row-sub">
                                            {{ nodeName(road.from) }} → {{ nodeName(road.to) }}
                                            · {{ fmtLength(road.points) }}
                                            · {{ waypointCount(road) }} 个控制点
                                        </div>
                                    </div>
                                    <el-button link size="small"
                                               :type="road.enabled === 0 ? 'success' : 'warning'"
                                               @click.stop="toggleRoad(road.id, true)">
                                        {{ road.enabled === 0 ? '恢复' : '封路' }}
                                    </el-button>
                                    <el-button link type="danger" size="small" @click.stop="removeRoad(road)">
                                        删除
                                    </el-button>
                                </div>
                            </div>
                        </div>
                    </el-tab-pane>

                    <el-tab-pane label="基础信息" name="base">
                        <div class="pane">
                            <el-form label-width="110px" size="small">
                                <el-form-item label="厂区名称">
                                    <el-input v-model="plantState.plant.name"/>
                                </el-form-item>
                                <el-form-item label="底图地址">
                                    <el-input v-model="plantState.plant.imageUrl"/>
                                </el-form-item>
                                <el-form-item label="底图宽 / 高">
                                    <div class="size-row">
                                        <el-input-number v-model="plantState.plant.imageWidth" :min="100"
                                                         controls-position="right"/>
                                        <span class="size-x">×</span>
                                        <el-input-number v-model="plantState.plant.imageHeight" :min="100"
                                                         controls-position="right"/>
                                    </div>
                                </el-form-item>
                                <el-form-item label="米 / 像素">
                                    <el-input-number v-model="plantState.plant.metersPerPixel" :min="0.01" :step="0.1"
                                                     :precision="2" controls-position="right"/>
                                </el-form-item>
                                <el-form-item label="说明">
                                    <el-input v-model="plantState.plant.remark" type="textarea" :rows="2"/>
                                </el-form-item>
                            </el-form>
                            <div class="base-tip">
                                点位 {{ plantState.nodes.length }} 个 · 道路 {{ plantState.roads.length }} 条 ·
                                磅房 {{ plantState.scales.length }} 座 · 物资 {{ plantState.materials.length }} 种
                            </div>
                        </div>
                    </el-tab-pane>
                </el-tabs>
            </aside>
        </div>

        <el-drawer v-model="nodeDrawer" :title="selectedNode ? `编辑点位 · ${selectedNode.name}` : '编辑点位'"
                   :size="drawerSize" direction="rtl" class="node-drawer">
            <div v-if="selectedNode" class="drawer-form">
                <el-form label-width="72px" size="small">
                    <el-form-item label="名称">
                        <el-input v-model="selectedNode.name" @input="markDirty"/>
                    </el-form-item>
                    <el-form-item label="编码">
                        <el-input v-model="selectedNode.code" @input="markDirty"/>
                    </el-form-item>
                    <el-form-item label="类型">
                        <el-select v-model="selectedNode.type" style="width: 100%"
                                   @change="onNodeTypeChange(selectedNode)">
                            <el-option v-for="item in TYPE_OPTIONS" :key="item.value"
                                       :label="item.label" :value="item.value"/>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="状态">
                        <el-select v-model="selectedNode.status" style="width: 100%"
                                   @change="markDirty">
                            <el-option label="正常" :value="1"/>
                            <el-option label="检修" :value="2"/>
                            <el-option label="停用" :value="3"/>
                        </el-select>
                    </el-form-item>
                    <el-form-item label="页面展示">
                        <div class="visible-row">
                            <el-switch v-model="selectedNode.visible" @change="markDirty"/>
                            <span class="visible-tip">
                                {{ selectedNode.visible ? '地图与站点列表可见' : '仅参与寻路，不在页面展示' }}
                            </span>
                        </div>
                    </el-form-item>
                    <el-form-item label="坐标 X / Y">
                        <div class="size-row">
                            <el-input-number v-model="selectedNode.x" :controls="false"
                                             @change="syncNodeCoords"/>
                            <span class="size-x">,</span>
                            <el-input-number v-model="selectedNode.y" :controls="false"
                                             @change="syncNodeCoords"/>
                        </div>
                    </el-form-item>
                    <el-form-item label="微调">
                        <div class="nudge">
                            <button class="nudge-btn" title="左移 1px（Shift 5px）"
                                    @click="nudgeNode(-1, 0)">←
                            </button>
                            <button class="nudge-btn" title="上移 1px（Shift 5px）"
                                    @click="nudgeNode(0, -1)">↑
                            </button>
                            <button class="nudge-btn" title="下移 1px（Shift 5px）"
                                    @click="nudgeNode(0, 1)">↓
                            </button>
                            <button class="nudge-btn" title="右移 1px（Shift 5px）"
                                    @click="nudgeNode(1, 0)">→
                            </button>
                            <span class="nudge-tip">1px / Shift 5px</span>
                        </div>
                    </el-form-item>
                    <el-form-item label="说明">
                        <el-input v-model="selectedNode.description" @input="markDirty"/>
                    </el-form-item>
                </el-form>
                <div class="edit-hint">
                    直接在地图上拖动点位最便捷；也可用上面的微调按钮或方向键（Shift 加速），坐标修改会同步道路端点
                </div>
                <div class="drawer-actions">
                    <el-button size="small" type="danger" plain @click="removeNode(selectedNode)">删除点位</el-button>
                    <el-button size="small" type="primary" @click="nodeDrawer = false">完成</el-button>
                </div>
            </div>
            <div v-else class="drawer-empty">请在地图或列表中选择一个点位</div>
        </el-drawer>
    </div>
</template>

<script setup lang="js">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import PlantMap from '../components/PlantMap.vue'
import {
    TYPE_LABEL,
    TYPE_COLOR,
    loadConfig,
    plantState,
    roadsWithState,
    saveConfig,
    toggleRoad as toggleRoadState,
} from '../system/PlantStore.js'
import {polylineLength} from '../utils/RouteX.js'

const TYPE_OPTIONS = [
    {value: 'gate', label: '大门'},
    {value: 'junction', label: '路口'},
    {value: 'scale', label: '磅房'},
    {value: 'parking', label: '停车场'},
    {value: 'pickup', label: '拉货点'},
    {value: 'delivery', label: '送货点'},
]
const ROAD_LEVEL_TEXT = {main: '主干道', secondary: '次干道', branch: '支路'}

const tab = ref('node')
// 道路管控模式：开启后点击地图道路直接封路/恢复（与道路页签的按钮等价）
const roadMode = ref(false)
const selectedId = ref('')
const selectedRoadId = ref('')
const dirty = ref(false)
const adding = ref(false)
const nodeDrawer = ref(false)
const drawerSize = window.innerWidth <= 640 ? '92%' : '440px'

const draft = reactive({name: '', code: '', type: 'junction', x: null, y: null})
const roadDraft = reactive({from: '', to: '', name: '', level: 'secondary'})

const selectedNode = computed(() => plantState.nodes.find((node) => node.id === selectedId.value))

const sortedNodes = computed(() => [...plantState.nodes].sort((a, b) =>
    (Number(a.sortNo) || 0) - (Number(b.sortNo) || 0)))

function typeColor(type) {
    return TYPE_COLOR[type] || '#64748b'
}

function fmtCoord(node) {
    return `${Number(node.x).toFixed(1)}, ${Number(node.y).toFixed(1)}`
}

function nodeName(id) {
    return plantState.nodes.find((node) => node.id === id)?.name || id
}

function fmtLength(points) {
    const meters = polylineLength(points || []) * (Number(plantState.plant.metersPerPixel) || 1)
    return meters >= 1000 ? `${(meters / 1000).toFixed(2)} 公里` : `${Math.round(meters)} 米`
}

/** 点位拖动：同步该点作为端点的道路折线首尾坐标 */
function onMoveNode({id, x, y}) {
    const node = plantState.nodes.find((item) => item.id === id)
    if (!node) return
    node.x = x
    node.y = y

    for (const road of plantState.roads) {
        if (!Array.isArray(road.points) || !road.points.length) continue
        if (road.from === id) road.points[0] = [x, y]
        if (road.to === id) road.points[road.points.length - 1] = [x, y]
    }
    // 大小磅房记录共用 nodeId，坐标只在 node 上维护
    dirty.value = true
}

function markDirty() {
    dirty.value = true
}

function onSelectNode(id) {
    selectedId.value = id
    selectedRoadId.value = ''
    tab.value = 'node'
    nodeDrawer.value = !!id
    // 焦点若停在输入框，全局方向键会被忽略；选中点位时主动释放
    if (document.activeElement instanceof HTMLElement) document.activeElement.blur()
}

/** 按钮微调选中点位坐标 */
function nudgeNode(dx, dy) {
    const node = selectedNode.value
    if (!node) return
    node.x = Math.round((Number(node.x) + dx) * 10) / 10
    node.y = Math.round((Number(node.y) + dy) * 10) / 10
    syncNodeCoords()
}

function selectRoad(id) {
    selectedRoadId.value = id
    selectedId.value = ''
    nodeDrawer.value = false
    tab.value = 'road'
}

function waypointCount(road) {
    return Math.max(0, (road.points?.length || 0) - 2)
}

/** 点位坐标输入框修改后，同步以该点为首尾的道路折线 */
function syncNodeCoords() {
    const node = selectedNode.value
    if (!node) return
    node.x = Math.round((Number(node.x) || 0) * 10) / 10
    node.y = Math.round((Number(node.y) || 0) * 10) / 10
    for (const road of plantState.roads) {
        if (!Array.isArray(road.points) || !road.points.length) continue
        if (road.from === node.id) road.points[0] = [node.x, node.y]
        if (road.to === node.id) road.points[road.points.length - 1] = [node.x, node.y]
    }
    markDirty()
}

/** 类型变更：磅房需要同步 scale 记录，被物资绑定时禁止改成其它类型 */
function onNodeTypeChange(node) {
    if (!node) return
    if (node.type === 'scale') {
        if (!plantState.scales.some((scale) => scale.nodeId === node.id)) {
            plantState.scales.push({
                id: node.id,
                code: node.code || '',
                name: node.name,
                nodeId: node.id,
                area: '',
                capacity: '',
                status: node.status || 1,
                sortNo: plantState.scales.length + 1,
            })
        }
        markDirty()
        return
    }

    const scale = plantState.scales.find((item) => item.nodeId === node.id)
    if (!scale) {
        markDirty()
        return
    }
    const bound = plantState.materials.some((material) => material.scaleId === scale.id
        || (material.backupScaleIds || []).includes(scale.id))
    if (bound) {
        ElMessage.warning('该磅房已被物资绑定，请先在「磅房与绑定」中调整后再修改类型')
        node.type = 'scale'
        return
    }
    plantState.scales = plantState.scales.filter((item) => item.nodeId !== node.id)
    markDirty()
}

/** 拖动道路控制点 */
function onMoveWaypoint({roadId, index, x, y}) {
    const road = plantState.roads.find((item) => item.id === roadId)
    if (!road?.points?.[index]) return
    road.points[index] = [x, y]
    markDirty()
}

/** 在线段上插入控制点 */
function onInsertWaypoint({roadId, index, x, y}) {
    const road = plantState.roads.find((item) => item.id === roadId)
    if (!road?.points?.length) return
    const safeIndex = Math.max(1, Math.min(road.points.length - 1, index))
    road.points.splice(safeIndex, 0, [x, y])
    selectedRoadId.value = road.id
    markDirty()
}

/** 双击控制点删除（保留首尾端点） */
function onRemoveWaypoint({roadId, index}) {
    const road = plantState.roads.find((item) => item.id === roadId)
    if (!road?.points?.length) return
    if (index <= 0 || index >= road.points.length - 1) return
    road.points.splice(index, 1)
    markDirty()
}

function startAdd() {
    adding.value = true
    nodeDrawer.value = false
    tab.value = 'node'
    Object.assign(draft, {
        name: '',
        code: '',
        type: 'junction',
        x: null,
        y: null,
    })
}

function cancelAdd() {
    adding.value = false
}

function onPickPoint(point) {
    if (!adding.value) return
    draft.x = point.x
    draft.y = point.y
}

function commitAdd() {
    if (!draft.name.trim()) {
        ElMessage.warning('请填写点位名称')
        return
    }
    if (draft.x === null || draft.y === null) {
        ElMessage.warning('请在地图上点击取点')
        return
    }

    const name = draft.name.trim()
    const code = draft.code.trim()
    // 新增前做重复校验：名称、编号均忽略大小写与首尾空格
    if (plantState.nodes.some((node) => (node.name || '').trim().toLowerCase() === name.toLowerCase())) {
        ElMessage.warning(`点位名称「${name}」已存在，请换个名称`)
        return
    }
    if (code && plantState.nodes.some((node) => (node.code || '').trim().toLowerCase() === code.toLowerCase())) {
        ElMessage.warning(`点位编号「${code}」已存在，请换个编号`)
        return
    }

    const id = `n_${Date.now().toString(36)}`
    plantState.nodes.push({
        id,
        code,
        name,
        type: draft.type,
        x: draft.x,
        y: draft.y,
        status: 1,
        description: '',
        sortNo: plantState.nodes.length + 1,
        visible: true,
    })

    if (draft.type === 'scale') {
        plantState.scales.push({
            id,
            code,
            name,
            nodeId: id,
            area: '',
            capacity: '',
            status: 1,
            sortNo: plantState.scales.length + 1,
        })
    }

    selectedId.value = id
    adding.value = false
    dirty.value = true
    ElMessage.success(`已新增「${name}」，记得保存`)
}

async function removeNode(node) {
    const usedByRoad = plantState.roads.some((road) => road.from === node.id || road.to === node.id)
    const usedByMaterial = plantState.materials.some((material) => material.pointNodeId === node.id)
    if (usedByRoad || usedByMaterial) {
        ElMessage.warning('该点位已被道路或物资引用，请先删除相关道路/调整物资绑定')
        return
    }
    try {
        await ElMessageBox.confirm(`确定删除点位「${node.name}」吗？`, '删除点位', {type: 'warning'})
    } catch (e) {
        return
    }

    plantState.nodes = plantState.nodes.filter((item) => item.id !== node.id)
    plantState.scales = plantState.scales.filter((item) => item.nodeId !== node.id)
    if (selectedId.value === node.id) selectedId.value = ''
    nodeDrawer.value = false
    dirty.value = true
}

function addRoad() {
    const fromNode = plantState.nodes.find((node) => node.id === roadDraft.from)
    const toNode = plantState.nodes.find((node) => node.id === roadDraft.to)
    if (!fromNode || !toNode) {
        ElMessage.warning('请选择起点和终点')
        return
    }
    if (fromNode.id === toNode.id) {
        ElMessage.warning('起点和终点不能相同')
        return
    }

    const name = roadDraft.name.trim() || `${fromNode.name}-${toNode.name}`
    // 新增道路重复校验：同名，或同一对点位之间已存在道路（方向不同视为同一条）
    if (plantState.roads.some((road) => (road.name || '').trim().toLowerCase() === name.toLowerCase())) {
        ElMessage.warning(`道路名称「${name}」已存在，请换个名称`)
        return
    }
    const duplicated = plantState.roads.some((road) =>
        (road.from === fromNode.id && road.to === toNode.id)
        || (road.from === toNode.id && road.to === fromNode.id))
    if (duplicated) {
        ElMessage.warning(`「${fromNode.name}」与「${toNode.name}」之间已存在道路，请勿重复添加`)
        return
    }

    plantState.roads.push({
        id: `r_${Date.now().toString(36)}`,
        code: '',
        name,
        groupName: '新增道路',
        group: '新增道路',
        level: roadDraft.level,
        from: fromNode.id,
        to: toNode.id,
        points: [[fromNode.x, fromNode.y], [toNode.x, toNode.y]],
        oneWay: 0,
        enabled: 1,
        sortNo: plantState.roads.length + 1,
    })

    roadDraft.name = ''
    dirty.value = true
    ElMessage.success('已新增道路，记得保存')
}

async function removeRoad(road) {
    try {
        await ElMessageBox.confirm(`确定删除道路「${road.name}」吗？`, '删除道路', {type: 'warning'})
    } catch (e) {
        return
    }
    plantState.roads = plantState.roads.filter((item) => item.id !== road.id)
    if (selectedRoadId.value === road.id) selectedRoadId.value = ''
    dirty.value = true
}

function toggleRoad(roadId, persist) {
    toggleRoadState(roadId)
    if (persist) {
        ElMessage.success('道路状态已更新并写入数据库')
        dirty.value = false
    }
}

/** 地图道路管控模式：点击道路切换封路/恢复（即时写库） */
function onToggleRoad(roadId) {
    const road = plantState.roads.find((item) => item.id === roadId)
    const willClose = road?.enabled !== 0
    toggleRoad(roadId, true)
    if (willClose) ElMessage.warning(`已封闭「${road?.name || roadId}」`)
    else ElMessage.success(`已恢复「${road?.name || roadId}」通行`)
}

async function save() {
    try {
        await saveConfig()
        dirty.value = false
        ElMessage.success('已保存到 nav 数据库')
    } catch (e) {
        ElMessage.error(plantState.saveError || '保存失败，请检查后端服务')
    }
}

async function reload() {
    await loadConfig(true)
    dirty.value = false
    if (plantState.loadError) ElMessage.error('后端配置不可用，当前为内置默认配置')
    else ElMessage.success('已从数据库重新加载')
}

onMounted(loadConfig)
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.toolbar {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 16px;
    background: #ffffff;
    border: 1px solid #e3eaf3;
    border-radius: 12px;
    box-shadow: 0 4px 16px rgba(31, 58, 95, .05);
}

.toolbar-main {
    flex: 1;
}

.toolbar-title {
    font-size: 15px;
    font-weight: 700;
    color: #1e293b;
}

.toolbar-sub {
    margin-top: 2px;
    font-size: 12px;
    color: #94a3b8;
}

.dirty-tag,
.error-tag {
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 11px;
}

.dirty-tag {
    color: #b45309;
    background: #fff7e6;
}

.error-tag {
    color: #b91c1c;
    background: #fef2f2;
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
    align-items: baseline;
    gap: 12px;
    padding: 12px 16px;
    border-bottom: 1px solid #edf2f8;
}

/* 道路管控开关：贴地图卡片右上角 */
.map-road-switch {
    margin-left: auto;
    align-self: center;
    flex: 0 0 auto;
}

.map-title {
    font-size: 15px;
    font-weight: 700;
    color: #1e293b;
}

.map-sub {
    font-size: 12px;
    color: #8fa0b8;
}

.side {
    flex: 0 0 400px;
    width: 400px;
    max-height: calc(100vh - 170px);
    overflow-y: auto;
    background: #ffffff;
    border: 1px solid #e3eaf3;
    border-radius: 12px;
    padding: 4px 14px 14px;
    box-shadow: 0 4px 16px rgba(31, 58, 95, .05);
}

.tabs {
    margin-top: 4px;
}

.pane {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.pane-actions {
    display: flex;
    justify-content: flex-end;
}

.add-form {
    padding: 10px 12px;
    border: 1px dashed #cfe0f4;
    border-radius: 9px;
    background: #f9fcff;
}

.add-title {
    margin-bottom: 8px;
    font-size: 13px;
    font-weight: 700;
    color: #1e293b;
}

.add-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
}

.coord-text {
    font-size: 12px;
    color: #64748b;
}

.edit-hint {
    font-size: 11px;
    line-height: 1.6;
    color: #8fa0b8;
}

.road-hint {
    padding: 6px 8px;
    border-radius: 7px;
    background: #f6f2ff;
    color: #7c6bb0;
}

.list {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.row {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 7px 8px;
    border: 1px solid #eef3f9;
    border-radius: 8px;
    cursor: pointer;
    transition: background .12s ease, border-color .12s ease;
}

.row:hover {
    background: #f8fbff;
}

.row.active {
    border-color: #1677ff;
    background: #f2f8ff;
}

.dot {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    flex-shrink: 0;
}

.row-main {
    flex: 1;
    min-width: 0;
}

.row-title {
    font-size: 13px;
    font-weight: 600;
    color: #1e293b;
}

.row-code {
    margin-left: 4px;
    padding: 0 4px;
    border-radius: 4px;
    background: #eef4fc;
    color: #5b7ba6;
    font-size: 10px;
}

.row-hidden {
    margin-left: 4px;
    padding: 0 5px;
    border-radius: 4px;
    background: #f1f5f9;
    color: #94a3b8;
    font-size: 10px;
}

.visible-row {
    display: flex;
    align-items: center;
    gap: 8px;
}

.visible-tip {
    font-size: 11px;
    color: #94a3b8;
}

.row-sub {
    margin-top: 1px;
    font-size: 11px;
    color: #94a3b8;
}

.size-row {
    display: flex;
    align-items: center;
    gap: 6px;
}

.size-x {
    color: #94a3b8;
}

.nudge {
    display: flex;
    align-items: center;
    gap: 4px;
}

.nudge-btn {
    width: 26px;
    height: 26px;
    border: 1px solid #d7e3f2;
    border-radius: 6px;
    background: #ffffff;
    color: #475569;
    font-size: 13px;
    line-height: 1;
    cursor: pointer;
}

.nudge-btn:hover {
    border-color: #8b5cf6;
    color: #7c3aed;
}

.nudge-tip {
    margin-left: 6px;
    font-size: 11px;
    color: #94a3b8;
}

.base-tip {
    font-size: 12px;
    color: #94a3b8;
    line-height: 1.7;
}

.drawer-form {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 0 4px;
}

.drawer-actions {
    display: flex;
    justify-content: space-between;
    gap: 10px;
    padding-top: 6px;
    border-top: 1px dashed #e6edf6;
}

.drawer-empty {
    padding: 40px 0;
    text-align: center;
    color: #94a3b8;
    font-size: 12px;
}

@media (max-width: 1180px) {
    .workspace {
        flex-direction: column;
    }

    .side {
        width: 100%;
        flex: 1 1 auto;
        max-height: none;
    }
}
</style>
