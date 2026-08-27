import {createRouter, createWebHashHistory} from 'vue-router'
import {PREFIX, routers} from './Router.js'
import {clearAccount, ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";
import {triggerAuthFailure} from "@/framework/services/net/AxiosInst.js";

const {wsCache} = useSessionCache()

let base = import.meta.env.VITE_BASE;
base = PREFIX;

// 创建路由实例
const router = createRouter({
    history: createWebHashHistory(base), // 设置正确的基础路径
    strict: true,
    routes: routers,
    scrollBehavior(to, from, savedPosition) {
        return savedPosition || {left: 0, top: 0}
    },
})

/**
 * 读取当前登录用户的权限码数组
 * 注意：wsCache.get 会自动反序列化（内部 JSON.parse），
 * 存进去的是数组，取出来直接就是数组，不要再做 JSON.parse！
 */
function getPerms() {
    const perms = wsCache.get(ECacheType.ACCOUNT).role.perms;
    if (!perms) {
        return [];
    }
    return Array.isArray(perms) ? perms : [];
}

/**
 * 判断当前用户是否拥有访问某路由的权限
 * 路由未声明 meta.perms 视为公开（如 login / 404）
 */
function canAccess(to) {
    if (!to.meta) {
        return true;
    }
    const perms = to.meta.perms;
    if (!perms || perms.length === 0) {
        return true;
    }
    const auth = getPerms();
    return perms.some(p => auth.includes(p));
}

/**
 * 查找目标路由 to 所在容器路由下，当前用户有权限访问的第一个子路由
 * 实现：to.matched 从根到叶子，从后往前找最后一个带 children 的容器路由，
 * 在其 children 中取第一个有权限的子页；该容器下没有则继续向外层容器回退。
 * 不特化任何路由名（如 home），新增布局路由自动生效。
 * @param to 目标路由对象（守卫 beforeEach 的 to）
 */

/*
const routes = [
    {
        path: '/',
        component: Layout,
        children: [
            {
                path: 'dashboard',
                component: Dashboard,
                children: [
                    {
                        path: 'overview',
                        component: Overview  // 这是最终匹配到的路由
                    }
                ]
            }
        ]
    }
]

to.matched为:
[
    {path: '/', component: Layout, children: [...]},     // index 0: 根路由
    {path: 'dashboard', component: Dashboard, ...},      // index 1: 子路由
    {path: 'overview', component: Overview, ...}         // index 2: 最终叶子路由
]

假设路由结构:
/ (Layout容器)
  ├── dashboard (有权限)
  │   ├── overview (需要 'view' 权限)
  │   └── settings (需要 'admin' 权限)
  └── profile (公开)

  用户访问 /dashboard/overview，但用户没有 'view' 权限：

to.matched = [Layout, dashboard, overview]

从后往前遍历：
i=2：overview 没有 children，跳过
i=1：dashboard 有 children，检查其子路由：
overview：需要 'view' 权限，用户没有 ❌
settings：需要 'admin' 权限，用户没有 ❌
i=0：Layout 有 children，检查其子路由：
dashboard：需要权限，但没有权限？跳过或继续检查
profile：公开权限 ✅ 返回 profile
*/

function findFirstAccessibleRoute(to) {
    const auth = getPerms();
    // 从后往前遍历（从最深的叶子路由往上找）
    for (let i = to.matched.length - 1; i >= 0; i--) {
        const container = to.matched[i];
        // 检查这个路由是否有 children
        if (!container?.children?.length) {
            // 没有 children，说明是叶子路由，跳过
            continue;
        }

        // 找到有 children 的容器路由，遍历其子路由
        for (const child of container.children) {
            // 跳过空 path 的占位路由
            if (!child.path) {
                continue;
            }
            // 如果子路由有权限，返回这个子路由
            const perms = child.meta?.perms || [];
            if (perms.length === 0 || perms.some(p => auth.includes(p))) {
                return child;
            }
        }
    }
    return null;
}

router.beforeEach((to, current, next) => {
    // 对于静态重定向的router定义不会触发beforeEach,只能redirect中打日志
    console.warn('goto ------ current: ' + current.fullPath + ' -> to:', to.fullPath/*, ' 当前hash:', window.location.hash*/)
    const isLoggedIn = !!wsCache.get(ECacheType.ACCOUNT)

    // 未登录：只允许进登录页，同时把原始目标 fullPath 存入 redirect 参数
    if (!isLoggedIn) {
        if (to.name === 'login') {
            next()
        } else {
            next({name: 'login', query: {redirect: to.fullPath}})
        }
        return
    }

    // 下面是isLoggedIn===true的情况

    // 已登录访问登录页：优先跳转 redirect 参数指向的原始目标，否则跳首页
    if (to.name === 'login') {
        const redirect = to.query?.redirect
        if (redirect) {
            next({path: redirect})
        } else {
            next({name: 'home'})
        }
        return
    }

    // 权限拦截：无权限时重定向到目标所在容器下第一个有权限的子页，全无权限则登出
    if (canAccess(to)) {
        next()
        return
    }
    console.warn(`无权限访问路由: ${to.fullPath}, 所需权限: ${JSON.stringify(to.meta?.perms)}`);
    const fallback = findFirstAccessibleRoute(to);
    if (fallback) {
        next({name: fallback.name})
    } else {
        triggerAuthFailure(true);
        next({name: 'login'})
    }
});

export {
    router,
}
