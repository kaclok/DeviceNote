import {ECacheType, useCache} from '@/framework/composable/use/useCache.ts'
import {post} from './net/InnerRequest.js'

const {wsCache} = useCache()

class TokenService {
    static getAT() {
        return wsCache.get(ECacheType.ACCESS_TOKEN)
    }

    static getATIssueAt() {
        return wsCache.get(ECacheType.ACCESS_TOKEN_ISSUE_AT)
    }

    static getATExpireAt() {
        return wsCache.get(ECacheType.ACCESS_TOKEN_EXPIRE_AT)
    }

    static getRT() {
        return wsCache.get(ECacheType.REFRESH_TOKEN)
    }

    static getRTIssueAt() {
        return wsCache.get(ECacheType.REFRESH_TOKEN_ISSUE_AT)
    }

    static getATExpireRt() {
        return wsCache.get(ECacheType.REFRESH_TOKEN_EXPIRE_AT)
    }

    static setAT(at) {
        return wsCache.set(ECacheType.ACCESS_TOKEN, at)
    }

    static setATIssueAt(atAt) {
        return wsCache.set(ECacheType.ACCESS_TOKEN_ISSUE_AT, atAt)
    }

    static setATExpireAt(atAt) {
        return wsCache.set(ECacheType.ACCESS_TOKEN_EXPIRE_AT, atAt)
    }

    static setRT(rt) {
        return wsCache.set(ECacheType.REFRESH_TOKEN, rt)
    }

    static setRTIssueAt(rtAt) {
        return wsCache.set(ECacheType.REFRESH_TOKEN_ISSUE_AT, rtAt)
    }

    static setRTExpireAt(rtAt) {
        return wsCache.set(ECacheType.REFRESH_TOKEN_EXPIRE_AT, rtAt)
    }

    static isRT(config) {
        return !!config && config.__isRT
    }

    static async getRemoteAT() {
        // 发起refresh请求：rt由请求拦截器根据__isRT自动注入到请求头
        // 响应拦截器中的_setToken会自动把新AT/RT写入本地存储
        // console.error("-----------------refreshAccessToken-----------------")
        await post({
            url: 'x/refreshAccessToken',
            // 标识是否为RT请求, 用于在前端发送请求的时候判断哪些请求需要填充refresh-token
            // 这个__isRT发送不到后端，因为axios会忽略不认识的自定义属性，自然后端也不会原封不动的返回给前端这个__isRT
            __isRT: true,
        })
    }
}

export {
    TokenService
}
