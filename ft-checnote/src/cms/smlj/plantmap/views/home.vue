<script setup lang="js">
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage} from 'element-plus'
import {ORIENT_LANDSCAPE, ORIENT_PORTRAIT, loadConfig, pageOrientationOf, plantState} from '../system/PlantStore.js'

const route = useRoute()

const NAVS = [
    {
        name: 'home_map', path: '/home/map', title: '厂区寻路', sub: '起点 → 磅房 → 装卸点',
        icon: 'M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Z M9 4v14 M15 6v14',
    },
    {
        name: 'home_gb', path: '/home/gb', title: '地磅查询', sub: '过磅日志 · 车牌记录',
        icon: 'M12 3v3 M5 7h14 M6 7l-3.2 7a3.6 3.6 0 0 0 7.2 0L6.8 7Z M17.2 7l-3.2 7a3.6 3.6 0 0 0 7.2 0L17 7Z M12 6v15 M8 21h8',
    },
    {
        name: 'home_setting', path: '/home/setting', title: '配置', sub: '磅房绑定 · 地图配置',
        icon: 'M4 7h5 M13 7h7 M4 17h9 M17 17h3 M11 4v6 M6 14v6',
    },
]

const activeName = computed(() => route.name)

/*
 * 切页显示方向：每个切页在「配置 / 磅房与绑定」里配一档（nav.page_display.orientation）——
 * 0 跟随手机横竖屏、1 始终横屏、2 始终竖屏。
 * Web 端锁不住系统方向，1 档的做法是：视口竖着时把整层壳旋转 90° 当成横屏画布（横过来即可正向读）；
 * 0 / 2 档都不旋转，区别只在页面自己用哪套版式（见 map.vue 的 landscapeLayout）。
 */
const portraitMq = window.matchMedia('(orientation: portrait)')
// 车机式外壳的原始判据：真横屏且高度很矮（手机横放）
const shortMq = window.matchMedia('(orientation: landscape) and (max-height: 560px)')
// 窄屏手机版式的原始判据（原先写死在 @media (max-width:760px)）
const narrowMq = window.matchMedia('(max-width: 760px)')
const portraitViewport = ref(portraitMq.matches)
const shortLandscape = ref(shortMq.matches)
const narrowViewport = ref(narrowMq.matches)

function onViewportChange(event) {
    if (event.media === portraitMq.media) portraitViewport.value = event.matches
    else if (event.media === shortMq.media) shortLandscape.value = event.matches
    else if (event.media === narrowMq.media) narrowViewport.value = event.matches
}

const activeOrientation = computed(() => pageOrientationOf(activeName.value))
// 竖着拿且本页「始终横屏」→ 整层壳旋转 90° 当横屏画布
const forceLandscape = computed(() => activeOrientation.value === ORIENT_LANDSCAPE && portraitViewport.value)
// 横着拿且本页「始终竖屏」→ 反过来旋转 -90°，把横视口当成竖画布（否则会退化成桌面版式塞进矮视口）
const forcePortrait = computed(() => activeOrientation.value === ORIENT_PORTRAIT && !portraitViewport.value)
/*
 * 是否套用车机式横屏外壳（顶栏压扁 + 左侧图标条）：
 * 2 始终竖屏 → 永远不套；竖屏视口下只有 1 始终横屏 才套（配合旋转）；
 * 横屏视口下按原判据（高度 ≤560 的手机横放），大屏仍走普通外壳。
 */
const carShell = computed(() => {
    if (activeOrientation.value === ORIENT_PORTRAIT) return false
    if (portraitViewport.value) return activeOrientation.value === ORIENT_LANDSCAPE
    return shortLandscape.value
})
/*
 * 是否套用窄屏手机外壳（顶栏压扁 + 导航横排）：规则原先写死在 @media (max-width:760px) 里，
 * 改成类名是为了照顾两种「画布宽度 ≠ 视口宽度」的强制旋转——媒体查询看到的永远是视口。
 * 横屏外壳（carShell）和旋转横屏（forceLandscape）各自带版式，不叠加 narrow。
 */
const narrowShell = computed(() => {
    if (forceLandscape.value || carShell.value) return false
    return forcePortrait.value || narrowViewport.value
})

// 地磅值班：跳转到「磅房与绑定」页配置的网页链接
onMounted(() => {
    portraitMq.addEventListener('change', onViewportChange)
    shortMq.addEventListener('change', onViewportChange)
    loadConfig()
})

onBeforeUnmount(() => {
    portraitMq.removeEventListener('change', onViewportChange)
    shortMq.removeEventListener('change', onViewportChange)
})

function openDuty() {
    const url = (plantState.plant.dutyUrl || '').trim()
    if (!url) {
        ElMessage.warning('尚未配置地磅值班链接，请在「配置」页顶部设置')
        return
    }
    window.open(url, '_blank')
}
</script>

<template>
    <div class="layout"
         :class="{'is-map': activeName === 'home_map', 'force-land': forceLandscape, 'force-port': forcePortrait, 'is-car': carShell, narrow: narrowShell}">
        <header class="topbar">
            <div class="brand">
                <span class="brand-mark">🗺</span>
                <div class="brand-text">
                    <div class="brand-title">厂区车辆寻路</div>
                    <div class="brand-sub">氯碱厂区 · 导航</div>
                </div>
            </div>
            <div class="topbar-right">
                <button class="duty-btn" @click="openDuty">
                    <svg class="duty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                         stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="10" cy="7.5" r="3.2"/>
                        <path d="M4 20c.7-3.6 3-5.6 6-5.6s5.3 2 6 5.6"/>
                        <circle cx="17.5" cy="7" r="2.6"/>
                        <path d="M15.8 12.3c.5-.2 1.1-.3 1.7-.3 2.2 0 3.9 1.4 4.5 4"/>
                    </svg>
                    <span class="duty-text">地磅值班</span>
                </button>
            </div>
        </header>

        <div class="body">
            <aside class="sidenav">
                <router-link v-for="item in NAVS" :key="item.name" :to="item.path" class="sidenav-item"
                             :class="{active: activeName === item.name}">
                    <div class="item-inner">
                        <svg class="item-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                             stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                            <path :d="item.icon"/>
                        </svg>
                        <div class="item-text">
                            <span class="item-title">{{ item.title }}</span>
                            <span class="item-sub">{{ item.sub }}</span>
                        </div>
                    </div>
                </router-link>
            </aside>

            <main class="main">
                <router-view/>
            </main>
        </div>
    </div>
</template>

<style scoped>
.layout {
    display: flex;
    flex-direction: column;
    min-height: 100%;
    background: #f1f5f9;
}

.topbar {
    display: flex;
    align-items: center;
    gap: 20px;
    height: 60px;
    padding: 0 20px;
    background: #ffffff;
    border-bottom: 1px solid #e2e8f0;
    flex-shrink: 0;
}

.brand {
    display: flex;
    align-items: center;
    gap: 10px;
}

.brand-mark {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 8px;
    background: linear-gradient(135deg, #1c4785, #1677ff);
    color: #ffffff;
    font-size: 20px;
}

.brand-title {
    font-size: 18px;
    font-weight: 700;
    color: #0f172a;
    line-height: 1.25;
}

.brand-sub {
    font-size: 11px;
    color: #94a3b8;
}

.topbar-right {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 14px;
}

/* 地磅值班跳转按钮 */
.duty-btn {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    height: 34px;
    padding: 0 16px;
    border: none;
    border-radius: 999px;
    background: linear-gradient(135deg, #ff9f43, #ff7a1f);
    color: #ffffff;
    font-size: 13px;
    font-weight: 600;
    letter-spacing: .5px;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(255, 122, 31, .32);
    transition: transform .15s ease, box-shadow .15s ease, filter .15s ease;
}

.duty-btn:hover {
    transform: translateY(-1px);
    box-shadow: 0 7px 18px rgba(255, 122, 31, .42);
    filter: brightness(1.06);
}

.duty-btn:active {
    transform: translateY(0);
    filter: brightness(.97);
}

.duty-icon {
    width: 17px;
    height: 17px;
    flex: 0 0 17px;
}

.body {
    display: flex;
    flex: 1;
    overflow: hidden;
}

.sidenav {
    width: 220px;
    flex-shrink: 0;
    background: #0f172a;
    padding: 16px 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.sidenav-item {
    display: block;
    border-radius: 8px;
    text-decoration: none;
    color: #cbd5e1;
    transition: background .15s ease, color .15s ease;
}

.sidenav-item:hover {
    background: #1e293b;
}

.sidenav-item.active {
    background: #1677ff;
    color: #ffffff;
}

.item-inner {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 11px 14px;
    line-height: 1.4;
}

.item-icon {
    width: 18px;
    height: 18px;
    flex: 0 0 18px;
}

.item-text {
    display: flex;
    flex-direction: column;
}

.item-title {
    display: block;
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 1px;
}

.item-sub {
    display: block;
    font-size: 11px;
    color: #94a3b8;
}

.sidenav-item.active .item-sub {
    color: #bfdbfe;
}

.main {
    flex: 1;
    /* flex 子项默认 min-height:auto：内容高于外壳时会把 .main 撑破，
       于是 overflow-y:auto 永远没有可滚空间、超出的部分直接被裁掉（横屏矮画布必踩） */
    min-height: 0;
    overflow-y: auto;
    padding: 16px 20px 24px;
}

/* 窄屏手机外壳（.narrow）：左侧导航改为顶部横向标签，把宽度全部留给内容。
 * 由 narrowShell 计算（原 @media max-width:760px 只认视口宽度，识别不了强制旋转后的窄画布）。 */
.layout.narrow .topbar {
    height: 52px;
    gap: 10px;
    padding: 0 12px;
}

.layout.narrow .brand-mark {
    width: 30px;
    height: 30px;
    border-radius: 7px;
    font-size: 16px;
}

.layout.narrow .brand-title {
    font-size: 15px;
}

.layout.narrow .brand-sub {
    display: none;
}

.layout.narrow .duty-btn {
    height: 30px;
    padding: 0 12px;
    font-size: 12px;
    gap: 5px;
}

.layout.narrow .duty-icon {
    width: 15px;
    height: 15px;
    flex: 0 0 15px;
}

.layout.narrow .body {
    flex-direction: column;
    overflow: visible;
}

.layout.narrow .sidenav {
    width: 100%;
    flex-direction: row;
    gap: 4px;
    padding: 8px;
    overflow-x: auto;
    position: sticky;
    top: 0;
    z-index: 5;
}

.layout.narrow .sidenav-item {
    flex: 1 0 auto;
}

.layout.narrow .item-inner {
    justify-content: center;
    gap: 6px;
    padding: 10px 12px;
}

.layout.narrow .item-icon {
    width: 16px;
    height: 16px;
    flex: 0 0 16px;
}

.layout.narrow .item-title {
    margin-bottom: 0;
    font-size: 13px;
}

.layout.narrow .item-sub {
    display: none;
}

.layout.narrow .main {
    padding: 10px 10px 18px;
}


/* 小屏手机（≤360px）：三个标签收窄，保证一屏内全部可见 */
@media (max-width: 360px) {
    .duty-btn {
        padding: 0 9px;
        gap: 4px;
    }

    .topbar-right {
        gap: 8px;
    }
    .item-inner {
        gap: 4px;
        padding: 9px 8px;
    }

    .item-icon {
        display: none;
    }

    .item-title {
        font-size: 12px;
    }
}

/* ===== 横屏 / 竖屏锁定：车机式外壳 =====
 * 高度只有 390 上下，顶栏压到 42px，导航从顶部横排改为左侧图标条，
 * 内容区完全交给 map.vue 的导航布局（那张页面会 fixed 铺满，用 --map-top/--map-left 对齐）。
 * ⚠️ 触发条件不再写在媒体查询里：切页显示方向可配置后，「竖屏视口也用横屏版式」只对
 *    「始终横屏」的切页成立，媒体查询表达不了 ⇒ 改由 .layout.is-car 类名驱动（见 carShell）。
 */
/* ===== 车机式横屏外壳：由 .layout.is-car 驱动（JS 按切页配置 + 视口判定，见 carShell） ===== */
.layout.is-car {
    /* 地图卡片高度按「可视高 - 顶栏 - 上下留白」算；旋转场景由末尾的强制横屏块改写 */
    --vp-h: 100dvh;
}

/* 横屏车机壳一律钉死在视口：
 * ① 寻路页靠 `.main` 里的地址栏滚距把地址栏收起来，静态定位的顶栏/导航会被一起滚走（实测顶栏被顶出屏幕 60px）；
 * ② 其余切页（地磅查询、配置）若不钉死，`.layout` 只有 min-height:100%、会被内容撑高，
 *    `.main` 跟着内容长高 ⇒ overflow-y:auto 永无可滚空间，多出来的部分要么被裁要么整页滚走顶栏。 */
.layout.is-car {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    min-height: 0;
}

.layout.is-car .topbar {
    height: 42px;
    gap: 10px;
    padding: 0 10px;
}

/* 横屏矮画布（351px 内容高）每一像素都要留给内容：默认 16/20/24 的外留白占掉 40px。
 * 寻路页另有更紧的 padding:0（地图要贴边），见下方 .is-map 规则。 */
.layout.is-car .main {
    padding: 10px 12px 12px;
}

.layout.is-car .brand-mark {
    width: 26px;
    height: 26px;
    border-radius: 7px;
    font-size: 15px;
}

.layout.is-car .brand-title {
    font-size: 14px;
    line-height: 1.2;
}

.layout.is-car .brand-sub {
    display: none;
}

.layout.is-car .duty-btn {
    height: 28px;
    padding: 0 10px;
    font-size: 12px;
    gap: 5px;
}

.layout.is-car .duty-icon {
    width: 14px;
    height: 14px;
    flex: 0 0 14px;
}

.layout.is-car .body {
    flex-direction: row;
    overflow: hidden;
}

/* 导航收成 64px 图标条，宽度全部让给地图 */
.layout.is-car .sidenav {
    width: 64px;
    flex-direction: column;
    gap: 6px;
    padding: 6px;
    overflow: visible;
    position: static;
    z-index: auto;
}

.layout.is-car .sidenav-item {
    flex: 0 0 auto;
}

.layout.is-car .item-inner {
    flex-direction: column;
    justify-content: center;
    gap: 2px;
    padding: 8px 2px;
}

.layout.is-car .item-icon {
    width: 18px;
    height: 18px;
    flex: 0 0 18px;
}

.layout.is-car .item-title {
    font-size: 10px;
    margin-bottom: 0;
    letter-spacing: 0;
    white-space: nowrap;
}

.layout.is-car .item-sub {
    display: none;
}

/* ⚠️ 只有寻路页要「地图贴边 + 由地图卡自己滚」。其余切页必须沿用 .main 的默认
 * padding + overflow-y:auto —— 否则车机外壳（is-car）会把地磅查询等页面的滚动一并关掉，
 * 内容超出 351px 可视高就只能被裁掉、滚不动。 */
.layout.is-car.is-map .main {
    padding: 0;
    overflow: hidden;
}

/* ===== 视口竖着 + 当前切页配了「始终横屏」：整层壳旋转 90° 当横屏画布 =====
 * 是否旋转由配置决定（nav.page_display.orientation = 1），不再写死某个页面：
 * 唯一判据是**视口本身是不是竖的**（宽 < 高），跟屏幕多大、系统方向锁定都无关。
 * 竖着拿时把整层壳旋转 90°：393×852 的竖屏 → 852×393 的横屏画布，横过来即可正向阅读。
 *
 * ⚠️ 几何：宽取视口高（100dvh/dvw 跟随地址栏收放）、高取视口宽，transform-origin 左上，
 *    rotate(90deg) 后再 translateY(-100%) 把旋转后的盒子平移回可视区。
 * ⚠️ 旋转（transform）会让 .layout 成为 fixed 后代的包含块：map.vue 里 .page 的
 *    position:fixed + --map-top/--map-left 全部落在旋转后的坐标系里，无需换算；
 *    但 100vh / 100dvh 这类视口单位不会跟着转，所以 --vp-h 要改成 100dvw（旋转后的「高度」= 视口宽度）。
 * ⚠️ min-height 必须归零：基础样式里 .layout{min-height:100%}（= 竖屏视口高 852）会顶开
 *    height:100dvw(393)，不归零的话旋转后的画布尺寸直接错位。
 * ⚠️ 只有 .force-land（配置成「始终横屏」且视口是竖的）才旋转：其余页面保持普通竖屏版式。
 */
.layout.force-land {
    --vp-h: 100dvw;
    position: fixed;
    top: 0;
    left: 0;
    width: 100vh;
    height: 100vw;
    width: 100dvh;
    height: 100dvw;
    min-height: 0;
    transform-origin: 0 0;
    transform: rotate(90deg) translateY(-100%);
    overflow: hidden;
}

/* ===== 视口横着 + 当前切页配了「始终竖屏」：反向旋转 -90° 当竖画布 =====
 * 与 .force-land 对称：横屏视口下不旋转的话，页面会按视口宽度走成桌面版式
 * （左导航 + 852 宽内容塞进 393 高），既不是「竖屏展示」也留不住地图。
 *
 * ⚠️ 几何：宽取视口高、高取视口宽（即一块 393×852 的竖画布），transform-origin 左上，
 *    R(-90) 把盒子甩到视口上方（y ∈ [-852, 0]），translateX(-100%) 沿本地 x 轴左移一个宽，
 *    旋转后正好下移 852，落回 [0, 852]×[0, 393]。
 * ⚠️ 配套：窄屏版式必须走 .layout.narrow / .page.narrow 类名（媒体查询看到的是 852 宽视口）。
 */
.layout.force-port {
    --vp-h: 100dvw;
    position: fixed;
    top: 0;
    left: 0;
    width: 100vh;
    height: 100vw;
    width: 100dvh;
    height: 100dvw;
    min-height: 0;
    transform-origin: 0 0;
    transform: rotate(-90deg) translateX(-100%);
    overflow: hidden;
}

/* 定高壳（旋转壳 / 钉死在视口的寻路页）里的 .body 必须允许收缩：
 * flex 子项默认 min-height:auto 会被内容撑破壳高，.main 于是长得跟内容一样高、
 * overflow-y:auto 永远没有可滚空间（表现就是「内容被裁且滚不动」）。 */
.layout.is-car .body,
.layout.force-land .body,
.layout.force-port .body {
    min-height: 0;
}
</style>
