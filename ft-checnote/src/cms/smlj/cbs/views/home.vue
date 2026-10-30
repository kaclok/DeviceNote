<script setup lang="js">
import {useRoute, useRouter} from 'vue-router';
import {clearAccount, ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";
import {ElMessageBox} from "element-plus";
import {Files, Setting, OfficeBuilding, EditPen, Search, SwitchButton, User} from "@element-plus/icons-vue";
import {clearDictCache} from "@/cms/smlj/cbs/system/SysX.js";

const router = useRouter();
const route = useRoute();
const {wsCache} = useSessionCache()

// 空值兜底：未登录或缓存被清时避免 .includes / .username 报错
const acc = wsCache.get(ECacheType.ACCOUNT);
const account = ref(acc)
// ⚠️ jackson NON_EMPTY：perms 空数组时字段整个消失，须兜底 []
const perms = ref(acc?.role?.perms || [])

function hasPerm(code) {
    return perms.value.includes(code)
}

// ============ 总览（登录落地页，对齐目标站 dashboard） ============
const MODULES = computed(() => [
    {title: '特殊作业报备', desc: '在线抽查', path: '/home/spotcheck', color: '#a05260', icon: Files, perm: 'cbs:spotcheck'},
    {title: '基础设置', desc: '企业信息 / 证照预警', path: '/home/baseinfo/ent', color: '#8a7a6b', icon: Setting, perm: 'cbs:baseinfo'},
    {title: '承包商', desc: '单位管理 / 项目管理', path: '/home/contractor/unit', color: '#8e6cc6', icon: OfficeBuilding, perm: 'cbs:contractor'},
    {title: '安全承诺', desc: '每日安全承诺', path: '/home/promise', color: '#3ba99c', icon: EditPen, perm: 'cbs:promise'},
].filter(m => hasPerm(m.perm)))

// ============ 分组侧边菜单（对齐目标站菜单树：一个菜单项 = 一个页面） ============
// 结构：module → [子分组] → 页面。子分组用 el-sub-menu 渲染，可点击收起/展开。
const MENU_TREE = [
    {title: '特殊作业报备', icon: Files, path: '/home/spotcheck', perm: 'cbs:spotcheck'},
    {
        title: '基础设置', icon: Setting, children: [
            {path: '/home/baseinfo/ent', title: '企业基本信息', perm: 'cbs:baseinfo'},
            {path: '/home/baseinfo/intro', title: '企业简介', perm: 'cbs:baseinfo'},
            {path: '/home/baseinfo/ts', title: '安全"三同时"', perm: 'cbs:baseinfo'},
            {path: '/home/baseinfo/chem', title: '许可的危险化学品', perm: 'cbs:baseinfo'},
            {path: '/home/baseinfo/proc', title: '重点监管的危险化工工艺', perm: 'cbs:baseinfo'},
            {path: '/home/baseinfo/cert', title: '证照预警', perm: 'cbs:baseinfo'},
            {path: '/home/org/role', title: '角色管理', perm: 'cbs:org'},
            {path: '/home/org/dept', title: '部门管理', perm: 'cbs:org'},
            {path: '/home/org/post', title: '岗位管理', perm: 'cbs:org'},
            {path: '/home/org/user', title: '人员管理', perm: 'cbs:org'},
        ]
    },
    {
        title: '承包商', icon: OfficeBuilding, children: [
            {
                title: '承包商', children: [
                    {path: '/home/contractor/unit', title: '承包商管理', perm: 'cbs:contractor'},
                    {path: '/home/contractor/audit', title: '承包商审核', perm: 'cbs:contractor'},
                ]
            },
            {
                title: '项目管理', children: [
                    {path: '/home/project/proj', title: '项目管理', perm: 'cbs:project'},
                    {path: '/home/project/state', title: '项目状态', perm: 'cbs:project'},
                    {path: '/home/project/enter', title: '入场管理', perm: 'cbs:project'},
                    {path: '/home/project/prep', title: '开工准备', perm: 'cbs:project'},
                    {path: '/home/project/accept', title: '项目验收', perm: 'cbs:project'},
                ]
            },
            {
                title: '基础设置', children: [
                    {path: '/home/dict/contractor_type', title: '承包商类型', perm: 'cbs:baseinfo'},
                    {path: '/home/dict/penalty_type', title: '承包商处罚类型', perm: 'cbs:baseinfo'},
                    {path: '/home/dict/project_type', title: '项目类型', perm: 'cbs:baseinfo'},
                ]
            },
        ]
    },
    {title: '安全承诺', icon: EditPen, path: '/home/promise', perm: 'cbs:promise'},
]

/** 按权限递归裁剪；叶子全被裁掉的分组整组消失（避免空菜单） */
function pruneMenu(nodes) {
    const out = []
    for (const n of nodes) {
        if (n.path) {
            if (hasPerm(n.perm)) out.push(n)
            continue
        }
        const kids = pruneMenu(n.children || [])
        if (kids.length) out.push({...n, children: kids})
    }
    return out
}

const MENU = computed(() => pruneMenu(MENU_TREE))

/** 当前路由所在的各级分组，用于首次进入时自动展开 */
const defaultOpeneds = computed(() => {
    const open = []
    const hitLeaf = (n, p) => n.path === p || (n.children || []).some(c => hitLeaf(c, p))
    for (const g of MENU.value) {
        if (!g.children || !hitLeaf(g, route.path)) continue
        open.push(g.title)
        for (const c of g.children) {
            if (c.children && hitLeaf(c, route.path)) open.push(g.title + '/' + c.title)
        }
    }
    return open
})

// 当前是否总览页
const isDashboard = computed(() => route.path === '/home')

// 菜单高亮
const activeMenu = computed(() => route.path)

// 顶栏标题：当前所在模块
const moduleTitle = computed(() => {
    const p = route.path
    if (p.startsWith('/home/baseinfo') || p.startsWith('/home/org')) return '基础设置'
    if (p.startsWith('/home/contractor') || p.startsWith('/home/project') || p.startsWith('/home/dict')) return '承包商'
    if (p.startsWith('/home/promise')) return '安全承诺'
    if (p.startsWith('/home/spotcheck')) return '特殊作业报备'
    return '总览'
})

function logout() {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {type: 'warning'}).then(() => {
        // cbs 后端没有 logout 端点（登录态是纯前端会话缓存），退出 = 清缓存回登录页
        clearDictCache()
        clearAccount()
        router.push({name: 'login'})
    }).catch(() => {
    })
}
</script>

<template>
    <!-- ==================== 总览（模块宫格） ==================== -->
    <div v-if="isDashboard" class="dashboard">
        <div class="dash-top">
            <div class="dash-user">
                <el-icon><User/></el-icon>
                <span>下午好，{{ account?.username || account?.account || '' }}</span>
                <span class="dash-logout" @click="logout">
                    <el-icon><SwitchButton/></el-icon>退出登录
                </span>
            </div>
        </div>
        <div class="dash-body">
            <h1 class="dash-title">智慧安全生产管控平台</h1>
            <div class="dash-grid">
                <div v-for="m in MODULES" :key="m.path" class="dash-card"
                     :style="{background: m.color}" @click="router.push(m.path)">
                    <div class="dash-icon"><el-icon :size="26"><component :is="m.icon"/></el-icon></div>
                    <div class="dash-card-text">
                        <div class="dash-card-title">{{ m.title }}</div>
                        <div class="dash-card-desc">{{ m.desc }}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- ==================== 模块布局（深色侧边栏 + 内容） ==================== -->
    <div v-else class="page-container">
        <div class="page-body">
            <div class="sidebar">
                <div class="sidebar-logo" @click="router.push('/home')">
                    <span class="logo-text">智慧安全管控平台</span>
                </div>
                <el-menu :default-active="activeMenu" :default-openeds="defaultOpeneds" router unique-opened
                         class="side-menu">
                    <template v-for="g in MENU" :key="g.title">
                        <!-- 单项 -->
                        <el-menu-item v-if="g.path" :index="g.path">
                            <el-icon><component :is="g.icon"/></el-icon>
                            <span>{{ g.title }}</span>
                        </el-menu-item>
                        <!-- 一级分组 -->
                        <el-sub-menu v-else :index="g.title">
                            <template #title>
                                <el-icon><component :is="g.icon"/></el-icon>
                                <span>{{ g.title }}</span>
                            </template>
                            <template v-for="c in g.children" :key="c.path || c.title">
                                <!-- 二级分组（可点击收起/展开） -->
                                <el-sub-menu v-if="c.children" :index="g.title + '/' + c.title">
                                    <template #title>
                                        <span>{{ c.title }}</span>
                                    </template>
                                    <el-menu-item v-for="d in c.children" :key="d.path" :index="d.path">
                                        {{ d.title }}
                                    </el-menu-item>
                                </el-sub-menu>
                                <!-- 二级页面 -->
                                <el-menu-item v-else :index="c.path">
                                    {{ c.title }}
                                </el-menu-item>
                            </template>
                        </el-sub-menu>
                    </template>
                </el-menu>
            </div>

            <div class="main-area">
                <div class="topbar">
                    <span class="topbar-title">{{ moduleTitle }}</span>
                    <div class="right-menu">
                        <span class="welcome">欢迎您，{{ account?.username || account?.account || '' }}</span>
                        <span class="logout-link" @click="logout">
                            <el-icon><SwitchButton/></el-icon>退出登录
                        </span>
                    </div>
                </div>
                <div class="page-content">
                    <router-view/>
                </div>
            </div>
        </div>
    </div>
</template>

<style lang="scss" scoped>
/* ============ 总览 ============ */
.dashboard {
    width: 100%;
    height: 100%;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    background: radial-gradient(1200px 700px at 30% -10%, #14355f 0%, #0b1d3a 55%, #081527 100%);

    .dash-top {
        height: 56px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: flex-end;
        padding: 0 28px;
        color: #cdd8ea;

        .dash-user {
            display: flex;
            align-items: center;
            gap: 8px;
            font-size: 14px;

            .dash-logout {
                margin-left: 14px;
                display: inline-flex;
                align-items: center;
                gap: 4px;
                cursor: pointer;
                color: #35e0c8;

                &:hover {
                    color: #6ff0de;
                }
            }
        }
    }

    .dash-body {
        flex: 1;
        min-height: 0;
        overflow-y: auto;
        padding: 30px 0 60px;

        .dash-title {
            margin: 10px 0 60px;
            text-align: center;
            color: #ffffff;
            font-size: 44px;
            font-weight: 700;
            letter-spacing: 6px;
            text-shadow: 0 4px 16px rgba(0, 0, 0, .35);
        }

        .dash-grid {
            width: 900px;
            margin: 0 auto;
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 28px;
        }

        .dash-card {
            height: 110px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            gap: 18px;
            padding: 0 26px;
            cursor: pointer;
            box-shadow: 0 6px 18px rgba(0, 0, 0, .25);
            transition: transform .18s, box-shadow .18s;

            &:hover {
                transform: translateY(-4px);
                box-shadow: 0 12px 26px rgba(0, 0, 0, .35);
            }

            .dash-icon {
                width: 52px;
                height: 52px;
                flex-shrink: 0;
                border-radius: 50%;
                border: 2px solid rgba(255, 255, 255, .85);
                display: flex;
                align-items: center;
                justify-content: center;
                color: #ffffff;
            }

            .dash-card-title {
                color: #ffffff;
                font-size: 19px;
                font-weight: 600;
                letter-spacing: 2px;
            }

            .dash-card-desc {
                margin-top: 6px;
                color: rgba(255, 255, 255, .72);
                font-size: 12px;
            }
        }
    }
}

/* ============ 模块布局 ============ */
.page-container {
    width: 100%;
    height: 100%;
    box-sizing: border-box;
    background-color: #eef1f6;
    overflow: hidden;

    .page-body {
        width: 100%;
        height: 100%;
        display: flex;
        overflow: hidden;
        box-sizing: border-box;
        min-height: 0;

        .sidebar {
            width: 220px;
            flex-shrink: 0;
            box-sizing: border-box;
            background: linear-gradient(180deg, #0e1f38 0%, #0b1a30 100%);
            display: flex;
            flex-direction: column;
            overflow-y: auto;
            overflow-x: hidden;

            .sidebar-logo {
                height: 56px;
                flex-shrink: 0;
                display: flex;
                align-items: center;
                padding: 0 18px;
                cursor: pointer;
                border-bottom: 1px solid rgba(255, 255, 255, .08);

                .logo-text {
                    color: #ffffff;
                    font-size: 16px;
                    font-weight: 700;
                    letter-spacing: 1px;
                }
            }

            .side-menu {
                border-right: none;
                background: transparent;
                flex: 1;

                :deep(.el-menu-item) {
                    height: 42px;
                    color: #a7b4c8;

                    &:hover {
                        background: rgba(255, 255, 255, .06);
                        color: #ffffff;
                    }

                    &.is-active {
                        background: #17c0ad !important;
                        color: #ffffff;
                    }
                }

                :deep(.el-sub-menu__title) {
                    height: 46px;
                    color: #dbe3ee;
                    font-weight: 600;

                    &:hover {
                        background: rgba(255, 255, 255, .06);
                    }

                    .el-icon {
                        color: #5fc9bc;
                    }
                }

                :deep(.el-sub-menu .el-menu) {
                    background: rgba(0, 0, 0, .18);
                }

                /* 三级菜单缩进：顶层图标+文字 x≈49，二级分组/页面 52，三级页面 72 */
                :deep(.el-menu--inline > .el-menu-item),
                :deep(.el-menu--inline > .el-sub-menu > .el-sub-menu__title) {
                    padding-left: 52px !important;
                    font-size: 13px;
                }

                :deep(.el-menu--inline > .el-sub-menu > .el-sub-menu__title) {
                    font-weight: 500;
                    color: #b9c6d8;
                }

                :deep(.el-menu--inline .el-menu--inline > .el-menu-item) {
                    padding-left: 72px !important;
                    font-size: 13px;
                }
            }
        }

        .main-area {
            flex: 1;
            min-width: 0;
            display: flex;
            flex-direction: column;
            overflow: hidden;

            .topbar {
                height: 52px;
                flex-shrink: 0;
                display: flex;
                align-items: center;
                background: #ffffff;
                border-bottom: 1px solid #e4e8f0;
                padding: 0 22px;

                .topbar-title {
                    color: #17c0ad;
                    font-size: 14px;
                    padding-bottom: 12px;
                    border-bottom: 2px solid #17c0ad;
                    line-height: 40px;
                }

                .right-menu {
                    margin-left: auto;
                    display: flex;
                    align-items: center;
                    gap: 18px;

                    .welcome {
                        color: #5a6a85;
                        font-size: 13px;
                    }

                    .logout-link {
                        display: inline-flex;
                        align-items: center;
                        gap: 4px;
                        color: #17c0ad;
                        font-size: 13px;
                        cursor: pointer;

                        &:hover {
                            color: #10a797;
                        }
                    }
                }
            }

            .page-content {
                flex: 1;
                box-sizing: border-box;
                overflow-y: auto;
                overflow-x: hidden;
                padding: 18px 22px 40px;
            }
        }
    }
}
</style>
