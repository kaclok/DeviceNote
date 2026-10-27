import {computed, reactive} from 'vue'
import defaultConfig from '../data/plant.json'
import {axiosInst} from '@/framework/services/net/AxiosInst.js'

const PLANT_ID = 'default'

// 点位/磅房状态：1 正常、2 检修、3 停用
export const STATUS_TEXT = {1: '正常', 2: '检修', 3: '停用'}
export const DIRECTION_TEXT = {out: '拉货', in: '送货'}
export const TYPE_LABEL = {gate: '大门', junction: '路口', scale: '磅房', parking: '停车场', pickup: '拉货点', delivery: '送货点'}
export const TYPE_COLOR = {gate: '#1c4785', scale: '#1677ff', parking: '#8b5cf6', pickup: '#00a870', delivery: '#ff8f1f'}

// nav 库数值编码 ↔ 前端字符串枚举（编码唯一口径见 backend/db/nav-numeric-types.sql 列注释）。
// 前端内部一律使用字符串枚举，仅在 normalize（读）/ toPayload（写）边界做转换。
export const NODE_TYPE_CODE = {gate: 1, junction: 2, scale: 3, parking: 4, pickup: 5, delivery: 6}
export const ROAD_LEVEL_CODE = {main: 1, secondary: 2, branch: 3}
export const GATE_MODE_CODE = {both: 1, in: 2, out: 3, none: 4}
export const MATERIAL_DIRECTION_CODE = {in: 1, out: 2}
const NODE_TYPE_BY_CODE = invertCode(NODE_TYPE_CODE)
const ROAD_LEVEL_BY_CODE = invertCode(ROAD_LEVEL_CODE)
const GATE_MODE_BY_CODE = invertCode(GATE_MODE_CODE)
const MATERIAL_DIRECTION_BY_CODE = invertCode(MATERIAL_DIRECTION_CODE)

function invertCode(map) {
    return Object.fromEntries(Object.entries(map).map(([k, v]) => [v, k]))
}

/** 数值编码 → 字符串枚举；已是字符串或未知值原样透传 */
function fromCode(byCode, value) {
    return byCode[value] ?? value
}

/** 字符串枚举 → 数值编码；未知/空值返回 undefined，由调用方的默认编码兜底 */
function toCode(codeMap, value) {
    return codeMap[value]
}

function clone(value) {
    return JSON.parse(JSON.stringify(value))
}

function parsePoints(points) {
    if (Array.isArray(points)) return points
    if (typeof points !== 'string' || !points.trim()) return []
    try {
        const parsed = JSON.parse(points)
        return Array.isArray(parsed) ? parsed : []
    } catch (e) {
        return []
    }
}

function parseIds(ids) {
    if (Array.isArray(ids)) return ids
    if (typeof ids !== 'string' || !ids.trim()) return []
    try {
        const parsed = JSON.parse(ids)
        return Array.isArray(parsed) ? parsed : []
    } catch (e) {
        return []
    }
}

function normalize(raw) {
    const plant = raw?.plant || defaultConfig.plant
    return {
        plant: {
            ...plant,
            imageWidth: Number(plant.imageWidth) || 1280,
            imageHeight: Number(plant.imageHeight) || 558,
            metersPerPixel: Number(plant.metersPerPixel) || 1,
            dutyUrl: plant.dutyUrl || '',
            vehicleWeighCount: Number(plant.vehicleWeighCount) || 6,
            scaleLogCount: Number(plant.scaleLogCount) || 50,
        },
        nodes: (raw?.nodes || []).map((node) => ({
            ...node,
            x: Number(node.x),
            y: Number(node.y),
            type: fromCode(NODE_TYPE_BY_CODE, node.type),
            status: Number(node.status) || 1,
            description: node.description ?? node.desc ?? '',
            sortNo: Number(node.sortNo) || 0,
            visible: node.visible !== false && node.visible !== 0 && node.visible !== '0',
        })),
        roads: (raw?.roads || []).map((road) => ({
            ...road,
            from: road.from ?? road.fromNodeId ?? '',
            to: road.to ?? road.toNodeId ?? '',
            group: road.groupName ?? road.group ?? '',
            groupName: road.groupName ?? road.group ?? '',
            level: fromCode(ROAD_LEVEL_BY_CODE, road.level),
            points: parsePoints(road.points),
            oneWay: Number(road.oneWay) || 0,
            enabled: road.enabled === 0 || road.enabled === '0' ? 0 : 1,
            sortNo: Number(road.sortNo) || 0,
        })),
        scales: (raw?.scales || []).map((scale) => ({
            ...scale,
            status: Number(scale.status) || 1,
            sortNo: Number(scale.sortNo) || 0,
            gbNo: scale.gbNo == null ? '' : String(scale.gbNo),
        })),
        // 大门通行规则单独配置，与 type=gate 的点位一一对应
        gates: (raw?.gates || []).map((gate) => ({
            ...gate,
            gateMode: fromCode(GATE_MODE_BY_CODE, gate.gateMode) || 'both',
            allowedMaterialIds: parseIds(gate.allowedMaterialIds),
            sortNo: Number(gate.sortNo) || 0,
        })),
        materials: (raw?.materials || []).map((material) => ({
            ...material,
            direction: fromCode(MATERIAL_DIRECTION_BY_CODE, material.direction),
            backupScaleIds: parseIds(material.backupScaleIds),
            voiceText: material.voiceText || '',
            enabled: material.enabled === 0 || material.enabled === '0' ? 0 : 1,
            sortNo: Number(material.sortNo) || 0,
        })),
    }
}

export const plantState = reactive({
    ...normalize(clone(defaultConfig)),
    loaded: false,
    loadError: false,
    saving: false,
    saveError: '',
})

export const plantMeta = computed(() => plantState.plant)
export const nodeList = computed(() => plantState.nodes)
export const roadList = computed(() => plantState.roads)
export const scaleList = computed(() => plantState.scales)
export const materialList = computed(() => plantState.materials)
export const nodeMap = computed(() => new Map(plantState.nodes.map((node) => [node.id, node])))
export const scaleById = computed(() => new Map(plantState.scales.map((scale) => [scale.id, scale])))

let loadPromise = null

/** 从后端加载配置；失败时保留 plant.json 默认数据并置 loadError */
export function loadConfig(force = false) {
    if (!force && plantState.loaded) return Promise.resolve(!plantState.loadError)
    if (!force && loadPromise) return loadPromise
    loadPromise = doLoadConfig().finally(() => {
        loadPromise = null
    })
    return loadPromise
}

async function doLoadConfig() {
    try {
        // 加时间戳防缓存：浏览器会缓存同一个 GET 响应，导致外部改库后页面仍读到旧配置
        const res = await axiosInst.get('/nav/config', {params: {plantId: PLANT_ID, _t: Date.now()}})
        const data = res?.data?.data
        if (!data || !data.plant) throw new Error('后端未返回厂区配置')
        Object.assign(plantState, normalize(data), {loaded: true, loadError: false})
        return true
    } catch (e) {
        Object.assign(plantState, {loaded: true, loadError: true})
        return false
    }
}

function toPayload() {
    return {
        plant: {...plantState.plant},
        nodes: plantState.nodes.map((node) => ({
            id: node.id,
            plantId: PLANT_ID,
            code: node.code || '',
            name: node.name,
            type: toCode(NODE_TYPE_CODE, node.type),
            x: Number(node.x),
            y: Number(node.y),
            status: Number(node.status) || 1,
            description: node.description || node.desc || '',
            sortNo: node.sortNo || 0,
            visible: node.visible !== false,
        })),
        roads: plantState.roads.map((road) => ({
            id: road.id,
            plantId: PLANT_ID,
            code: road.code || '',
            name: road.name,
            groupName: road.groupName || road.group || '',
            level: toCode(ROAD_LEVEL_CODE, road.level) ?? 2,
            fromNodeId: road.from,
            toNodeId: road.to,
            points: JSON.stringify(road.points || []),
            oneWay: road.oneWay || 0,
            enabled: road.enabled === 0 ? 0 : 1,
            sortNo: road.sortNo || 0,
        })),
        scales: plantState.scales.map((scale) => ({
            id: scale.id,
            plantId: PLANT_ID,
            code: scale.code || '',
            name: scale.name,
            nodeId: scale.nodeId,
            area: scale.area || '',
            capacity: scale.capacity || '',
            status: Number(scale.status) || 1,
            sortNo: scale.sortNo || 0,
            // 库列 smallint：空串/非数字 → NULL（未绑定）
            gbNo: /^\d+$/.test(String(scale.gbNo ?? '').trim()) ? Number(String(scale.gbNo).trim()) : null,
        })),
        // 以 type=gate 的点位为准派生，保证大门与规则一一对应（删除点位时规则自动消失）
        gates: plantState.nodes
            .filter((node) => node.type === 'gate')
            .map((node, index) => {
                const gate = plantState.gates.find((item) => item.nodeId === node.id)
                return {
                    id: gate?.id || `gate_${node.id}`,
                    plantId: PLANT_ID,
                    nodeId: node.id,
                    gateMode: toCode(GATE_MODE_CODE, gate?.gateMode || 'both') ?? 1,
                    allowedMaterialIds: JSON.stringify(gate?.allowedMaterialIds || []),
                    sortNo: gate?.sortNo || index + 1,
                }
            }),
        materials: plantState.materials.map((material) => ({
            id: material.id,
            plantId: PLANT_ID,
            code: material.code || '',
            name: material.name,
            direction: toCode(MATERIAL_DIRECTION_CODE, material.direction) ?? 2,
            category: material.category || '',
            scaleId: material.scaleId,
            backupScaleIds: JSON.stringify(material.backupScaleIds || []),
            pointNodeId: material.pointNodeId,
            voiceText: material.voiceText || '',
            enabled: material.enabled === 0 ? 0 : 1,
            sortNo: material.sortNo || 0,
        })),
    }
}

/** 整体保存到后端；失败时抛出，由调用方提示 */
export async function saveConfig() {
    plantState.saving = true
    plantState.saveError = ''
    try {
        await axiosInst.post('/nav/config', toPayload())
        plantState.loadError = false
        return true
    } catch (e) {
        plantState.saveError = e?.data?.message || e?.message || '保存失败'
        throw e
    } finally {
        plantState.saving = false
    }
}

/** 状态/绑定等即时写库的调用方不关心失败细节，避免未处理的 Promise */
function persistSilently() {
    saveConfig().catch(() => undefined)
}

export function scaleStatusOf(scaleId) {
    return Number(scaleById.value.get(scaleId)?.status) || 1
}

/** 大门通行方向：both 双向 / in 仅进厂（禁止驶出）/ out 仅出厂（禁止驶入）/ none 禁行（不让进不让出） */
export function gateModeOf(nodeId) {
    return plantState.gates.find((gate) => gate.nodeId === nodeId)?.gateMode || 'both'
}

/** 修改大门通行方向并即时写库 */
export function setGateMode(nodeId, gateMode) {
    const node = nodeMap.value.get(nodeId)
    if (!node || node.type !== 'gate') return Promise.resolve()
    const gate = plantState.gates.find((item) => item.nodeId === nodeId)
    if (gate) gate.gateMode = gateMode
    else plantState.gates.push({
        id: `gate_${nodeId}`,
        plantId: PLANT_ID,
        nodeId,
        gateMode,
        sortNo: plantState.gates.length + 1,
    })
    persistSilently()
    return Promise.resolve()
}

/** 大门允许通行的物资 id；空数组表示不限物资 */
export function gateMaterialsOf(nodeId) {
    return plantState.gates.find((gate) => gate.nodeId === nodeId)?.allowedMaterialIds || []
}

/** 修改大门允许通行的物资并即时写库 */
export function setGateMaterials(nodeId, materialIds) {
    const node = nodeMap.value.get(nodeId)
    if (!node || node.type !== 'gate') return Promise.resolve()
    const ids = [...new Set((materialIds || []).filter(Boolean))]
    const gate = plantState.gates.find((item) => item.nodeId === nodeId)
    if (gate) gate.allowedMaterialIds = ids
    else plantState.gates.push({
        id: `gate_${nodeId}`,
        plantId: PLANT_ID,
        nodeId,
        gateMode: 'both',
        allowedMaterialIds: ids,
        sortNo: plantState.gates.length + 1,
    })
    persistSilently()
    return Promise.resolve()
}

/** 新增物资并写库；返回的 Promise 失败时抛出，由调用方提示 */
export function addMaterial(draft) {
    const material = {
        id: draft.id || `m_${Date.now().toString(36)}`,
        plantId: PLANT_ID,
        code: draft.code || '',
        name: draft.name,
        direction: draft.direction || 'out',
        category: draft.category || '',
        scaleId: draft.scaleId,
        backupScaleIds: draft.backupScaleIds || [],
        pointNodeId: draft.pointNodeId,
        voiceText: draft.voiceText || '',
        enabled: 1,
        sortNo: plantState.materials.length + 1,
    }
    plantState.materials.push(material)
    return saveConfig()
}

/** 修改物资语音介绍并写库；失败时由调用方提示 */
export function setMaterialVoice(materialId, voiceText) {
    const material = plantState.materials.find((item) => item.id === materialId)
    if (!material) return Promise.resolve()
    material.voiceText = (voiceText || '').trim()
    return saveConfig()
}

/** 设置值班人员网页链接（存于 plant.dutyUrl）并写库；失败时由调用方提示 */
export function setDutyUrl(url) {
    plantState.plant.dutyUrl = (url || '').trim()
    return saveConfig()
}

/** 物资可称重的全部磅房，第一项为主磅房 */
export function boundScaleIds(material) {
    const ids = [material?.scaleId, ...(material?.backupScaleIds || [])].filter(Boolean)
    return [...new Set(ids)]
}

export function boundScales(material) {
    return boundScaleIds(material)
        .map((id) => scaleById.value.get(id))
        .filter(Boolean)
}

export function boundScaleId(material) {
    return material?.scaleId || ''
}

export function boundScale(material) {
    return scaleById.value.get(boundScaleId(material))
}

/** 建议的备用磅：优先可称重列表中的正常磅房，其次任意正常磅房 */
export function suggestedScaleFor(material) {
    if (!material) return null
    for (const id of boundScaleIds(material)) {
        if (id !== material.scaleId && scaleStatusOf(id) === 1) return scaleById.value.get(id)
    }
    return plantState.scales.find((scale) => scale.id !== material.scaleId && scale.status === 1) || null
}

export function setScaleStatus(scaleId, status) {
    const scale = scaleById.value.get(scaleId)
    if (!scale) return Promise.resolve()
    scale.status = status
    const node = nodeMap.value.get(scale.nodeId)
    if (node) node.status = status
    persistSilently()
    return Promise.resolve()
}

export function setBinding(materialId, scaleId) {
    const material = plantState.materials.find((item) => item.id === materialId)
    if (!material || !scaleId) return Promise.resolve()
    // 提升为主磅房，同时保留其余可称重磅房
    const ids = [scaleId, ...boundScaleIds(material).filter((id) => id !== scaleId)]
    return setBindingScales(materialId, ids)
}

/** 设置物资可称重的磅房集合，第一座为主磅房 */
export function setBindingScales(materialId, scaleIds) {
    const material = plantState.materials.find((item) => item.id === materialId)
    if (!material) return Promise.resolve()
    const ids = [...new Set((scaleIds || []).filter(Boolean))]
    if (!ids.length) return Promise.resolve()
    material.scaleId = ids[0]
    material.backupScaleIds = ids.slice(1)
    persistSilently()
    return Promise.resolve()
}

export function toggleRoad(roadId) {
    const road = plantState.roads.find((item) => item.id === roadId)
    if (!road) return Promise.resolve()
    road.enabled = road.enabled === 0 ? 1 : 0
    persistSilently()
    return Promise.resolve()
}

export const poiNodes = computed(() => plantState.nodes
    .filter((node) => node.type !== 'junction' && node.visible !== false)
    .map((node) => {
        if (node.type !== 'scale') return node
        const scale = plantState.scales.find((item) => item.nodeId === node.id)
        return {
            ...node,
            scaleId: scale?.id,
            scaleCode: scale?.code,
            area: scale?.area,
            status: scale ? scale.status : node.status,
        }
    }))

export const poiGroups = computed(() => ['gate', 'scale', 'parking', 'pickup', 'delivery']
    .map((type) => ({type, label: TYPE_LABEL[type], items: poiNodes.value.filter((node) => node.type === type)}))
    .filter((group) => group.items.length))

export const roadsWithState = computed(() => plantState.roads.map((road) => ({
    ...road,
    closed: road.enabled === 0,
})))

export const closedRoadList = computed(() => plantState.roads.filter((road) => road.enabled === 0))
