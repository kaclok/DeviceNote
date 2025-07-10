import {ECacheType, useCache} from '@/framework/composable/use/useCache.ts'
import {post} from './net/InnerRequest.js'

const {wsCache} = useCache()

class TokenService {
    static isRT(config) {
        return !!config && config.__isRT
    }

    static async getRemoteAT() {
        await post({
            url: '/refreshAT', headers: {
                rt: TokenService.getRT(),
            },

            __isRT: true, // 标识是否为RT请求, 用于请求的时候填充refresh-token
        })
    }

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
}

export {
    TokenService
}
