import {createApp} from 'vue'
import App from './App.vue'

import ElementPlus from 'element-plus'
import zhCn from "element-plus/es/locale/lang/zh-cn";
import 'dayjs/locale/zh-cn';
import VConsole from 'vconsole';

import {RegisterDirective} from "@/framework/directives/DirectiveList.js";
import {Switch} from "@/framework/services/LocaleService.js";

import "@/framework/services/net/Init.js";

// 创建实例
const app = createApp(App)

if (__DEV__) {
    const vc = new VConsole();
}

app.config.errorHandler = (error, instance, info) => {
    console.error(error, instance, info);
};
app.config.warnHandler = (msg, instance, trace) => {
    console.warn(msg, instance, trace);
};
app.config.performance = true;

async function setupAll(app) {
    await Switch(app, import.meta.env.VITE_LOCALE);

    app.use(ElementPlus, {
        locale: zhCn,
    })

    RegisterDirective(app);

    app.mount('#app');
}

setupAll(app).then(r => {
    //
}).catch(e => {
    //
});
