// 探针：副图边框 + 底部居中标题
import * as echarts from 'echarts/core'
import {BarChart, LineChart, CandlestickChart} from 'echarts/charts'
import {GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, TitleComponent, VisualMapComponent} from 'echarts/components'
import {SVGRenderer} from 'echarts/renderers'
import {klineOption} from './src/cms/stock/trend/utils/ChartX.js'

echarts.use([BarChart, LineChart, CandlestickChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, TitleComponent, VisualMapComponent, SVGRenderer])

let pass = 0, fail = 0
const ck = (name, cond) => { cond ? pass++ : (fail++, console.log('FAIL', name)) }

const bars = Array.from({length: 30}, (_, i) => ({
    date: `2026-01-${String((i % 28) + 1).padStart(2, '0')}`,
    open: 1250 + i, close: 1252 + i, high: 1255 + i, low: 1248 + i,
    volume: 34567890 + i * 1e5, amount: 4.4e9 + i * 1e7,
}))
const opt = klineOption(bars, {ma: true, boll: true, macd: true, vol: true, amount: true}, 560)

ck('副图 grid 带边框（show/borderColor/borderWidth）',
    opt.grid.slice(1).every((g) => g.show === true && g.borderColor === '#2c3a58' && g.borderWidth === 1))
ck('主图无边框', opt.grid[0].show !== true)
ck('标题 3 个且底部居中（left=center，top 在各自 grid 内底部）',
    opt.title.length === 3 && opt.title.every((t, i) => t.left === 'center'
        && t.top > opt.grid[i + 1].top && t.top < opt.grid[i + 1].top + opt.grid[i + 1].height))
ck('标题带半透明底板', opt.title.every((t) => typeof t.backgroundColor === 'string' && t.padding))

const c = echarts.init(null, null, {renderer: 'svg', ssr: true, width: 800, height: 560})
c.setOption(opt)
const svg = c.renderToSVGString()
c.dispose()
ck('SSR 渲染且标题文字在 SVG 中', svg.includes('成交量(万手)') && svg.includes('MACD(12,26,9)') && svg.includes('成交额(亿)'))

console.log(`==== ${pass} pass / ${fail} fail ====`)
process.exit(fail ? 1 : 0)
