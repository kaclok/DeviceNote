import {createApp} from 'vue'
import App from './App.vue'

import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'dayjs/locale/zh-cn'
import VConsole from 'vconsole'

// 本入口的全局样式：el-select 下拉面板 teleport 到 body，scoped 样式命中不到
import './styles/plantmap.css'
// ElMessage / ElMessageBox 由 JS 显式调用，按需插件不会自动注入它们的样式
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'

import {RegisterDirective} from '@/framework/directives/DirectiveList.js'
import {Switch} from '@/framework/services/LocaleService.js'
import {router} from './router/Index.js'

const app = createApp(App)

if (__DEV__) {
    // 开发环境引入手机浏览器的开发者工具
    new VConsole()
}

app.config.errorHandler = (error, instance, info) => {
    console.error(error, instance, info)
}
app.config.warnHandler = (msg, instance, trace) => {
    console.warn(msg, instance, trace)
}
app.config.performance = true

async function setupAll() {
    await Switch(app, import.meta.env.VITE_LOCALE)

    app.use(ElementPlus, {
        locale: zhCn,
    })

    RegisterDirective(app)
    app.use(router)

    // mount 必须放在最后
    app.mount('#app')
}

setupAll().catch((e) => {
    console.error(e)
})
