import {backends, defaultBackend} from '../../../../backends.mjs'

// 入口 html 配置覆盖：window.__APP_CONFIG__ = { backend }（cghtz 的 index.html 已配），部署后可直接改、无需重新构建。
// ⚠️ 仅 prod 生效 —— dev 若吃这个绝对地址会绕过 vite proxy 造成跨域失败
// typeof 守卫防未声明标识符：可选链只防 null/undefined 属性访问，裸 `window?.` 在 node（探针/SSR）下会 ReferenceError
const backend = (typeof window !== 'undefined') ? window?.__APP_CONFIG__?.backend : undefined

// dev 走 vite proxy 前缀（前缀规则由 vite.config.js 按 backends 的同一套 key 生成）；
// prod 直连绝对地址
export const resolvedApi = backend ? backends[backend]?.api : defaultBackend.api
