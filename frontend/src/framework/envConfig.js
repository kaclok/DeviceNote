// let mode = process.env.NODE_ENV;
// 主要作用就是：和vite.config.js中的proxy配置, 以及axiosInstance文件的changeBaseURL合作，达到在登录页面可以下拉选择vue前端连接的springboot后端的ip地址，方便开发阶段调试
import {loadEnv} from 'vite'

function getEnvConfig(modeName) {
    return loadEnv(modeName, './.env');
}

function getModeBaseAPI(modeName) {
    let modeCfg = getEnvConfig(modeName);
    return modeCfg['VITE_BASE_API'];
}

let allModes = {
    'development': getModeBaseAPI('development'),
    'production': getModeBaseAPI('production'),
}

export {allModes, getModeBaseAPI, getEnvConfig}
