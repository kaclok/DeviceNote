import {createRouter, createWebHistory} from 'vue-router'
import {routers} from './Router.js'
import {LocalStorageService} from "@/framework/services/LocalStorageService.js";

// 创建路由实例
const router = createRouter({
    history: createWebHistory(), // createWebHashHistory URL带#，createWebHistory URL不带#
    strict: true,
    routes: routers,
    scrollBehavior: () => ({left: 0, top: 0})
})

if (__DEV__) {
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
}

router.beforeEach((to, current, next) => {
    let isLoggedIn = LocalStorageService.getStore("account") !== null
    if (to.name === 'loginRouter' && isLoggedIn) {
        // 如果用户已登录且尝试访问登录页，重定向到记录页
        next({name: 'recordRouter'});
    } else if (to.name === 'recordRouter' && !isLoggedIn) {
        // 如果用户未登录且尝试访问记录页，重定向到登录页
        next({name: 'loginRouter'});
    } else {
        next();
    }
});


export {
    router,
}