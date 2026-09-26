export const backends = {
    index: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, dq: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, yb: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, sb: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, ai_entry: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, safe_product: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, wz: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, watchdog: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, sbrhjy: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, cggy: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, cghtz: {
        api: "/api-cghtz",
        url: 'http://10.8.13.66:7090'
    }, carop: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, filetest: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, fileview: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, filechunk: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    }, downloads: {
        api: "/api",
        url: 'http://10.8.13.66:7090'
    },
}
// 未登记入口的兜底后端（绝大多数入口连它）
export const defaultBackend = backends.index
