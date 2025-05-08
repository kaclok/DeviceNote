// https://blog.csdn.net/weixin_45687201/article/details/139833908

// 编译时：全局变量
export default {
    // 需要同步在define.d.ts中申明，否则不能被webstorm识别
    __APP_VERSION__: JSON.stringify('v0.0.1'),
    /*__VUE_PROD_DEVTOOLS__: "true", // https://cn.vuejs.org/api/compile-time-flags.html*/
    __DEV__: process.env.NODE_ENV !== 'production',
    __OK__: 200,

    __Z_MODAL__: 1000,
    __Z_DROPDOWN__: 500,
    __Z_TOOLTIP__: 300,
    __Z_BASE__: 1,
    __Z_STEP__: 1,

    __AT_EXPIRE_CODE__: 10000,
    __RT_EXPIRE_CODE__: 10001,
    __HEART_BEAT_CODE__: 10003,
    __CHUNK_SIZE__: 2 * 1024 * 1024 // 2M
}
