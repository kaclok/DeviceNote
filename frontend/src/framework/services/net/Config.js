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
