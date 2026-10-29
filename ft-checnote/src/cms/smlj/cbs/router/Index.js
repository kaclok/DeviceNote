import {createRouter, createWebHashHistory} from 'vue-router'
import {PREFIX, routers} from './Router.js'
import {clearAccount, ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";
import {triggerAuthFailure} from "@/framework/services/net/AxiosInst.js";
import {ensureMe} from "@/cms/smlj/cbs/system/SysX.js";

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
    // 可选链：刷新失败 / 登出清缓存后 ACCOUNT 可能为空，直接 .role.perms 会抛 TypeError
    const perms = wsCache.get(ECacheType.ACCOUNT)?.role?.perms;
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
 * 查找目标路由 to 所在容器路由下，当前用户有权限访问的第一个子路由。
 * 不特化任何路由名（如 home），新增布局路由自动生效。
 */
function findFirstAccessibleRoute(to) {
    const auth = getPerms();
    // 从后往前遍历（从最深的叶子路由往上找）
    for (let i = to.matched.length - 1; i >= 0; i--) {
        const container = to.matched[i];
        if (!container?.children?.length) {
            continue;
        }
        for (const child of container.children) {
            if (!child.path) {
                continue;
            }
            const perms = child.meta?.perms || [];
            if (perms.length === 0 || perms.some(p => auth.includes(p))) {
                return child;
            }
        }
    }
    return null;
}

router.beforeEach(async (to, current, next) => {
    console.warn('goto ------ current: ' + current.fullPath + ' -> to:', to.fullPath)
    const isLoggedIn = !!wsCache.get(ECacheType.ACCOUNT)

    // 未登录：只允许进登录页，同时把原始目标 fullPath 存入 goto 参数
    if (!isLoggedIn) {
        if (to.name === 'login') {
            next()
        } else {
            next({name: 'login', query: {goto: to.fullPath}})
        }
        return
    }

    // 下面是isLoggedIn===true的情况

    // 已登录访问登录页：优先跳转 goto 参数指向的原始目标，否则跳首页
    if (to.name === 'login') {
        const goto = to.query?.goto
        if (goto) {
            next({path: goto})
        } else {
            next({name: 'home'})
        }
        return
    }

    // 已登录：先把「当前用户快照」刷新一遍，再判权限（60s TTL + 在途去重，见 SysX.ensureMe）
    await ensureMe()

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
