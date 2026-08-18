/**
 * 配置浏览器本地存储的方式，可直接存储对象数组。
 */

import WebStorageCache from 'web-storage-cache'

type cacheType = 'localStorage' | 'sessionStorage'

// 缓存数据分为2部分
// 1、跟随用户的数据，随着用户登录登出而变化
// 2、跟随app的数据，跟随app的安装卸载而变化
const ECacheType = Object.freeze({
    AUTH_CENTER_URL: "AUTH_CENTER_URL", // 授权服务器url
    RES_URL: "RES_URL", // 资源服务器url

    ACCOUNT: "ACCOUNT", // 账户

    ACCESS_TOKEN: "ACCESS_TOKEN",
    ACCESS_TOKEN_EXPIRE_AT: "ACCESS_TOKEN_EXPIRE_AT",
    ACCESS_TOKEN_ISSUE_AT: "ACCESS_TOKEN_ISSUE_AT",

    REFRESH_TOKEN: "REFRESH_TOKEN",
    REFRESH_TOKEN_EXPIRE_AT: "REFRESH_TOKEN_EXPIRE_AT",
    REFRESH_TOKEN_ISSUE_AT: "REFRESH_TOKEN_ISSUE_AT",
})

const useCache = (type: cacheType = 'localStorage') => {
    const wsCache: WebStorageCache = new WebStorageCache({
        storage: type
    })

    return {
        wsCache
    }
}

function clearAll() {
    const {wsCache} = useCache()
    // todo for in是否使用正确？
    for (const t in ECacheType) {
        wsCache.delete(t)
    }
}

function clearToken() {
    const {wsCache} = useCache()
    wsCache.delete(ECacheType.ACCESS_TOKEN)
    wsCache.delete(ECacheType.ACCESS_TOKEN_EXPIRE_AT)
    wsCache.delete(ECacheType.ACCESS_TOKEN_ISSUE_AT)

    wsCache.delete(ECacheType.REFRESH_TOKEN)
    wsCache.delete(ECacheType.REFRESH_TOKEN_EXPIRE_AT)
    wsCache.delete(ECacheType.REFRESH_TOKEN_ISSUE_AT)
}

// 账户登出清除数据
function clearAccount() {
    const {wsCache} = useCache()
    wsCache.delete(ECacheType.ACCOUNT)
    clearToken()
}

export {
    ECacheType,
    useCache,

    clearAll,
    clearToken,
    clearAccount,
}
