const defaultUrl = "/api"

let config = {
    /**
     * api请求基础路径
     */
    base_url: defaultUrl,
    /**
     * 接口成功返回状态码
     */
    result_code: 200,

    /**
     * 接口请求超时时间
     */
    request_timeout: 30000,

    /**
     * 响应签名校验开关(与后端 application.yml sign.response-enabled 同步开启/关闭)
     * true : 对每个成功(code=200)且 data 非空的响应, 用与后端相同的规范文本算法
     *        重算 HMAC-SHA256 并与 body.sign 比对, 防响应数据被篡改; 不匹配直接 reject。
     * false: 不校验, 兼容后端未开启响应签名的场景。
     * 注意: 若后端用 @SignIgnore 豁免了某接口的响应签名, 前端开启时该校验会失败,
     *       需在下方校验处对该接口同步放行(见 AxiosInst.js)。
     */
    verify_response_sign: false,

    /**
     * 请求签名开关(与后端 application.yml sign.request-enabled 同步开启/关闭)
     * true : AxiosInst 请求拦截器给 GET/query params 附加 __timestamp__/__nonce__/__sign__,
     *        后端 SignInterceptor 据此验签(防篡改+防重放)。
     * false: params 原样透传, 不附加任何签名参数; 此时后端也必须关闭验签,
     *        否则后端会对带业务参数但无签名的请求返回 RC10401。
     * 默认 true = 维持既有行为; 两侧需同步切换。
     */
    request_sign_enabled: false,

    /**
     * 默认接口请求类型
     * 可选值：application/x-www-form-urlencoded multipart/form-data
     */
    default_headers: 'multipart/form-data',

    /* // 出于开发阶段的调试目的，让可以在悬浮下拉菜单中切换baseUrl
    changeBaseURL: function (url) {
        this.base_url = url
    },*/

    resetBaseURL: function () {
        this.base_url = defaultUrl
    },
}

export { config }
