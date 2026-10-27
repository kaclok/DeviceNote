<template>
    <div class="page">
        <section class="panel">
            <div class="panel-head">
                <div class="panel-head-main">
                    <div class="panel-title-row">
                        <span class="panel-title">车牌过磅记录</span>
                    </div>
                    <div class="panel-sub">按车牌号（支持模糊）查询最近 6 条过磅记录及关联的送货/拉货单据（数据来自 jt_dev 过磅系统）</div>
                </div>
                <div class="panel-tools">
                    <el-input v-model="vehicleNo" size="small" class="vehicle-input" clearable
                              placeholder="如 陕KM9368 / KM93" @keyup.enter="searchByVehicle"/>
                    <el-button size="small" type="primary" :loading="searching"
                               :disabled="cdOf(VEHICLE_KEY) > 0" @click="searchByVehicle">
                        {{ cdOf(VEHICLE_KEY) > 0 ? `查询中(${cdOf(VEHICLE_KEY)}s)` : '查询记录' }}
                    </el-button>
                </div>
            </div>
        </section>

        <section class="panel">
            <div class="panel-head">
                <div class="panel-head-main">
                    <div class="panel-title-row">
                        <span class="panel-title">磅房过磅日志</span>
                        <span class="count-chip">{{ gbScales.length }} 座磅房</span>
                    </div>
                    <div class="panel-sub">点击「查询记录」查看该磅最近上报的日志（超重、异常等会标红）；「磅号」读取自磅房配置（nav.scale.gb_no）</div>
                </div>
                <div class="panel-tools">
                    <el-select v-model="statusFilter" size="small" class="status-filter">
                        <el-option v-for="item in STATUS_OPTIONS" :key="item.value"
                                   :label="item.label" :value="item.value"/>
                    </el-select>
                </div>
            </div>

            <div class="scale-grid">
                <div v-for="scale in gbScales" :key="scale.id" class="scale-card" :class="`s-${scaleStatusOf(scale.id)}`">
                    <div class="scale-head">
                        <div class="scale-badge">磅</div>
                        <div class="scale-title-wrap">
                            <div class="scale-title">{{ scale.name }}</div>
                            <div class="scale-sub">{{ scale.code }} · {{ scale.area || '未设置区域' }}</div>
                        </div>
                        <span class="status-pill" :class="`st-${scaleStatusOf(scale.id)}`">
                            <i class="pill-dot"></i>{{ STATUS_TEXT[scaleStatusOf(scale.id)] }}
                        </span>
                    </div>

                    <div class="scale-metrics">
                        <div class="metric">
                            <b>{{ scale.capacity || '-' }}</b>
                            <span>限重</span>
                        </div>
                    </div>

                    <div class="scale-bottom">
                        <span class="products-label">绑定物资</span>
                        <div class="scale-products">
                            <span v-for="name in materialsOf(scale.id)" :key="name" class="product-chip">{{ name }}</span>
                            <span v-if="!materialsOf(scale.id).length" class="scale-empty">暂无</span>
                        </div>
                        <el-button size="small" type="primary" plain class="query-btn"
                                   :loading="queryingId === scale.id"
                                   :disabled="cdOf(scale.id) > 0" @click="queryScale(scale)">
                            {{ cdOf(scale.id) > 0 ? `查询中(${cdOf(scale.id)}s)` : '查询记录' }}
                        </el-button>
                    </div>
                </div>
            </div>

            <div v-if="!gbScales.length" class="scale-empty-block">
                暂无磅房，可在「配置」页添加
            </div>
        </section>

        <el-dialog v-model="resultDialog" :title="resultTitle" width="min(760px, 94vw)" append-to-body
                   class="gb-result-dialog" destroy-on-close>
            <div class="result-body">
                <!-- 磅房日志结果 -->
                <template v-if="resultMode === 'logs'">
                    <div v-if="logRows.length" ref="logListRef" class="log-list">
                        <div v-for="row in logRows" :key="row.id" class="log-row"
                             :class="{warn: isAbnormal(row.content)}">
                            <span class="log-time">{{ row.time }}</span>
                            <span class="log-content">{{ row.content }}</span>
                        </div>
                    </div>
                    <div v-else class="gb-empty">该磅还没有上报过日志（请确认磅号配置与监控器一致）</div>
                    <div v-if="logTotal > logRows.length" class="log-more">仅展示最近 {{ logRows.length }} 条，共 {{ logTotal }} 条</div>
                </template>

                <!-- 车牌过磅记录结果（jt_dev：送货/拉货混合罗列，类型标签区分） -->
                <template v-else>
                    <el-table v-if="eventRows.length" ref="eventTableRef" :data="eventRows" size="small" border
                              class="event-table">
                        <el-table-column label="类型" width="64" align="center">
                            <template #default="{ row }">
                                <span v-if="row.docType" class="doc-tag" :class="`dt-${row.docType}`">
                                    {{ DOC_TYPE_TEXT[row.docType] || row.docType }}
                                </span>
                                <span v-else>-</span>
                            </template>
                        </el-table-column>
                        <el-table-column label="单据号" min-width="130" show-overflow-tooltip>
                            <template #default="{ row }">{{ row.docCode || '-' }}</template>
                        </el-table-column>
                        <el-table-column label="货物名称" min-width="90" show-overflow-tooltip>
                            <template #default="{ row }">{{ row.goodsName || '-' }}</template>
                        </el-table-column>
                        <el-table-column label="称重编号" width="138" show-overflow-tooltip>
                            <template #default="{ row }">{{ row.weighingNo || '-' }}</template>
                        </el-table-column>
                        <el-table-column label="过磅编号" width="76" align="center">
                            <template #default="{ row }">{{ scaleNo(row.weighingStation) }}</template>
                        </el-table-column>
                        <el-table-column label="过磅时间" width="150">
                            <template #default="{ row }">{{ fmtTime(row.weighingTime) }}</template>
                        </el-table-column>
                        <el-table-column label="过磅重量" width="86" align="right">
                            <template #default="{ row }">{{ row.weighingWeight ?? '-' }}{{ row.weightUnit || '' }}</template>
                        </el-table-column>
                    </el-table>
                    <div v-else class="gb-empty">没有查到该车牌的过磅记录</div>
                </template>

                <!-- 一键到顶/到底：放在内容下方独立一行，不悬浮在内容上，避免手机端挡住点击 -->
                <div class="jump-btns">
                    <button class="jump-btn" @click="jumpScroll(true)">↑ 到顶</button>
                    <button class="jump-btn" @click="jumpScroll(false)">↓ 到底</button>
                </div>
            </div>
        </el-dialog>
    </div>
</template>

<script setup lang="js">
import {computed, onMounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {STATUS_TEXT, boundScaleIds, loadConfig, materialList, plantState, scaleList, scaleStatusOf} from '../system/PlantStore.js'
import {axiosInst} from '@/framework/services/net/AxiosInst.js'

// 异常日志关键词：命中即整行标红（超重、身份证未维护、主键重复、报警等）
const ABNORMAL_KEYWORDS = ['超重', '异常', '报警', '失败', '错误', '身份证', '主键']

// 磅房状态筛选：与「磅房状态」页同款下拉，默认只看正常
const STATUS_OPTIONS = [
    {value: 0, label: '全部'},
    {value: 1, label: '正常'},
    {value: 2, label: '检修'},
    {value: 3, label: '停用'},
]
const statusFilter = ref(1)

const vehicleNo = ref('')
const searching = ref(false)
const queryingId = ref('')

// 查询按钮冷却：key 为磅房 id（车牌查询用 VEHICLE_KEY），点击后 5s 内禁用并显示倒计时
const CD_SECONDS = 5
const VEHICLE_KEY = '__vehicle__'
const cdMap = ref({})
let cdTimer = null

function cdOf(key) {
    return cdMap.value[key] || 0
}

function startCd(key) {
    cdMap.value = {...cdMap.value, [key]: CD_SECONDS}
    if (cdTimer) return
    cdTimer = setInterval(() => {
        const next = {}
        let any = false
        for (const [k, v] of Object.entries(cdMap.value)) {
            if (v - 1 > 0) {
                next[k] = v - 1
                any = true
            }
        }
        cdMap.value = next
        if (!any) {
            clearInterval(cdTimer)
            cdTimer = null
        }
    }, 1000)
}

const resultDialog = ref(false)
const resultMode = ref('logs')
const resultTitle = ref('')
const logRows = ref([])
const logTotal = ref(0)
const eventRows = ref([])

// 送货/拉货类型文案；过磅编号取磅号里的数字（如「4#磅」显示 4）
const DOC_TYPE_TEXT = {delivery: '送货', sendcar: '拉货'}

function scaleNo(station) {
    const m = (station || '').match(/\d+/)
    return m ? m[0] : '-'
}

// 全部磅房（含检修/停用，历史日志仍可查），按状态筛选 + 排序；
// 磅号只读展示，数据始终来自 plantState.scales（nav.scale.gb_no）
const gbScales = computed(() => scaleList.value
    .filter((scale) => statusFilter.value === 0 || scaleStatusOf(scale.id) === statusFilter.value)
    .slice()
    .sort((a, b) => (scaleStatusOf(a.id) - scaleStatusOf(b.id)) || ((a.sortNo || 0) - (b.sortNo || 0))))

const materialsById = computed(() => {
    const map = new Map()
    for (const material of materialList.value) {
        for (const id of boundScaleIds(material)) {
            if (!map.has(id)) map.set(id, [])
            map.get(id).push(material.name)
        }
    }
    return map
})

function materialsOf(scaleId) {
    return materialsById.value.get(scaleId) || []
}

function isAbnormal(content) {
    return ABNORMAL_KEYWORDS.some((word) => (content || '').includes(word))
}

function fmtTime(v) {
    // 后端 LocalDateTime 可能序列化为数组 [y,m,d,h,mi,s]（jackson 未注册 JavaTimeModule 时）
    if (Array.isArray(v)) {
        const p = (n) => String(n ?? 0).padStart(2, '0')
        return `${v[0]}-${p(v[1])}-${p(v[2])} ${p(v[3])}:${p(v[4])}:${p(v[5])}`
    }
    return (v || '').replace('T', ' ').slice(0, 19)
}

// 一键到顶/到底：日志列表直接滚，事件表格走 el-table 的 setScrollTop
const logListRef = ref(null)
const eventTableRef = ref(null)

function jumpScroll(toTop) {
    if (resultMode.value === 'logs') {
        const el = logListRef.value
        if (!el) return
        el.scrollTo({top: toTop ? 0 : el.scrollHeight, behavior: 'smooth'})
    } else {
        eventTableRef.value?.setScrollTop(toTop ? 0 : 999999)
    }
}

async function queryScale(scale) {
    const gbNo = (scale.gbNo || '').trim()
    if (!gbNo) {
        ElMessage.warning(`「${scale.name}」尚未配置磅号（nav.scale.gb_no），无法查询`)
        return
    }
    startCd(scale.id)
    queryingId.value = scale.id
    try {
        const res = await axiosInst.get('/api/gb/list', {params: {source: gbNo, page: 1, size: 50}})
        const data = res?.data?.data
        logRows.value = data?.list || []
        logTotal.value = Number(data?.total) || 0
        resultMode.value = 'logs'
        resultTitle.value = `${scale.name} · 最近过磅日志`
        resultDialog.value = true
    } catch (e) {
        ElMessage.error(plantState.loadError ? '后端不可用' : '日志查询失败，请检查后端服务')
    } finally {
        queryingId.value = ''
    }
}

async function searchByVehicle() {
    const no = (vehicleNo.value || '').trim()
    if (!no) {
        ElMessage.warning('请输入车牌号（支持只输一部分）')
        return
    }
    startCd(VEHICLE_KEY)
    searching.value = true
    try {
        // jt_dev 过磅系统：最近 6 条过磅记录（含送货/拉货单据）
        const res = await axiosInst.get('/api/gb/vehicleWeighs', {params: {vehicleNo: no, size: 6}})
        const data = res?.data?.data
        eventRows.value = Array.isArray(data) ? data : []
        resultMode.value = 'events'
        resultTitle.value = `车牌「${no}」的过磅记录`
        resultDialog.value = true
    } catch (e) {
        ElMessage.error('过磅记录查询失败，请检查后端服务')
    } finally {
        searching.value = false
    }
}

onMounted(loadConfig)
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

.panel-tools {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
}

.vehicle-input {
    width: 240px;
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

.status-filter {
    width: 110px;
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

.query-btn {
    margin-left: auto;
    height: 24px;
    padding: 0 10px;
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

/* 日志结果 */
.jump-btns {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-top: 10px;
}

.jump-btn {
    min-width: 84px;
    height: 32px;
    padding: 0 14px;
    border: 1px solid #dbe6f3;
    border-radius: 8px;
    background: #f8fbff;
    color: #35639f;
    font-size: 13px;
    line-height: 1;
    cursor: pointer;
    transition: background .15s ease, color .15s ease;
    -webkit-tap-highlight-color: transparent;
}

.jump-btn:hover {
    background: #eef6ff;
    color: #1677ff;
}

.jump-btn:active {
    background: #dcecfd;
}

.log-list {
    max-height: 56vh;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.log-row {
    display: flex;
    gap: 10px;
    padding: 5px 8px;
    border-radius: 6px;
    font-size: 12px;
    line-height: 1.55;
    color: #334155;
}

.log-row:nth-child(odd) {
    background: #f8fafc;
}

.log-row.warn {
    background: #fdeaea;
    color: #c62828;
    font-weight: 600;
}

.log-time {
    flex: 0 0 150px;
    color: #94a3b8;
    font-variant-numeric: tabular-nums;
}

.log-row.warn .log-time {
    color: #d97575;
}

.log-content {
    flex: 1;
    min-width: 0;
    word-break: break-all;
    white-space: pre-wrap;
}

.log-more {
    margin-top: 8px;
    font-size: 11px;
    color: #94a3b8;
    text-align: center;
}

.gb-empty {
    padding: 30px 0;
    color: #94a3b8;
    font-size: 13px;
    text-align: center;
}

.event-table {
    max-height: 56vh;
    overflow-y: auto;
}

/* 送货/拉货类型标签 */
.doc-tag {
    display: inline-block;
    padding: 1px 8px;
    border-radius: 999px;
    font-size: 11px;
    font-weight: 600;
    line-height: 18px;
    white-space: nowrap;
}

.doc-tag.dt-delivery {
    color: #00875a;
    background: rgba(0, 168, 112, .12);
}

.doc-tag.dt-sendcar {
    color: #b45309;
    background: #fff4e5;
}

/* 竖屏手机：本页以手机端为主要使用场景 */
@media (max-width: 760px) {
    .panel-head {
        flex-direction: column;
    }

    .panel-tools {
        width: 100%;
    }

    .vehicle-input {
        flex: 1;
        min-width: 0;
        width: auto;
    }

    .status-filter {
        width: 96px;
    }
}

/* 窄屏（≤480px）：日志行改为时间在上、内容在下，避免时间列挤占内容宽度 */
@media (max-width: 480px) {
    .log-row {
        flex-direction: column;
        gap: 1px;
    }

    .log-time {
        flex: none;
        font-size: 11px;
    }
}
</style>
