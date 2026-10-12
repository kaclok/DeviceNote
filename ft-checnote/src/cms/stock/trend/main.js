import {createApp} from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'dayjs/locale/zh-cn'

import App from './App.vue'
import {router} from './router/Index.js'
// Element Plus 暗色变量（行情页整体走深色终端风），必须排在自定义样式之前
import 'element-plus/theme-chalk/dark/css-vars.css'
// 全局样式：下拉/日历面板会 teleport 到 body，scoped 样式命中不到
import './styles/trend.css'

document.documentElement.classList.add('dark')

const app = createApp(App)

// 组件渲染与事件处理过程中的错误
app.config.errorHandler = (error, instance, info) => {
    console.error(error, instance, info)
}
app.config.warnHandler = (msg, instance, trace) => {
    console.warn(msg, instance, trace)
}

// 旧的 lazy chunk 加载失败时（部署后旧 HTML 指向已删除的 chunk），自动刷新一次拿新版本
window.addEventListener('vite:preloadError', () => {
    if (sessionStorage.getItem('__trend_reloaded__')) return
    sessionStorage.setItem('__trend_reloaded__', '1')
    window.location.reload()
})

// date-picker 第一列展示为周一
app.use(ElementPlus, {locale: zhCn})
app.use(router)
app.mount('#app')
