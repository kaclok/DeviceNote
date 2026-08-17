import {createRouter, createWebHashHistory} from 'vue-router'
import {PREFIX, routers} from './Router.js'
import {useCache, ECacheType, clearAccount} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useCache()

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
    const perms = wsCache.get(ECacheType.PERMS);
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
    const perms = to.meta?.perms;
    if (!perms || perms.length === 0) {
        return true;
    }
    const auth = getPerms();
    return perms.some(p => auth.includes(p));
}

/**
 * 查找当前用户有权限访问的第一个 home 子路由（登录后的兜底页面）
 */
function findFirstAccessibleRoute() {
    const home = routers.find(r => r.path === '/home');
    if (!home?.children) {
        return null;
    }

    for (const child of home.children) {
        if (!child.path || child.name === 'home_default') {
            continue;
        }
        const perms = child.meta?.perms || [];
        const auth = getPerms();
        if (perms.length === 0 || perms.some(p => auth.includes(p))) {
            return child;
        }
    }
    return null;
}

/**
 * 清除登录态
 */
function clearLogin() {
    clearAccount()
}

router.beforeEach((to, current, next) => {
    let hasLoggedIn = !!wsCache.get(ECacheType.ACCOUNT)

    // 如果用户已登录且尝试访问登录页，重定向到台账页
    if (to.name === 'login' && hasLoggedIn) {
        const fallback = findFirstAccessibleRoute();
        next(fallback ? {name: fallback.name} : {name: 'login'});
        return;
    }

    // 未登录
    if (!hasLoggedIn) {
        // 未登录且尝试访问其他页面，重定向到登录页
        if (to.name !== 'login') {
            next({name: 'login'});
        }
        // 未登录并且准备登录，正常执行
        else {
            next();
        }
    }
    // 已登录
    else {
        // 在登录页准备继续登录
        if (to.name === 'login') {
            const fallback = findFirstAccessibleRoute();
            next(fallback ? {name: fallback.name} : {name: 'login'});
        }
        // 已登录准备跳转其他页面
        else {
            // 路由级权限控制：无权限直接拦截，防止手动改 URL 绕过
            if (canAccess(to)) {
                next();
            } else {
                console.warn(`无权限访问路由: ${to.fullPath}, 所需权限: ${JSON.stringify(to.meta?.perms)}`);
                const fallback = findFirstAccessibleRoute();
                if (fallback) {
                    // 重定向到当前用户有权限的第一个页面
                    next({name: fallback.name});
                } else {
                    // 用户无任何页面权限，登出
                    clearLogin();
                    next({name: 'login'});
                }
            }
        }
    }
});

export {
    router,
}
