import {resolve} from "path";

const mpaInput = {
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
    downloads: resolve(__dirname, './pages/downloads/index.html'),
}

// https://blog.csdn.net/usernotdefined/article/details/129897995
const mpaRewrites = [
    {from: /\/cggy/, to: '/pages/smlj/cggy/index.html'},
]

const mpaPages= [
    {
        entry: resolve(__dirname, './src/main.ts'),
        filename: resolve(__dirname, './src/index.html'),
        template: resolve(__dirname, './src/index.html'),
        inject: {
            data: {
                title: 'mpa'
            }
        }
    },
    {
        entry: resolve(__dirname, './src/subPage/main.ts'),
        filename: resolve(__dirname, './src/subPage/index.html'),
        template: resolve(__dirname, './src/subPage/index.html'),
        inject: {
            data: {
                title: 'mpa'
            }
        }
    }
]

export {
    mpaInput,
    mpaRewrites,
    mpaPages,
}
