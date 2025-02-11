// https://blog.csdn.net/weixin_45687201/article/details/139833908

export default {
    // 需要同步在define.d.ts中申明，否则不能被webstorm识别
    __APP_VERSION__: JSON.stringify('v0.0.1'),
    __DEV__: process.env.NODE_ENV !== 'production',
    __OK__: 200,
    __AT_EXPIRE_CODE__: 10000,
    __RT_EXPIRE_CODE__: 10001,
    __HEART_BEAT_CODE__: 10003,
    __CHUNK_SIZE__: 2 * 1024 * 1024 // 2M
}
