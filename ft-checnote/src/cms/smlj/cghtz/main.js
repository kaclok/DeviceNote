import {createApp} from 'vue'
import App from './App.vue'

import ElementPlus from 'element-plus'
import zhCn from "element-plus/es/locale/lang/zh-cn";
import 'dayjs/locale/zh-cn';
import VConsole from 'vconsole';
// 模块级全局样式：下拉类控件字号等（面板 teleport 到 body，scoped 样式命中不到）
import "./styles/cghtz.css";

// import直接引用一个文件时，会执行一遍这个文件，而不获取任何文件对象, 比如：import './lib/init.js';
import {RegisterDirective} from "@/framework/directives/DirectiveList.js";
import {Switch} from "@/framework/services/LocaleService.js";

import "@/framework/services/net/Init.js";
import {changeAuthFailureHandler} from "@/framework/services/net/AxiosInst.js";
import {clearAccount} from "@/framework/composable/use/useCache.ts";
import {router} from "./router/Index.js";

// 注册鉴权失败处理(RT过期/未登录/AT被篡改)：清登录态并跳登录页
// 不在此处调用服务端logout：RT已过期、服务端会话已失效，
// 且logout请求会因AT缺失再次触发鉴权失败导致递归
changeAuthFailureHandler((needLogout) => {
    clearAccount();
    if (router.currentRoute.value.name !== 'login') {
        const cur = router.currentRoute.value.fullPath
        router.push({name: 'login', query: {redirect: cur}});
    }
});

// 上面解决的是"用户刷新时"。还有一种情况： 用户一直停在旧页面上没刷新 ，然后他点了某个菜单 → 旧页面里的旧 JS 去要旧 chunk → 还是没有。
// 这种用户手动刷新一下就好了。如果不想让他自己刷，加个自动刷新兜底（每个入口的 main.js 加一次即可
// 旧的 lazy chunk 加载失败时，自动刷新一次拿新版本
window.addEventListener('vite:preloadError', () => {
    if (sessionStorage.getItem('__reloaded__')) return  // 防止死循环
    sessionStorage.setItem('__reloaded__', '1')
    // 页面刷新
    window.location.reload()
})

// 创建实例
const app = createApp(App)

if (__DEV__) {
    // 开发环境引入手机浏览器的开发者工具
    const vc = new VConsole();
}

// 局处理组件渲染和事件处理过程中的错误
app.config.errorHandler = (error, instance, info) => {
    console.error(error, instance, info);
};
app.config.warnHandler = (msg, instance, trace) => {
    console.warn(msg, instance, trace);
};
app.config.performance = true;

async function setupAll(app) {
    // navigator.language
    await Switch(app, import.meta.env.VITE_LOCALE);

    // https://blog.csdn.net/weixin_41765715/article/details/132346684
    // 将data-picker的第一列换成周一
    app.use(ElementPlus, {
        locale: zhCn,
    })

    // 自定义指令
    RegisterDirective(app);
    // 路由
    app.use(router);

    // mount在最后
    app.mount('#app');
}

setupAll(app).then(r => {
    //
}).catch(e => {
    //
});
