<template>
    <div class="page" :class="{land: landLayout, narrow: narrowLayout, tight: tightLayout, rot: rotated}">
        <!-- 旋转态的消息容器：ElMessage 默认 teleport 到 body，壳转了 90° 后它会「竖着」弹在画布外 -->
        <div ref="toastHost" class="toast-host"></div>

        <section class="panel">
            <div class="panel-head">
                <div class="panel-head-main">
                    <div class="panel-title-row">
                        <span class="panel-title">车牌过磅记录</span>
                    </div>
                    <div class="panel-sub">按车牌号（支持模糊）查询最近的过磅记录及关联的送货/拉货单据（数据来自 jt_dev 过磅系统）</div>
                </div>
                <div class="panel-tools">
                    <!-- 旋转态锁只读：系统软键盘的方向由操作系统决定，网页转不动它，
                         弹出来就是竖的、正好压在横过来的画面上 ⇒ 改用下面那套页面内车牌键盘。
                         打开键盘挂在外面这层（display:contents，不占位）的 click 上：
                         点的是里头的 input，事件冒泡到这里，比 @focus 可靠 -->
                    <span class="vehicle-field" @click="onVehicleFocus">
                        <el-input v-model="vehicleNo" size="small" class="vehicle-input" clearable
                                  :readonly="rotated" inputmode="none"
                                  placeholder="如 陕KM9368 / KM93"
                                  @focus="onVehicleFocus" @keydown="onVehicleKeydown"
                                  @keyup.enter="searchByVehicle"/>
                    </span>
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
                    <!-- 旋转态（始终横屏/竖屏）不能 teleport：下拉面板用 client 坐标定位，
                         跑到旋转壳外面就会「竖着」弹在画布外，详见 PlantStore.pageRotated -->
                    <el-select v-model="statusFilter" size="small" class="status-filter" :teleported="!rotated">
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

        <!-- 不能 append-to-body：切页配成「始终横屏/竖屏」时整层壳被旋转，弹窗挂到 body 会
             跑到旋转坐标系外面（文字方向与页面正交，且宽度按视口算会超出画布） -->
        <el-dialog v-model="resultDialog" :title="resultTitle" :width="dialogWidth" :top="dialogTop"
                   :append-to-body="false" class="gb-result-dialog" destroy-on-close>
            <div class="result-body">
                <!-- 磅房日志结果 -->
                <template v-if="resultMode === 'logs'">
                    <div v-if="logRows.length" ref="logListRef" class="log-list" :style="{maxHeight: listMaxHeight}">
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
                              class="event-table" :max-height="landLayout ? landListHeight : undefined">
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
                            <template #default="{ row }">{{ scaleNo(row.weighingUserName) }}</template>
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

        <!-- 页面内车牌键盘（只在旋转态出现）：系统软键盘跟着设备物理方向走，网页没法把它转过来，
             所以旋转态把输入框锁成只读、改弹这套键盘 —— 它在旋转壳里面，跟着画布一起转。
             用 fixed 贴画布底边：.layout 有 transform，fixed 的包含块就是它，坐标天然是画布像素。 -->
        <div v-if="padOpen" class="kbd">
            <div class="kbd-head">
                <span class="kbd-title">车牌号</span>
                <span class="kbd-value" :class="{empty: !vehicleNo}">{{ vehicleNo || '支持只输一部分，如 KM93' }}</span>
                <button class="kbd-done" @click="padOpen = false">完成</button>
            </div>
            <div class="kbd-rows">
                <button v-for="key in PAD_KEYS" :key="key" class="kbd-key" @click="padInput(key)">{{ key }}</button>
                <button class="kbd-key kbd-fn back" @click="padBack">退格</button>
                <button class="kbd-key kbd-fn" @click="vehicleNo = ''">清空</button>
            </div>
        </div>
    </div>
</template>

<script setup lang="js">
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {ElMessage} from 'element-plus'
import {
    STATUS_TEXT,
    boundScaleIds,
    loadConfig,
    materialList,
    pageCanvasNarrow,
    pageCanvasSize,
    pageLandscape,
    pageRotated,
    plantState,
    scaleList,
    scaleStatusOf,
} from '../system/PlantStore.js'
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

// ===== 提示信息 =====
// 旋转态不能扔到 body：整层壳被 rotate(±90°) 后，body 上的消息既方向正交、又落在画布外。
// 挂到页面内的 .toast-host 上，它就跟着画布一起转；非旋转态仍走 body（顶部居中，行为不变）。
const toastHost = ref(null)

function toast(type, message) {
    ElMessage({message, type, appendTo: rotated.value && toastHost.value ? toastHost.value : document.body})
}

// ===== 页面内车牌键盘（旋转态专用）=====
// 系统软键盘的方向由操作系统按设备物理方向决定，网页无法旋转它 ⇒ 旋转态把输入框锁成 readonly，
// 改弹这套跟着画布转的键盘。键位按车牌实际用法排：数字一行、字母三行（含常用的「陕」），
// 最后一行交给「退格 / 清空」，右下角的「完成」在键盘头部。
const PAD_ROWS = [
    ['1', '2', '3', '4', '5', '6', '7', '8', '9', '0'],
    ['Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P'],
    ['A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L', '陕'],
    ['Z', 'X', 'C', 'V', 'B', 'N', 'M'],
]
const PAD_KEYS = PAD_ROWS.flat()
const PAD_MAX = 12
const padOpen = ref(false)

function onVehicleFocus() {
    if (rotated.value) padOpen.value = true
}

function padInput(key) {
    if (vehicleNo.value.length >= PAD_MAX) return
    vehicleNo.value += key
}

function padBack() {
    vehicleNo.value = vehicleNo.value.slice(0, -1)
}

// 接实体键盘：readonly 的输入框照样收得到 keydown（外接键盘 / 车机实体键时不用另开输入法）
function onVehicleKeydown(event) {
    if (!rotated.value || !padOpen.value) return
    if (event.key === 'Backspace') {
        event.preventDefault()
        padBack()
    } else if (/^[a-zA-Z0-9]$/.test(event.key)) {
        event.preventDefault()
        padInput(event.key.toUpperCase())
    } else if (event.key === 'Enter' || event.key === 'Escape') {
        padOpen.value = false
    }
}


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

// 送货/拉货类型文案；过磅编号取过磅人员列（weighing_user_name），「4#磅」这类值只显示数字 4
const DOC_TYPE_TEXT = {delivery: '送货', sendcar: '拉货'}

function scaleNo(value) {
    const text = (value || '').trim()
    if (!text) return '-'
    const m = text.match(/\d+/)
    return m ? m[0] : text
}

// 查询条数：配置页可配（存于 nav.plant），兜底 6 / 50
const vehicleWeighCount = computed(() => Number(plantState.plant.vehicleWeighCount) || 6)
const scaleLogCount = computed(() => Number(plantState.plant.scaleLogCount) || 50)

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
        toast('warning', `「${scale.name}」尚未配置磅号（nav.scale.gb_no），无法查询`)
        return
    }
    startCd(scale.id)
    queryingId.value = scale.id
    try {
        const res = await axiosInst.get('/api/gb/list', {params: {source: gbNo, page: 1, size: scaleLogCount.value}})
        const data = res?.data?.data
        logRows.value = data?.list || []
        logTotal.value = Number(data?.total) || 0
        resultMode.value = 'logs'
        resultTitle.value = `${scale.name} · 最近过磅日志`
        resultDialog.value = true
    } catch (e) {
        toast('error', plantState.loadError ? '后端不可用' : '日志查询失败，请检查后端服务')
    } finally {
        queryingId.value = ''
    }
}

async function searchByVehicle() {
    const no = (vehicleNo.value || '').trim()
    if (!no) {
        toast('warning', '请输入车牌号（支持只输一部分）')
        return
    }
    startCd(VEHICLE_KEY)
    searching.value = true
    try {
        // jt_dev 过磅系统：最近 N 条过磅记录（含送货/拉货单据），N 由配置页设定
        const res = await axiosInst.get('/api/gb/vehicleWeighs', {params: {vehicleNo: no, size: vehicleWeighCount.value}})
        const data = res?.data?.data
        eventRows.value = Array.isArray(data) ? data : []
        resultMode.value = 'events'
        resultTitle.value = `车牌「${no}」的过磅记录`
        resultDialog.value = true
    } catch (e) {
        toast('error', '过磅记录查询失败，请检查后端服务')
    } finally {
        searching.value = false
    }
}

// 本页在 nav.page_display 里的标识（与路由 name 一致）
const PAGE_CODE = 'home_gb'
// 版式档位：land = 横屏车机（画布矮而宽）；narrow = 竖屏手机；tight = 更窄（日志行改上下叠）
const landLayout = ref(false)
const narrowLayout = ref(false)
const tightLayout = ref(false)
// 整层壳是否被旋转过（配「始终横屏/竖屏」且视口方向相反）：决定下拉面板能不能 teleport
const rotated = ref(false)
// 画布像素尺寸：旋转态下 vw/vh 不跟 transform 转（竖视口 + 始终横屏时画布 852×393、
// 而 100vh 仍是 852），弹窗宽度与列表高度只能按这个算
const canvasSize = ref({w: 0, h: 0})

function syncLayout() {
    landLayout.value = pageLandscape(PAGE_CODE)
    narrowLayout.value = !landLayout.value && pageCanvasNarrow(PAGE_CODE)
    tightLayout.value = narrowLayout.value && pageCanvasNarrow(PAGE_CODE, 480)
    rotated.value = pageRotated(PAGE_CODE)
    canvasSize.value = pageCanvasSize(PAGE_CODE)
    // 不再旋转了（转回真横屏/竖屏）就把页面内键盘收起来，输入框也解回可编辑 —— 系统键盘方向正常了
    if (!rotated.value && padOpen.value) padOpen.value = false
}

// 结果弹窗宽度：不能用 vw —— 切页配成「始终横屏」时是整层 CSS 旋转，94vw 拿到的是视口宽
// （竖着拿 = 393），而画布宽是 852，弹窗会缩成中间一小条。按画布像素算才对。
const dialogWidth = computed(() => {
    const w = canvasSize.value.w || window.innerWidth
    return `${Math.round(Math.min(760, Math.max(280, w * 0.94)))}px`
})

// 弹窗的垂直起点：横屏画布只有 ~393px 高，默认 15vh 是按视口高算的（旋转态会算成 128px），
// 加上内容高度就把弹窗顶出画布底部
const dialogTop = computed(() => (landLayout.value ? '10px' : '15vh'))

// 日志/表格的可视高度：横屏只有 ~393px 画布高，弹窗给到「画布高 - 标题栏 - 按钮行」
const landListHeight = computed(() =>
    Math.round(Math.max(140, (canvasSize.value.h || window.innerHeight) - 168)))
const listMaxHeight = computed(() => (landLayout.value ? `${landListHeight.value}px` : '56vh'))

let layoutTimer = 0

function onViewportChange() {
    syncLayout()
    clearTimeout(layoutTimer)
    layoutTimer = setTimeout(syncLayout, 320)
}

syncLayout()

onMounted(async () => {
    window.addEventListener('resize', onViewportChange)
    window.addEventListener('orientationchange', onViewportChange)
    await loadConfig()
    // 配置到货后才拿到本页的显示方向
    syncLayout()
})

onBeforeUnmount(() => {
    window.removeEventListener('resize', onViewportChange)
    window.removeEventListener('orientationchange', onViewportChange)
    clearTimeout(layoutTimer)
    if (cdTimer) clearInterval(cdTimer)
})
</script>

<style scoped>
.page {
    /* 提示信息容器用 absolute 挂在页面内，需要这里当包含块 */
    position: relative;
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

/* 只为了让 click 有个落脚点：display:contents 不生成盒子，flex 布局照旧按里面的 el-input 算 */
.vehicle-field {
    display: contents;
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

/* 横屏车机（.page.land）：画布矮而宽（852×393 一类，内容区只有 ~311px 高），
 * 唯一目标是「一屏看全所有磅房」—— 说明文字与卡片留白全部让位，卡片压成 4 列 2 行。
 * 由 PlantStore.pageLandscape(切页配置 + 视口) 决定，home.vue 负责旋转整层壳。 */
.page.land {
    gap: 8px;
}

.page.land .panel {
    padding: 6px 9px 7px;
    border-radius: 10px;
}

.page.land .panel-head {
    align-items: center;
    gap: 10px;
}

/* 副标题在横屏只是白占一行高度（每条 ~19px），隐藏 */
.page.land .panel-sub {
    display: none;
}

.page.land .panel-title {
    font-size: 14px;
}

.page.land .vehicle-input {
    width: 190px;
}

.page.land .status-filter {
    width: 86px;
}

.page.land .scale-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 7px;
    margin-top: 6px;
}

.page.land .scale-card {
    gap: 5px;
    padding: 7px 8px 8px;
    border-radius: 8px;
}

.page.land .scale-card:hover {
    transform: none;
}

.page.land .scale-head {
    gap: 6px;
}

.page.land .scale-badge {
    width: 20px;
    height: 20px;
    flex: 0 0 20px;
    border-radius: 6px;
    font-size: 11px;
}

.page.land .scale-title {
    font-size: 12.5px;
}

.page.land .scale-sub {
    margin-top: 1px;
    font-size: 10px;
}

.page.land .status-pill {
    gap: 4px;
    padding: 1px 6px;
    font-size: 10px;
}

.page.land .scale-metrics {
    padding: 2px 7px;
    border-radius: 6px;
}

.page.land .metric b {
    font-size: 12px;
}

.page.land .metric span {
    font-size: 10px;
}

/* 绑定物资只留一行、超出裁掉（横屏看状态时物资是次要信息，省下的高度给第二行卡片） */
.page.land .scale-bottom {
    flex-wrap: nowrap;
    gap: 5px;
    overflow: hidden;
}

.page.land .products-label {
    font-size: 10px;
}

.page.land .scale-products {
    flex-wrap: nowrap;
    overflow: hidden;
}

.page.land .product-chip {
    padding: 0 5px;
    font-size: 10px;
    line-height: 15px;
}

.page.land .query-btn {
    height: 22px;
    padding: 0 8px;
    font-size: 11px;
}

/* 竖屏手机（.page.narrow）：画布窄，标题与工具改上下排。
 * 用类名而不是 @media(max-width:760px)：横屏/竖屏被切页配置旋转过时，
 * 媒体查询看到的视口宽和实际画布宽不是一回事（详见 PlantStore.pageCanvasNarrow）。 */
.page.narrow .panel-head {
    flex-direction: column;
}

.page.narrow .panel-tools {
    width: 100%;
}

.page.narrow .vehicle-input {
    flex: 1;
    min-width: 0;
    width: auto;
}

.page.narrow .status-filter {
    width: 96px;
}

/* 更窄的画布（.page.tight）：日志行改为时间在上、内容在下，避免时间列挤占内容宽度 */
.page.tight .log-row {
    flex-direction: column;
    gap: 1px;
}

.page.tight .log-time {
    flex: none;
    font-size: 11px;
}

/* 旋转态（.page.rot：配「始终横屏/竖屏」且视口方向相反）的 el-select 下拉面板。
 * 组件默认把面板 teleport 到 body，用的是 client 坐标（相对屏幕，不是旋转后的画布）——
 * 整层壳被转了 ±90° 后，面板既对不准字段、方向也是「正的」（相对手机反而是竖的）。
 * 模板里已按 rotated 关掉 teleport 改内联渲染，这里再把位置/尺寸按「字段正下方」固定。
 * ⚠️ 不要覆盖 display：开合由组件自己控制，写死会让面板一直显示。
 * ⚠️ z-index 必须给：.scale-card 是 position:relative，不抬高层级会被卡片压住。
 * ⚠️ bottom/right 要一起写：面板向上翻转时组件会留下 bottom，与 top:100% 一起把高度压成 0。 */
.page.rot .status-filter :deep(.el-select__popper) {
    position: absolute !important;
    top: 100% !important;
    left: auto !important;
    right: 0 !important;
    bottom: auto !important;
    transform: none !important;
    z-index: 3000 !important;
    width: max-content !important;
    min-width: 100% !important;
    max-width: 220px !important;
    margin: 4px 0 0;
    max-height: 170px;
    overflow-y: auto;
}

/* 内联后的小箭头没有定位依据（组件那套偏移量是给 client 坐标算的），直接不要 */
.page.rot .status-filter :deep(.el-popper__arrow) {
    display: none;
}

/* ===== 旋转态的两个「逃出旋转壳」修正 ===== */

/* 1) 提示信息：旋转态把它挂在 .toast-host（页面内，跟着画布一起转）。
 * 消息元素本身是 fixed + 行内 top 偏移（那套坐标是给 body 算的），这里改回静态排列，
 * 由容器的 flex 竖排 —— 行内 top 对 static 元素无效，多条消息自然叠加。 */
.toast-host {
    position: absolute;
    top: 6px;
    left: 0;
    right: 0;
    z-index: 4000;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    pointer-events: none;
}

.toast-host :deep(.el-message) {
    position: static !important;
    top: auto !important;
    left: auto !important;
    margin: 0 !important;
    max-width: 92%;
    transform: none !important;
    pointer-events: auto;
}

/* 2) 页面内车牌键盘：贴画布底边、按画布宽度排 10 列。
 * ⚠️ 用 fixed 而不是 absolute：.layout 带 transform，fixed 的包含块就是它 ⇒ 坐标是画布像素；
 *    写成 absolute 会相对 .page（在 .main 的滚动容器里）算，页面一滚就跟着走。 */
.kbd {
    position: fixed;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 3500;
    padding: 6px 8px 8px;
    background: rgba(15, 23, 42, .96);
    box-shadow: 0 -6px 20px rgba(15, 23, 42, .35);
}

.kbd-head {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 2px 5px;
}

.kbd-title {
    flex: 0 0 auto;
    font-size: 12px;
    font-weight: 600;
    color: #94a3b8;
}

.kbd-value {
    flex: 1;
    min-width: 0;
    font-size: 15px;
    font-weight: 700;
    letter-spacing: 1px;
    color: #ffffff;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.kbd-value.empty {
    font-size: 11px;
    font-weight: 400;
    letter-spacing: 0;
    color: #64748b;
}

.kbd-done {
    flex: 0 0 auto;
    height: 24px;
    padding: 0 14px;
    border: none;
    border-radius: 6px;
    background: #1677ff;
    color: #ffffff;
    font-family: inherit;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
}

.kbd-rows {
    display: grid;
    grid-template-columns: repeat(10, minmax(0, 1fr));
    gap: 3px;
}

.kbd-key {
    height: 28px;
    padding: 0;
    border: 1px solid rgba(148, 163, 184, .3);
    border-radius: 5px;
    background: #1e293b;
    color: #e2e8f0;
    font-family: inherit;
    font-size: 13px;
    line-height: 1;
    cursor: pointer;
    -webkit-tap-highlight-color: transparent;
    user-select: none;
}

.kbd-key:active {
    background: #334155;
    border-color: #1677ff;
}

/* 最后一行只剩 7 个字母 + 退格(2 格) + 清空(1 格)，正好补满 10 列 */
.kbd-fn {
    font-size: 11px;
    color: #b6c7dd;
    background: #24344a;
}

.kbd-fn.back {
    grid-column: span 2;
}
</style>
