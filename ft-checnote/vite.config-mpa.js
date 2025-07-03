import {resolve} from "path";

export default {
    // https://cn.vitejs.dev/guide/build#multi-page-app
    // https://www.bilibili.com/video/BV1Ru4y1Q7SQ/?vd_source=5c9f5bd891aee351c325bcf632b5550f
    index: resolve(__dirname, './index.html'),
    dq: resolve(__dirname, './pages/dq/index.html'),
    yb: resolve(__dirname, './pages/yb/index.html'),
    sb: resolve(__dirname, './pages/sb/index.html'),
    ai_entry: resolve(__dirname, './pages/ai_entry/index.html'),
    safe_product: resolve(__dirname, './pages/safe_product/index.html'),
    wz: resolve(__dirname, './pages/wz/index.html'),
    watchdog: resolve(__dirname, './pages/smds/watchdog/index.html'),
    sbrhjy: resolve(__dirname, './pages/smlj/sbrhjy/index.html'),
    cggy: resolve(__dirname, './pages/smlj/cggy/index.html'),
}
