/**
 * 配置浏览器本地存储的方式，可直接存储对象数组。
 */

import WebStorageCache from 'web-storage-cache'

type cacheType = 'localStorage' | 'sessionStorage'

const ECacheType = {
    AUTH_CENTER_URL: "AUTH_CENTER_URL", // 授权服务器url
    RES_URL: "RES_URL", // 资源服务器url

    ACCOUNT: "ACCOUNT", // 账户
    USER_NAME: "USER_NAME", // 名字

    ACCESS_TOKEN: "ACCESS_TOKEN",
    ACCESS_TOKEN_EXPIRE_AT: "ACCESS_TOKEN_EXPIRE_AT",
    ACCESS_TOKEN_ISSUE_AT: "ACCESS_TOKEN_ISSUE_AT",

    REFRESH_TOKEN: "REFRESH_TOKEN",
    REFRESH_TOKEN_EXPIRE_AT: "REFRESH_TOKEN_EXPIRE_AT",
    REFRESH_TOKEN_ISSUE_AT: "REFRESH_TOKEN_ISSUE_AT",
}

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

export {
    ECacheType,
    useCache,

    clearAll,
    clearToken,
}
