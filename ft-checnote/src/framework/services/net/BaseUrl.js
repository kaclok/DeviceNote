import {backends, defaultBackend} from '../../../../backends.mjs'

// 入口 html 配置覆盖：window.__APP_CONFIG__ = { backend }（cghtz 的 index.html 已配），部署后可直接改、无需重新构建。
// ⚠️ 仅 prod 生效 —— dev 若吃这个绝对地址会绕过 vite proxy 造成跨域失败
const backend = window?.__APP_CONFIG__?.backend

// dev 走 vite proxy 前缀（前缀规则由 vite.config.js 按 backends 的同一套 key 生成）；
// prod 直连绝对地址
export const resolvedApi = backend ? backends[backend]?.api : defaultBackend.api
