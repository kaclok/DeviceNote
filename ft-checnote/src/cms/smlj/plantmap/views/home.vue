<script setup lang="js">
import {computed} from 'vue'
import {useRoute} from 'vue-router'

const route = useRoute()

const NAVS = [
    {
        name: 'home_map', path: '/home/map', title: '厂区寻路', sub: '起点 → 磅房 → 装卸点',
        icon: 'M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2Z M9 4v14 M15 6v14',
    },
    {
        name: 'home_scales', path: '/home/scales', title: '磅房与绑定', sub: '状态 · 物资换磅',
        icon: 'M12 4v16 M5 7h14 M7 7l-3 7a3.5 3.5 0 0 0 7 0L7 7Z M17 7l-3 7a3.5 3.5 0 0 0 7 0l-3-7',
    },
    {
        name: 'home_config', path: '/home/config', title: '地图配置', sub: '点位 · 道路 · 底图',
        icon: 'M4 7h5 M13 7h7 M4 17h9 M17 17h3 M11 4v6 M6 14v6',
    },
]

const activeName = computed(() => route.name)
</script>

<template>
    <div class="layout">
        <header class="topbar">
            <div class="brand">
                <span class="brand-mark">🗺</span>
                <div class="brand-text">
                    <div class="brand-title">厂区车辆寻路</div>
                    <div class="brand-sub">氯碱厂区 · 导航</div>
                </div>
            </div>
            <span class="topbar-tip">配置存于 device-note · nav</span>
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

.topbar-tip {
    margin-left: auto;
    font-size: 11px;
    color: #94a3b8;
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

    .brand-sub,
    .topbar-tip {
        display: none;
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
</style>
