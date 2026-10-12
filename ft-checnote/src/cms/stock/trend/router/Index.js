import {createRouter, createWebHashHistory} from 'vue-router'
import {PREFIX, routers} from './Router.js'

// 创建路由实例
const router = createRouter({
    history: createWebHashHistory(PREFIX), // 设置正确的基础路径
    strict: true,
    routes: routers,
    scrollBehavior(to, from, savedPosition) {
        return savedPosition || {left: 0, top: 0}
    },
})

export {
    router,
}
