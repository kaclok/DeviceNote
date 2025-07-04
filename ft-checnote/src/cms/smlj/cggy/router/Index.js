import {createRouter, createWebHashHistory, createWebHistory} from 'vue-router'
import {routers, PREFIX} from './Router.js'
import {LocalStorageService} from "@/framework/services/LocalStorageService.js";

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

/*if (__DEV__) {
    // 创建历史队列
    const historyQueue = []
    window.historyQueue = historyQueue

    router.afterEach((to, from) => {
        historyQueue.push({
            from: from.fullPath,
            to: to.fullPath,
            timestamp: new Date().getTime()
        })
    })
}*/

router.beforeEach((to, current, next) => {
    let isLoggedIn = LocalStorageService.getStore("account") !== null
    if (to.name === 'login' && isLoggedIn) {
        // 如果用户已登录且尝试访问登录页，重定向到记录页
        next({name: 'home'});
    } /*else if (to.name === 'home' && !isLoggedIn) {
        // 如果用户未登录且尝试访问记录页，重定向到登录页
        next({name: 'login'});
    }*/ else {
        next();
    }
});

export {
    router,
}