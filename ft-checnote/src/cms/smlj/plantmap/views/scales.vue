<template>
    <div class="page">
        <section class="panel">
            <div class="panel-head duty-head">
                <div class="panel-head-main">
                    <div class="panel-title-row">
                        <span class="panel-title">值班人员</span>
                    </div>
                    <div class="panel-sub">配置值班查询网页链接，保存后全站右上角的「地磅值班」按钮将跳转到该页面</div>
                </div>
                <div class="panel-tools duty-tools">
                    <el-input v-model="dutyUrlDraft" size="small" class="duty-input" clearable
                              placeholder="如 https://…/duty（值班表网页地址）"/>
                    <el-button size="small" type="primary" :loading="dutySaving" @click="saveDutyUrl">保存链接
                    </el-button>
                </div>
            </div>
        </section>

        <section class="panel">
            <div class="panel-head duty-head">
                <div class="panel-head-main">
                    <div class="panel-title-row">
                        <span class="panel-title">地磅查询条数</span>
                    </div>
                    <div class="panel-sub">配置「地磅查询」页两类查询各自返回的记录条数，保存后立即生效</div>
                </div>
                <div class="panel-tools duty-tools limits-tools">
                    <label class="limit-item">
                        <span class="limit-label">车牌过磅记录</span>
                        <el-input-number v-model="vehicleWeighDraft" size="small" :min="1" :max="100"
                                         :step="1" step-strictly controls-position="right" class="limit-input"/>
                        <span class="limit-unit">条</span>
                    </label>
                    <label class="limit-item">
                        <span class="limit-label">磅房过磅日志</span>
                        <el-input-number v-model="scaleLogDraft" size="small" :min="1" :max="200"
                                         :step="1" step-strictly controls-position="right" class="limit-input"/>
                        <span class="limit-unit">条</span>
                    </label>
                    <el-button size="small" type="primary" :loading="limitsSaving" @click="saveQueryLimits">
                        保存条数
                    </el-button>
                </div>
            </div>
        </section>

        <section class="panel">
            <div class="panel-head">
                <div class="panel-head-main">
                    <div class="panel-title-row">
                        <span class="panel-title">磅房状态</span>
                        <span class="count-chip">{{ scaleCards.length }} 座磅房</span>
                    </div>
                    <div class="panel-sub">标记检修或停用后，寻路页会把该磅房从可选目的地中移除</div>
                </div>
                <div class="panel-tools">
                    <el-select v-model="statusFilter" size="small" class="status-filter">
                        <el-option v-for="item in STATUS_OPTIONS" :key="item.value"
                                   :label="item.label" :value="item.value"/>
                    </el-select>
                    <el-button size="small" plain @click="reload">重新加载配置</el-button>
                </div>
            </div>

            <div class="scale-grid">
                <div v-for="scale in scaleCards" :key="scale.id" class="scale-card" :class="`s-${scale.status}`">
                    <div class="scale-head">
                        <div class="scale-badge">磅</div>
                        <div class="scale-title-wrap">
                            <div class="scale-title">{{ scale.name }}</div>
                            <div class="scale-sub">{{ scale.code }} · {{ scale.area || '未设置区域' }}</div>
                        </div>
                        <span class="status-pill" :class="`st-${scale.status}`">
                            <i class="pill-dot"></i>{{ STATUS_TEXT[scale.status] }}
                        </span>
                    </div>

                    <div class="scale-metrics">
                        <div class="metric">
                            <b>{{ scale.capacity || '-' }}</b>
                            <span>限重</span>
                        </div>
                    </div>

                    <div v-if="scale.status !== 1" class="scale-warning">
                        该磅房{{ STATUS_TEXT[scale.status] }}，{{ scale.materials.length }} 种物资将自动切换到其他可称重磅房
                    </div>

                    <div class="scale-bottom">
                        <span class="products-label">绑定物资</span>
                        <div class="scale-products">
                            <span v-for="name in scale.materials" :key="name" class="product-chip">{{ name }}</span>
                            <span v-if="!scale.materials.length" class="scale-empty">暂无</span>
                        </div>
                        <el-dropdown trigger="click" class="status-dropdown" placement="bottom-end"
                                     @command="(value) => changeStatus(scale, value)">
                            <el-button size="small" plain class="status-btn">状态<span class="caret">▾</span></el-button>
                            <template #dropdown>
                                <el-dropdown-menu>
                                    <el-dropdown-item v-for="option in statusActions(scale.status)" :key="option.value"
                                                      :command="option.value">
                                        {{ option.label }}
                                    </el-dropdown-item>
                                </el-dropdown-menu>
                            </template>
                        </el-dropdown>
                    </div>
                </div>
            </div>

            <div v-if="!scaleCards.length" class="scale-empty-block">
                当前状态下暂无磅房
            </div>
        </section>

        <section class="panel">
            <div class="panel-head">
                <div class="panel-head-main">
                    <div class="panel-title-row">
                        <span class="panel-title">物资与磅房绑定</span>
                        <span class="count-chip">{{ bindingRows.length }} 种物资</span>
                    </div>
                    <div class="panel-sub">一座磅房可称多种物资，一种物资也可绑定多座磅房；第一座为主磅房，仅可绑定正常使用的磅房</div>
                </div>
                <div class="panel-tools">
                    <el-button size="small" type="primary" @click="openMaterialDialog">新增物资</el-button>
                </div>
            </div>

            <el-table :data="bindingRows" size="small" class="data-table" border>
                <el-table-column prop="name" label="物资" width="100" show-overflow-tooltip/>
                <el-table-column label="语音介绍" min-width="240">
                    <template #default="{ row }">
                        <div class="voice-cell" @click="openVoiceDialog(row)">
                            <span class="voice-text" :class="{empty: !row.voiceText}">
                                {{ row.voiceText || '未配置，点击添加' }}
                            </span>
                            <el-button link type="primary" size="small" @click.stop="openVoiceDialog(row)">
                                编辑
                            </el-button>
                        </div>
                    </template>
                </el-table-column>
                <el-table-column label="流向" width="120" class-name="nowrap-col">
                    <template #default="{ row }">
                        <span class="dir-tag" :class="row.direction === 'out' ? 'dir-out' : 'dir-in'">
                            {{ row.directionText }}
                        </span>
                    </template>
                </el-table-column>
                <el-table-column prop="category" label="类别" width="110" show-overflow-tooltip/>
                <el-table-column label="主磅房" width="140">
                    <template #default="{ row }">
                        <span :class="row.status === 1 ? 'bind-name' : 'bind-name bad'">{{ row.scaleName }}</span>
                        <span v-if="row.status !== 1" class="mini-bad">{{ STATUS_TEXT[row.status] }}</span>
                    </template>
                </el-table-column>
                <el-table-column label="可称重磅房" min-width="300">
                    <template #default="{ row }">
                        <el-popover placement="bottom-start" trigger="click" :width="240"
                                    popper-class="bind-popper">
                            <template #reference>
                                <div class="bind-field">
                                    <template v-for="(scale, index) in row.usableScales" :key="scale.id">
                                        <span class="bind-chip">
                                            {{ scale.name }}
                                            <button type="button" class="chip-x" title="取消勾选"
                                                    @click.stop="removeScale(row, scale.id)">×
                                            </button>
                                        </span>
                                        <span v-if="index < row.usableScales.length - 1" class="chip-sep">,</span>
                                    </template>
                                    <span v-if="!row.usableScales.length" class="bind-placeholder">选择可称重磅房</span>
                                    <span class="bind-caret">▾</span>
                                </div>
                            </template>
                            <div class="bind-options">
                                <el-checkbox v-for="scale in normalScales" :key="scale.id"
                                             :model-value="row.scaleIds.includes(scale.id)"
                                             @change="() => toggleScale(row, scale.id)">
                                    {{ scale.name }} · {{ scale.area }}
                                </el-checkbox>
                            </div>
                        </el-popover>
                    </template>
                </el-table-column>
                <el-table-column label="绑定状态" width="100">
                    <template #default="{ row }">
                        <span class="bind-state" :class="row.stateClass">{{ row.stateText }}</span>
                    </template>
                </el-table-column>
            </el-table>

            <div class="table-tip">
                说明：可称重磅房按顺序排列，第一座为主磅；主磅检修或停用时，寻路自动改用列表中的下一座正常磅房。检修、停用的磅房不展示也不可选。
            </div>
        </section>

        <section class="panel">
            <div class="panel-head">
                <div class="panel-head-main">
                    <div class="panel-title-row">
                        <span class="panel-title">大门通行方向</span>
                        <span class="count-chip">{{ gateCards.length }} 座大门</span>
                    </div>
                    <div class="panel-sub">仅进厂的大门不能作为终点驶出，仅出厂的大门不能作为起点驶入，禁行的大门不允许任何车辆进出；修改后立即写入数据库</div>
                </div>
            </div>

            <div class="gate-grid">
                <div v-for="gate in gateCards" :key="gate.id" class="gate-card" :class="`g-${gate.gateMode}`">
                    <div class="gate-head">
                        <div class="gate-badge">门</div>
                        <div class="gate-title-wrap">
                            <div class="gate-title">{{ gate.name }}</div>
                            <div class="gate-sub">{{ gate.code || '未设置编码' }} · 通行方向</div>
                        </div>
                    </div>
                    <el-radio-group :model-value="gate.gateMode" size="small" class="gate-modes"
                                    @change="(value) => changeGateMode(gate, value)">
                        <el-radio-button v-for="item in GATE_MODE_OPTIONS" :key="item.value" :label="item.value">
                            {{ item.label }}
                        </el-radio-button>
                    </el-radio-group>
                    <div class="gate-materials">
                        <span class="gate-materials-label">允许物资</span>
                        <el-popover placement="bottom-start" trigger="click" :width="240"
                                    popper-class="bind-popper">
                            <template #reference>
                                <div class="bind-field gate-bind-field">
                                    <template v-for="(material, index) in gate.materials" :key="material.id">
                                        <span class="bind-chip">
                                            {{ material.name }}
                                            <button type="button" class="chip-x" title="取消允许"
                                                    @click.stop="removeGateMaterial(gate, material.id)">×
                                            </button>
                                        </span>
                                        <span v-if="index < gate.materials.length - 1" class="chip-sep">,</span>
                                    </template>
                                    <span v-if="!gate.materials.length" class="bind-placeholder">
                                        不限物资（所有车辆可通行）
                                    </span>
                                    <span class="bind-caret">▾</span>
                                </div>
                            </template>
                            <div class="bind-options">
                                <el-checkbox v-for="material in materialList" :key="material.id"
                                             :model-value="gate.allowedMaterialIds.includes(material.id)"
                                             @change="() => toggleGateMaterial(gate, material.id)">
                                    {{ material.name }}
                                </el-checkbox>
                            </div>
                        </el-popover>
                    </div>
                </div>
            </div>

            <div v-if="!gateCards.length" class="scale-empty-block">
                暂无大门点位，可在地图配置中添加类型为「大门」的点位
            </div>
        </section>

        <el-dialog v-model="materialDialog" title="新增物资" width="min(460px, 92vw)" append-to-body
                   class="material-dialog">
            <el-form label-width="92px" size="small">
                <el-form-item label="物资名称">
                    <el-input v-model="materialDraft.name" placeholder="如 硫酸"/>
                </el-form-item>
                <el-form-item label="物资编号">
                    <el-input v-model="materialDraft.code" placeholder="如 WZ07"/>
                </el-form-item>
                <el-form-item label="流向">
                    <el-select v-model="materialDraft.direction" style="width: 100%">
                        <el-option label="拉货（出厂）" value="out"/>
                        <el-option label="送货（入厂）" value="in"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="类别">
                    <el-input v-model="materialDraft.category" placeholder="如 酸类"/>
                </el-form-item>
                <el-form-item label="主磅房">
                    <el-select v-model="materialDraft.scaleId" style="width: 100%" placeholder="选择正常使用的磅房">
                        <el-option v-for="scale in normalScales" :key="scale.id"
                                   :label="`${scale.name}${scale.area ? ' · ' + scale.area : ''}`" :value="scale.id"/>
                    </el-select>
                </el-form-item>
                <el-form-item label="取/送货点">
                    <el-select v-model="materialDraft.pointNodeId" filterable style="width: 100%" placeholder="选择取货或送货点">
                        <el-option-group v-for="group in pointGroups" :key="group.type" :label="group.label">
                            <el-option v-for="node in group.items" :key="node.id" :label="node.name" :value="node.id"/>
                        </el-option-group>
                    </el-select>
                </el-form-item>
                <el-form-item label="语音介绍">
                    <div class="voice-input">
                        <el-input v-model="materialDraft.voiceText" type="textarea" :rows="3" maxlength="500"
                                  show-word-limit placeholder="司机点喇叭时朗读，例如：PVC，学名聚氯乙烯，俗称吨包料"/>
                        <el-button size="small" class="voice-preview" @click="previewDraftVoice">试听</el-button>
                    </div>
                </el-form-item>
            </el-form>
            <template #footer>
                <el-button size="small" @click="materialDialog = false">取消</el-button>
                <el-button size="small" type="primary" :loading="materialSaving" @click="submitMaterial">保存</el-button>
            </template>
        </el-dialog>

        <el-dialog v-model="voiceDialog" :title="`物资语音介绍 · ${voiceDraft.name}`"
                   width="min(520px, 92vw)" append-to-body>
            <div class="voice-tip">
                司机在寻路页点「语音」即可听到这段介绍。建议口语化，把俗称、学名都讲清楚，例如 PVC 与吨包料、聚氯乙烯。
            </div>
            <el-input v-model="voiceDraft.text" type="textarea" :rows="5" maxlength="500" show-word-limit
                      placeholder="例如：PVC，学名聚氯乙烯，司机常说的吨包料就是它，也常被叫作树脂粉。"/>
            <template #footer>
                <div class="voice-footer">
                    <el-button size="small" @click="previewVoice">试听</el-button>
                    <div class="voice-footer-right">
                        <el-button size="small" @click="voiceDialog = false">取消</el-button>
                        <el-button size="small" type="primary" :loading="voiceSaving" @click="saveVoice">
                            保存
                        </el-button>
                    </div>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<script setup lang="js">
import {computed, onMounted, reactive, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {
    DIRECTION_TEXT,
    STATUS_TEXT,
    addMaterial,
    boundScaleIds,
    gateMaterialsOf,
    gateModeOf,
    loadConfig,
    materialList,
    nodeList,
    plantState,
    saveConfig,
    scaleById,
    scaleList,
    scaleStatusOf,
    setBindingScales,
    setDutyUrl,
    setGateMaterials,
    setGateMode,
    setMaterialVoice,
    setScaleStatus,
} from '../system/PlantStore.js'
import {speak} from '../utils/SpeechX.js'

const GATE_MODE_OPTIONS = [
    {value: 'both', label: '双向'},
    {value: 'in', label: '仅进'},
    {value: 'out', label: '仅出'},
    {value: 'none', label: '禁行'},
]

// 0 表示全部：按 正常 → 检修 → 停用 分组排序展示
const STATUS_OPTIONS = [
    {value: 0, label: '全部'},
    {value: 1, label: '正常'},
    {value: 2, label: '检修'},
    {value: 3, label: '停用'},
]

const STATUS_ACTIONS = {
    1: [{value: 2, label: '检修'}, {value: 3, label: '停用'}],
    2: [{value: 1, label: '正常'}, {value: 3, label: '停用'}],
    3: [{value: 1, label: '正常'}, {value: 2, label: '检修'}],
}

const statusFilter = ref(1)

// 值班人员网页链接配置（存于 plant.dutyUrl）
const dutyUrlDraft = ref('')
const dutySaving = ref(false)

async function saveDutyUrl() {
    const url = (dutyUrlDraft.value || '').trim()
    if (url && !/^https?:\/\//i.test(url)) {
        ElMessage.warning('链接需以 http:// 或 https:// 开头')
        return
    }
    dutySaving.value = true
    try {
        await setDutyUrl(url)
        ElMessage.success('值班人员链接已保存')
    } catch (e) {
        ElMessage.error(plantState.saveError || '保存失败，请检查后端服务')
    } finally {
        dutySaving.value = false
    }
}

// 地磅查询条数配置（存于 nav.plant.vehicle_weigh_count / scale_log_count）
const vehicleWeighDraft = ref(6)
const scaleLogDraft = ref(50)
const limitsSaving = ref(false)

async function saveQueryLimits() {
    const vehicleCount = Number(vehicleWeighDraft.value) || 6
    const logCount = Number(scaleLogDraft.value) || 50
    limitsSaving.value = true
    try {
        plantState.plant.vehicleWeighCount = vehicleCount
        plantState.plant.scaleLogCount = logCount
        await saveConfig()
        ElMessage.success(`查询条数已保存：车牌记录 ${vehicleCount} 条、磅房日志 ${logCount} 条`)
    } catch (e) {
        ElMessage.error(plantState.saveError || '保存失败，请检查后端服务')
    } finally {
        limitsSaving.value = false
    }
}

function statusActions(status) {
    return STATUS_ACTIONS[status] || []
}

const scaleCards = computed(() => scaleList.value
    .map((scale) => ({
        ...scale,
        status: scaleStatusOf(scale.id),
        materials: materialList.value
            .filter((material) => boundScaleIds(material).includes(scale.id))
            .map((material) => material.name),
    }))
    .filter((scale) => statusFilter.value === 0 || scale.status === statusFilter.value)
    .sort((a, b) => (a.status - b.status) || ((a.sortNo || 0) - (b.sortNo || 0))))

const normalScales = computed(() => scaleList.value.filter((scale) => scaleStatusOf(scale.id) === 1))

// 大门点位及其通行方向，规则存于 nav.gate，与磅房状态同样即时写库
const gateCards = computed(() => nodeList.value
    .filter((node) => node.type === 'gate')
    .map((node) => {
        const allowedMaterialIds = gateMaterialsOf(node.id)
        return {
            ...node,
            gateMode: gateModeOf(node.id),
            allowedMaterialIds,
            materials: allowedMaterialIds
                .map((id) => materialList.value.find((item) => item.id === id))
                .filter(Boolean),
        }
    })
    .sort((a, b) => (a.sortNo || 0) - (b.sortNo || 0)))

const materialDialog = ref(false)
const materialSaving = ref(false)
const materialDraft = reactive({
    name: '', code: '', direction: 'out', category: '', scaleId: '', pointNodeId: '', voiceText: '',
})

// 物资语音介绍：司机页点喇叭时用手机 TTS 朗读
const voiceDialog = ref(false)
const voiceSaving = ref(false)
const voiceDraft = reactive({id: '', name: '', text: ''})

const pointGroups = computed(() => [
    {type: 'pickup', label: '拉货点', items: nodeList.value.filter((node) => node.type === 'pickup')},
    {type: 'delivery', label: '送货点', items: nodeList.value.filter((node) => node.type === 'delivery')},
].filter((group) => group.items.length))

const bindingRows = computed(() => materialList.value.map((material) => {
    const allIds = boundScaleIds(material)
    const visibleIds = allIds.filter((id) => scaleStatusOf(id) === 1)
    const hiddenIds = allIds.filter((id) => scaleStatusOf(id) !== 1)
    const primary = scaleById.value.get(material.scaleId)
    const primaryStatus = scaleStatusOf(material.scaleId)
    const usable = visibleIds.length > 0
    return {
        id: material.id,
        name: material.name,
        direction: material.direction,
        directionText: DIRECTION_TEXT[material.direction] || '-',
        category: material.category || '-',
        voiceText: material.voiceText || '',
        scaleIds: visibleIds,
        hiddenIds,
        usableScales: visibleIds.map((id) => scaleById.value.get(id)).filter(Boolean),
        scaleName: primary?.name || '-',
        status: primaryStatus,
        stateText: primaryStatus === 1 ? '正常' : (usable ? '自动切换' : '需切换'),
        stateClass: primaryStatus === 1 || usable ? 'state-ok' : 'state-bad',
    }
}))

function applyBinding(row, ids) {
    if (!ids.length) {
        ElMessage.warning('至少保留一座可称重磅房')
        return
    }
    // 检修/停用的磅房不展示，但保留在配置中，恢复正常后自动重新可用
    const allIds = [...ids, ...row.hiddenIds.filter((id) => !ids.includes(id))]
    setBindingScales(row.id, allIds)
    const names = ids.map((id) => scaleById.value.get(id)?.name || id).join(', ')
    ElMessage.success(`「${row.name}」可称重磅房：${names}`)
}

function toggleScale(row, scaleId) {
    const ids = row.scaleIds.includes(scaleId)
        ? row.scaleIds.filter((id) => id !== scaleId)
        : [...row.scaleIds, scaleId]
    applyBinding(row, ids)
}

function removeScale(row, scaleId) {
    applyBinding(row, row.scaleIds.filter((id) => id !== scaleId))
}

function changeStatus(scale, status) {
    setScaleStatus(scale.id, status)
    const text = STATUS_TEXT[status]
    ElMessage.success(status === 1 ? `${scale.name} 已恢复使用` : `${scale.name} 已标记为${text}`)
}

function changeGateMode(gate, gateMode) {
    setGateMode(gate.id, gateMode)
    const text = GATE_MODE_OPTIONS.find((item) => item.value === gateMode)?.label || ''
    ElMessage.success(`${gate.name} 通行方向已设为${text}`)
}

function applyGateMaterials(gate, materialIds) {
    setGateMaterials(gate.id, materialIds)
    const names = (materialIds || [])
        .map((id) => materialList.value.find((item) => item.id === id)?.name || id)
    ElMessage.success(names.length
        ? `${gate.name} 仅允许：${names.join('、')}`
        : `${gate.name} 已设为不限物资`)
}

function toggleGateMaterial(gate, materialId) {
    const ids = gate.allowedMaterialIds.includes(materialId)
        ? gate.allowedMaterialIds.filter((id) => id !== materialId)
        : [...gate.allowedMaterialIds, materialId]
    applyGateMaterials(gate, ids)
}

function removeGateMaterial(gate, materialId) {
    applyGateMaterials(gate, gate.allowedMaterialIds.filter((id) => id !== materialId))
}

function openMaterialDialog() {
    Object.assign(materialDraft, {
        name: '',
        code: '',
        direction: 'out',
        category: '',
        scaleId: normalScales.value[0]?.id || '',
        pointNodeId: '',
        voiceText: '',
    })
    materialDialog.value = true
}

function previewDraftVoice() {
    const text = (materialDraft.voiceText || '').trim()
    if (!text) {
        ElMessage.warning('请先填写语音介绍文案')
        return
    }
    if (!speak(text)) ElMessage.warning('当前浏览器不支持语音朗读')
}

function openVoiceDialog(row) {
    Object.assign(voiceDraft, {id: row.id, name: row.name, text: row.voiceText || ''})
    voiceDialog.value = true
}

function previewVoice() {
    const text = (voiceDraft.text || '').trim()
    if (!text) {
        ElMessage.warning('请先填写语音介绍文案')
        return
    }
    if (!speak(text)) ElMessage.warning('当前浏览器不支持语音朗读')
}

async function saveVoice() {
    const text = (voiceDraft.text || '').trim()
    if (!text) {
        ElMessage.warning('请填写语音介绍文案')
        return
    }
    voiceSaving.value = true
    try {
        await setMaterialVoice(voiceDraft.id, text)
        voiceDialog.value = false
        ElMessage.success(`「${voiceDraft.name}」语音介绍已保存`)
    } catch (e) {
        ElMessage.error(plantState.saveError || '保存失败，请检查后端服务')
    } finally {
        voiceSaving.value = false
    }
}

async function submitMaterial() {
    const name = (materialDraft.name || '').trim()
    const code = (materialDraft.code || '').trim()
    if (!name) {
        ElMessage.warning('请填写物资名称')
        return
    }
    if (!materialDraft.scaleId) {
        ElMessage.warning('请选择主磅房')
        return
    }
    if (!materialDraft.pointNodeId) {
        ElMessage.warning('请选择取/送货点')
        return
    }
    if (materialList.value.some((item) => (item.name || '').trim().toLowerCase() === name.toLowerCase())) {
        ElMessage.warning(`物资「${name}」已存在，请换个名称`)
        return
    }
    if (code && materialList.value.some((item) => (item.code || '').trim().toLowerCase() === code.toLowerCase())) {
        ElMessage.warning(`物资编号「${code}」已存在，请换个编号`)
        return
    }

    materialSaving.value = true
    try {
        await addMaterial({
            name,
            code,
            direction: materialDraft.direction,
            category: (materialDraft.category || '').trim(),
            scaleId: materialDraft.scaleId,
            pointNodeId: materialDraft.pointNodeId,
            voiceText: (materialDraft.voiceText || '').trim(),
        })
        materialDialog.value = false
        ElMessage.success(`已新增物资「${name}」并写入数据库`)
    } catch (e) {
        ElMessage.error(plantState.saveError || '保存失败，请检查后端服务')
    } finally {
        materialSaving.value = false
    }
}

async function reload() {
    await loadConfig(true)
    ElMessage.success('已重新加载数据库配置')
}

onMounted(async () => {
    await loadConfig()
    dutyUrlDraft.value = plantState.plant.dutyUrl || ''
    vehicleWeighDraft.value = Number(plantState.plant.vehicleWeighCount) || 6
    scaleLogDraft.value = Number(plantState.plant.scaleLogCount) || 50
})
</script>

<style scoped>
.page {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.panel {
    background: #ffffff;
    border: 1px solid #e3eaf3;
    border-radius: 12px;
    padding: 12px 14px 14px;
    box-shadow: 0 4px 16px rgba(31, 58, 95, .05);
}

.panel-head {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    margin-bottom: 10px;
}

.panel-head-main {
    flex: 1;
    min-width: 0;
}

.panel-title-row {
    display: flex;
    align-items: center;
    gap: 8px;
}

.panel-title {
    font-size: 15px;
    font-weight: 700;
    color: #1e293b;
}

.count-chip {
    padding: 1px 8px;
    border-radius: 999px;
    background: #f1f6fd;
    color: #5b7ba6;
    font-size: 11px;
    line-height: 18px;
}

.panel-sub {
    margin-top: 3px;
    font-size: 12px;
    color: #94a3b8;
}

.scale-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
    gap: 10px;
}

.scale-card {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 7px;
    padding: 10px 12px 10px;
    border: 1px solid #e6edf6;
    border-radius: 10px;
    background: linear-gradient(180deg, #fbfdff 0%, #ffffff 46%);
    overflow: hidden;
    transition: border-color .15s ease, box-shadow .15s ease, transform .15s ease;
}

.scale-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 3px;
    background: #00a870;
}

.scale-card.s-2::before {
    background: #ff8f1f;
}

.scale-card.s-3::before {
    background: #ef232a;
}

.scale-card:hover {
    border-color: #cfe0f4;
    box-shadow: 0 10px 26px rgba(31, 58, 95, .09);
    transform: translateY(-1px);
}

.scale-head {
    display: flex;
    align-items: center;
    gap: 8px;
}

.scale-badge {
    width: 26px;
    height: 26px;
    flex: 0 0 26px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: linear-gradient(135deg, #1677ff, #4f9cff);
    color: #ffffff;
    font-size: 13px;
    font-weight: 700;
    box-shadow: 0 3px 8px rgba(22, 119, 255, .25);
}

.scale-title-wrap {
    flex: 1;
    min-width: 0;
}

.scale-title {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
}

.scale-sub {
    margin-top: 2px;
    font-size: 11px;
    color: #94a3b8;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.status-pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    padding: 2px 8px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    white-space: nowrap;
}

.pill-dot {
    width: 5px;
    height: 5px;
    border-radius: 50%;
    background: currentColor;
}

.status-pill.st-1 {
    color: #00875a;
    background: rgba(0, 168, 112, .1);
}

.status-pill.st-2 {
    color: #b45309;
    background: #fff4e5;
}

.status-pill.st-3 {
    color: #c62828;
    background: #fdeaea;
}

.scale-metrics {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 4px 8px;
    border-radius: 8px;
    background: #f7fafd;
}

.metric {
    display: flex;
    align-items: baseline;
    gap: 4px;
}

.metric b {
    font-size: 13px;
    color: #0f172a;
}

.metric span {
    font-size: 11px;
    color: #94a3b8;
}

.scale-bottom {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: auto;
    padding-top: 1px;
}

.products-label {
    flex: 0 0 auto;
    font-size: 11px;
    color: #94a3b8;
}

.scale-products {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    flex: 1;
    min-width: 0;
    margin-left: -2px;
}

.product-chip {
    padding: 1px 6px;
    border-radius: 6px;
    background: #eef5ff;
    color: #2f6cc0;
    font-size: 11px;
    line-height: 17px;
}

.scale-empty {
    font-size: 11px;
    color: #b6c2d4;
}

.scale-warning {
    padding: 4px 8px;
    border-radius: 6px;
    background: #fff7ef;
    border: 1px solid #ffe4c7;
    color: #b45309;
    font-size: 11px;
    line-height: 1.45;
}

.status-btn {
    height: 24px;
    padding: 0 9px;
    margin-left: 2px;
}

.status-btn .caret {
    margin-left: 3px;
    font-size: 10px;
    color: #94a3b8;
}

.panel-tools {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
}

/* 值班人员配置 */
.duty-head {
    align-items: center;
}

.duty-tools {
    flex: 1;
    justify-content: flex-end;
    min-width: 0;
}

.duty-input {
    width: 320px;
}

/* 地磅查询条数配置 */
.limits-tools {
    flex-wrap: wrap;
}

.limit-item {
    display: inline-flex;
    align-items: center;
    gap: 5px;
}

.limit-label {
    font-size: 12px;
    color: #475569;
    white-space: nowrap;
}

.limit-input {
    width: 96px;
}

.limit-unit {
    font-size: 12px;
    color: #94a3b8;
}

@media (max-width: 760px) {
    .duty-head {
        flex-direction: column;
        align-items: stretch;
    }

    .duty-input {
        width: 100%;
        flex: 1;
        min-width: 0;
    }
}

.status-filter {
    width: 110px;
}

.scale-empty-block {
    padding: 26px 0;
    border: 1px dashed #e2e8f0;
    border-radius: 10px;
    background: #f8fafc;
    color: #94a3b8;
    font-size: 12px;
    text-align: center;
}

.gate-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
    gap: 10px;
}

.gate-card {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 9px;
    padding: 10px 12px;
    border: 1px solid #e6edf6;
    border-radius: 10px;
    background: linear-gradient(180deg, #fbfdff 0%, #ffffff 46%);
    overflow: hidden;
    transition: border-color .15s ease, box-shadow .15s ease;
}

.gate-card::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 3px;
    background: #1c4785;
}

.gate-card.g-in::before {
    background: #00a870;
}

.gate-card.g-out::before {
    background: #ff8f1f;
}

.gate-card.g-none::before {
    background: #e5484d;
}

.gate-card:hover {
    border-color: #cfe0f4;
    box-shadow: 0 10px 26px rgba(31, 58, 95, .09);
}

.gate-head {
    display: flex;
    align-items: center;
    gap: 8px;
}

.gate-badge {
    width: 26px;
    height: 26px;
    flex: 0 0 26px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 8px;
    background: linear-gradient(135deg, #1c4785, #3a6bb0);
    color: #ffffff;
    font-size: 13px;
    font-weight: 700;
    box-shadow: 0 3px 8px rgba(28, 71, 133, .25);
}

.gate-title-wrap {
    flex: 1;
    min-width: 0;
}

.gate-title {
    font-size: 14px;
    font-weight: 700;
    color: #0f172a;
}

.gate-sub {
    margin-top: 2px;
    font-size: 11px;
    color: #94a3b8;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.gate-modes {
    align-self: flex-start;
}

.gate-materials {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-top: 7px;
    border-top: 1px dashed #e8eef7;
}

.gate-materials-label {
    flex: 0 0 auto;
    font-size: 11px;
    color: #94a3b8;
}

.gate-bind-field {
    flex: 1;
    min-width: 0;
}

.data-table {
    border-radius: 10px;
    overflow: hidden;
}

.data-table :deep(.nowrap-col .cell) {
    white-space: nowrap;
}

.dir-tag {
    display: inline-block;
    padding: 1px 8px;
    border-radius: 5px;
    font-size: 11px;
    font-weight: 600;
    white-space: nowrap;
}

.dir-out {
    color: #2f6cc0;
    background: #eef5ff;
}

.dir-in {
    color: #0f7a5a;
    background: #eaf7f1;
}

.voice-cell {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    cursor: pointer;
}

.voice-cell:hover .voice-text {
    color: #1677ff;
}

.voice-text {
    flex: 1;
    min-width: 0;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    font-size: 12px;
    line-height: 1.5;
    color: #475569;
}

.voice-text.empty {
    color: #b6c2d4;
}

.voice-tip {
    margin-bottom: 10px;
    padding: 8px 10px;
    border-radius: 8px;
    background: #f2f7ff;
    border: 1px solid #d8e6fb;
    color: #4b6a92;
    font-size: 12px;
    line-height: 1.6;
}

.voice-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.voice-footer-right {
    display: flex;
    gap: 8px;
}

.voice-input {
    display: flex;
    flex-direction: column;
    gap: 6px;
    width: 100%;
}

.voice-input .voice-preview {
    align-self: flex-end;
}

.bind-name {
    font-weight: 600;
    color: #1e293b;
}

.bind-name.bad {
    color: #c62828;
}

.mini-bad {
    margin-left: 6px;
    padding: 1px 6px;
    border-radius: 4px;
    background: #fdeaea;
    color: #c62828;
    font-size: 10px;
}

.bind-state {
    font-size: 12px;
    font-weight: 600;
}

.state-ok {
    color: #00a870;
}

.state-bad {
    color: #ef232a;
}

.bind-field {
    position: relative;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    min-height: 26px;
    padding: 2px 22px 2px 6px;
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    background: #ffffff;
    cursor: pointer;
    transition: border-color .15s ease;
}

.bind-field:hover {
    border-color: #1677ff;
}

.bind-chip {
    position: relative;
    display: inline-block;
    margin: 3px 9px 3px 0;
    padding: 1px 8px;
    border-radius: 5px;
    background: #eef5ff;
    color: #2f6cc0;
    font-size: 12px;
    line-height: 17px;
    white-space: nowrap;
}

.chip-x {
    position: absolute;
    top: -6px;
    right: -6px;
    width: 13px;
    height: 13px;
    padding: 0;
    border: none;
    border-radius: 50%;
    background: #a7b3c4;
    color: #ffffff;
    font-size: 10px;
    line-height: 13px;
    text-align: center;
    cursor: pointer;
}

.chip-x:hover {
    background: #ef232a;
}

.chip-sep {
    margin-right: 4px;
    color: #94a3b8;
    font-size: 12px;
}

.bind-placeholder {
    padding-left: 2px;
    color: #b6bfcc;
    font-size: 12px;
}

.bind-caret {
    position: absolute;
    top: 50%;
    right: 8px;
    transform: translateY(-50%);
    color: #94a3b8;
    font-size: 11px;
    pointer-events: none;
}

.bind-options {
    display: flex;
    flex-direction: column;
    gap: 2px;
    max-height: 220px;
    overflow: auto;
}

.bind-options :deep(.el-checkbox) {
    height: 26px;
    margin-right: 0;
}

.table-tip {
    margin-top: 10px;
    font-size: 11px;
    line-height: 1.6;
    color: #94a3b8;
}
</style>
