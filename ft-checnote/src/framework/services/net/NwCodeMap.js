// https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
// AT过期(10000)、RT过期(10001)、未登录(10002)、AT被篡改(10005)
// 已在 AxiosInst.js 响应拦截器中统一处理(含并发刷新风暴防护、防死循环、登出跳转)：
//  - 10000: handleATExpired 刷新AT后重试原请求(共享refreshPromise防风暴、__retried防死循环)
//  - 10001/10002/10005: triggerAuthFailure 清登录态+跳登录页(由main.js注册)
// 此处不再重复，避免一处逻辑两处维护导致刷新/登出被触发两次。
// 原先这里用 useRouter() 在模块顶层调用会拿到 undefined(不在setup上下文)，router.push 会崩，已移除。
const NwCodeMap = {
    [__HEART_BEAT_CODE__]: (resp) => {

    },
}

export {
    NwCodeMap,
}
