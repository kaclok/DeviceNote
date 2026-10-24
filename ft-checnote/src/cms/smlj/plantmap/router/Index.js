import {createRouter, createWebHashHistory} from 'vue-router'
import {PREFIX, routers} from './Router.js'

// 本入口没有登录鉴权，路由守卫只保留滚动复位
const router = createRouter({
    history: createWebHashHistory(PREFIX),
    strict: true,
    routes: routers,
    scrollBehavior(to, from, savedPosition) {
        return savedPosition || {left: 0, top: 0}
    },
})

export {router}
