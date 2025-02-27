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

export {
    router,
}