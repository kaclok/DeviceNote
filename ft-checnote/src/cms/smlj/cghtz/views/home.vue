<script setup lang="js">
import {useRouter, useRoute} from 'vue-router';
import {useCache, ECacheType, clearAccount} from "@/framework/composable/use/useCache.ts";

const router = useRouter();
const route = useRoute();
const {wsCache} = useCache()

// 空值兜底：未登录或缓存被清时避免 .includes / .realName 报错
const acc = wsCache.get(ECacheType.ACCOUNT);
const account = ref(acc)
const perms = ref(acc.role.perms)

// 权限判断
function hasPerm(code) {
    return perms.value.includes(code)
}

// 侧边菜单（按权限过滤）
const menus = computed(() => {
    const list = [
        {path: '/home/ledger', title: '合同台账', icon: '📋', perm: 'contract:view'},
        {path: '/home/import', title: '批量导入', icon: '📥', perm: 'contract:import'},
        {path: '/home/users', title: '账号与权限', icon: '👥', perm: 'perm:assign'},
    ]
    return list.filter(m => hasPerm(m.perm))
})

// 当前激活菜单：响应式绑定 route.path，路由变化时菜单高亮自动跟随
const activeMenu = computed(() => route.path)

function logout() {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {type: 'warning'}).then(() => {
        clearAccount()
        router.push({name: 'login'})
    }).catch(() => {
    })
}
</script>

<template>
    <div class="page-container">
        <!-- 顶栏 -->
        <div class="page-title">
            <div class="logo">📑</div>
            <span class="page-title-content">合同台账管理系统</span>

            <div class="page-title-sub">陕西金泰化学神木氯碱</div>

            <div class="right-menu">
                <el-dropdown @command="logout">
                    <div class="user-info">
                        <el-avatar :size="30" style="background:#6366f1;font-size:13px">
                            {{ (account.realName || '?').slice(0, 1) }}
                        </el-avatar>
                        <span class="user-name">{{ account.realName || account.account }}</span>
                    </div>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>

        <!-- 主体：侧边菜单 + 内容 -->
        <div class="page-body">
            <div class="sidebar">
                <el-menu :default-active="activeMenu" router background-color="#0f172a"
                         text-color="#94a3b8" active-text-color="#ffffff" :unique-opened="true">
                    <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
                        <span class="menu-icon">{{ m.icon }}</span>
                        <span>{{ m.title }}</span>
                    </el-menu-item>
                </el-menu>
                <div class="sidebar-footer">v0.1.0</div>
            </div>

            <div class="page-content">
                <router-view/>
            </div>
        </div>
    </div>
</template>

<style lang="scss" scoped>
.page-container {
    width: 100%;
    height: 100%;
    background-color: #f1f5f9;
    display: flex;
    flex-direction: column;

    .page-title {
        width: 100%;
        height: 56px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        background-color: #ffffff;
        border-bottom: 1px solid #e2e8f0;
        padding: 0 20px;

        .logo {
            font-size: 22px;
            margin-right: 10px;
        }

        .page-title-content {
            color: #0f172a;
            font-size: 17px;
            font-weight: 700;
        }

        .page-title-sub {
            margin-left: 16px;
            padding-left: 16px;
            border-left: 1px solid #e2e8f0;
            color: #94a3b8;
            font-size: 12px;
        }

        .right-menu {
            margin-left: auto;
            display: flex;
            align-items: center;
            gap: 18px;

            .user-info {
                display: flex;
                align-items: center;
                gap: 8px;
                cursor: pointer;
                padding: 4px 8px;
                border-radius: 8px;
                transition: background .2s;

                &:hover {
                    background: #f8fafc;
                }

                .user-name {
                    font-size: 14px;
                    font-weight: 500;
                }

                .user-role {
                    font-size: 11px;
                    color: #2563eb;
                    background: #eff6ff;
                    padding: 1px 8px;
                    border-radius: 999px;
                }
            }
        }
    }

    .page-body {
        flex: 1;
        display: flex;
        overflow: hidden;

        .sidebar {
            width: 200px;
            flex-shrink: 0;
            background-color: #0f172a;
            display: flex;
            flex-direction: column;

            :deep(.el-menu) {
                border-right: none;
                flex: 1;

                .el-menu-item {
                    height: 46px;
                    margin: 2px 8px;
                    border-radius: 8px;

                    &.is-active {
                        background: linear-gradient(90deg, #2563eb, #3b82f6) !important;
                    }

                    .menu-icon {
                        margin-right: 10px;
                        font-size: 15px;
                    }
                }
            }

            .sidebar-footer {
                padding: 14px;
                font-size: 11px;
                color: #475569;
                text-align: center;
            }
        }

        .page-content {
            flex: 1;
            overflow-y: auto;
            padding: 20px 24px 40px;
        }
    }
}
</style>
