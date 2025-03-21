import {createApp} from 'vue'
import App from './App.vue'

// https://blog.csdn.net/weixin_41765715/article/details/132346684
// 将data-picker的第一列换成周一
import ElementPlus from 'element-plus'
import zhCn from "element-plus/es/locale/lang/zh-cn";
import 'dayjs/locale/zh-cn';

// import {router} from '@/cms/daily_paper/router/Index.js'
// import直接引用一个文件时，会执行一遍这个文件，而不获取任何文件对象, 比如：import './lib/init.js';
import {RegisterDirective} from "@/framework/directives/DirectiveList.js";
import {Switch} from "@/framework/services/LocaleService.js";

import "@/framework/services/net/Init.js";
import {router} from "@/cms/daily_paper/router/Index.js";
import {TokenService} from "@/framework/services/TokenService.js";
import {LocalStorageService} from "@/framework/services/LocalStorageService.js";

// 创建实例
const app = createApp(App)
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
   /* app.use(router);*/

    // mount在最后
    app.mount('#app');
}

setupAll(app).then(r => {
    //
}).catch(e => {
    //
});
