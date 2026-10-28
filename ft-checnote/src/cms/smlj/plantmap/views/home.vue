<script setup lang="js">
import {computed, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage} from 'element-plus'
import {loadConfig, plantState} from '../system/PlantStore.js'

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

// 地磅值班：跳转到「磅房与绑定」页配置的网页链接
onMounted(loadConfig)

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
    <div class="layout" :class="{'is-map': activeName === 'home_map'}">
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
    overflow-y: auto;
    padding: 16px 20px 24px;
}

/* 竖屏手机：左侧导航改为顶部横向标签，把宽度全部留给内容 */
@media (max-width: 760px) {
    .topbar {
        height: 52px;
        gap: 10px;
        padding: 0 12px;
    }

    .brand-mark {
        width: 30px;
        height: 30px;
        border-radius: 7px;
        font-size: 16px;
    }

    .brand-title {
        font-size: 15px;
    }

    .brand-sub {
        display: none;
    }

    .duty-btn {
        height: 30px;
        padding: 0 12px;
        font-size: 12px;
        gap: 5px;
    }

    .duty-icon {
        width: 15px;
        height: 15px;
        flex: 0 0 15px;
    }

    .body {
        flex-direction: column;
        overflow: visible;
    }

    .sidenav {
        width: 100%;
        flex-direction: row;
        gap: 4px;
        padding: 8px;
        overflow-x: auto;
        position: sticky;
        top: 0;
        z-index: 5;
    }

    .sidenav-item {
        flex: 1 0 auto;
    }

    .item-inner {
        justify-content: center;
        gap: 6px;
        padding: 10px 12px;
    }

    .item-icon {
        width: 16px;
        height: 16px;
        flex: 0 0 16px;
    }

    .item-title {
        margin-bottom: 0;
        font-size: 13px;
    }

    .item-sub {
        display: none;
    }

    .main {
        padding: 10px 10px 18px;
    }
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
 * ⚠️ 条件里多带了「竖屏」这一整支，且**不带宽度门槛**：寻路页要求「不受操作系统横屏/竖屏控制，
 *    永远横屏展示」，竖着拿时整页会旋转 90°（见文件末尾「强制横屏」块）；旋转后视口仍是竖屏、
 *    媒体查询不认旋转，所以竖屏这一支必须把同一套参数一起给出，宽度多大的竖屏都一样。
 */
@media (orientation: landscape) and (max-height: 560px), (orientation: portrait) {
    .layout {
        /* 地图卡片高度按「可视高 - 顶栏 - 上下留白」算；旋转场景由末尾的强制横屏块改写 */
        --vp-h: 100dvh;
    }

    /* 寻路页的壳钉死在视口：地址栏收起那套「往文档底部垫滚距再滚过去」的操作
       会把静态定位的顶栏/导航一起滚走（实测顶栏被顶出屏幕 60px），钉住后文档滚动不影响观感 */
    .layout.is-map {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        min-height: 0;
    }

    .topbar {
        height: 42px;
        gap: 10px;
        padding: 0 10px;
    }

    .brand-mark {
        width: 26px;
        height: 26px;
        border-radius: 7px;
        font-size: 15px;
    }

    .brand-title {
        font-size: 14px;
        line-height: 1.2;
    }

    .brand-sub {
        display: none;
    }

    .duty-btn {
        height: 28px;
        padding: 0 10px;
        font-size: 12px;
        gap: 5px;
    }

    .duty-icon {
        width: 14px;
        height: 14px;
        flex: 0 0 14px;
    }

    .body {
        flex-direction: row;
        overflow: hidden;
    }

    /* 导航收成 64px 图标条，宽度全部让给地图 */
    .sidenav {
        width: 64px;
        flex-direction: column;
        gap: 6px;
        padding: 6px;
        overflow: visible;
        position: static;
        z-index: auto;
    }

    .sidenav-item {
        flex: 0 0 auto;
    }

    .item-inner {
        flex-direction: column;
        justify-content: center;
        gap: 2px;
        padding: 8px 2px;
    }

    .item-icon {
        width: 18px;
        height: 18px;
        flex: 0 0 18px;
    }

    .item-title {
        font-size: 10px;
        margin-bottom: 0;
        letter-spacing: 0;
        white-space: nowrap;
    }

    .item-sub {
        display: none;
    }

    .main {
        padding: 0;
        overflow: hidden;
    }
}

/* ===== 竖屏视口：厂区寻路页强制横屏 =====
 * 寻路页是车机式横屏版式，要求「无论操作系统开关是横屏还是竖屏，页面始终横屏展示」——
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
 * ⚠️ 只作用于寻路页（.is-map）：地磅查询、配置页仍是普通竖屏页面。
 */
@media (orientation: portrait) {
    .layout.is-map {
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
}
</style>
