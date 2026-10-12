import axios from 'axios'
// 后端地址唯一真相源：与 vite dev proxy 共用 backends.mjs
import {backends, defaultBackend} from '../../../../../backends.mjs'

// 入口 html 里的 window.__APP_CONFIG__ = { backend: 'trend' }
const backendKey = (typeof window !== 'undefined') ? window?.__APP_CONFIG__?.backend : undefined
export const API_BASE = (backendKey && backends[backendKey]?.api) || defaultBackend.api

const http = axios.create({
    baseURL: API_BASE,
    // 上游是公网实时接口，单次聚合最长可能要十几秒
    timeout: 60000,
})

http.interceptors.response.use(
    // 直接返回业务数据，调用方不用再 .data
    (response) => response.data,
    (error) => {
        if (axios.isCancel(error)) {
            return Promise.reject(error)
        }
        // 服务端用 HTTPException(detail=...) 描述原因
        const detail = error?.response?.data?.detail
        const message = detail || error?.message || '行情服务请求失败'
        return Promise.reject(new Error(message))
    },
)

export default http
