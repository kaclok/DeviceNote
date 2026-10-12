/**
 * ECharts 公共配色与骨架：6 个页面共用的图只有「横向条形」和「环形占比」两种，
 * 抽成函数避免每个页面重复拼 option。
 */

export const C = {
    up: '#ef232a',
    down: '#12b981',
    accent: '#4c8dff',
    warn: '#eab308',
    axis: '#2c3a58',
    label: '#8b9ab5',
    split: 'rgba(44,58,88,.55)',
}

const tooltip = {
    backgroundColor: 'rgba(21,28,44,.95)',
    borderColor: C.axis,
    textStyle: {color: '#e8eefc', fontSize: 12},
}

/**
 * 横向条形图（板块家数、因素家数等）
 * @param {string[]} categories 类目（从上到下的顺序，函数内部会反转以匹配 y 轴从下往上）
 * @param {number[]} values
 * @param {string[]|string} colors 单色或与 categories 等长的颜色数组
 */
export function hbarOption(categories, values, colors, options = {}) {
    const max = Math.max(...values, 1)
    return {
        grid: {left: 8, right: 44, top: 8, bottom: 8, containLabel: true},
        tooltip: {...tooltip, trigger: 'axis', axisPointer: {type: 'shadow'}},
        xAxis: {
            type: 'value',
            max,
            axisLine: {show: false},
            axisTick: {show: false},
            axisLabel: {color: C.label, fontSize: 11},
            splitLine: {lineStyle: {color: C.split, type: 'dashed'}},
        },
        yAxis: {
            type: 'category',
            data: [...categories].reverse(),
            axisLine: {lineStyle: {color: C.axis}},
            axisTick: {show: false},
            axisLabel: {color: '#cbd5e1', fontSize: 11, width: 90, overflow: 'truncate'},
        },
        series: [{
            type: 'bar',
            barWidth: options.barWidth || 12,
            data: [...values].reverse().map((value, index) => ({
                value,
                itemStyle: {
                    borderRadius: [0, 4, 4, 0],
                    color: Array.isArray(colors)
                        ? [...colors].reverse()[index]
                        : (colors || C.accent),
                },
            })),
            label: {
                show: true,
                position: 'right',
                color: '#e8eefc',
                fontSize: 11,
                formatter: options.formatter || '{c}',
            },
        }],
    }
}

/**
 * 环形占比图（归因因素占比、板块构成等）
 * @param {{name:string,value:number,color?:string}[]} items
 */
export function pieOption(items, options = {}) {
    return {
        tooltip: {...tooltip, trigger: 'item', formatter: '{b}: {c} ({d}%)'},
        legend: {
            type: 'scroll',
            orient: 'vertical',
            right: 4,
            top: 'center',
            itemWidth: 10,
            itemHeight: 10,
            textStyle: {color: C.label, fontSize: 11},
        },
        series: [{
            type: 'pie',
            radius: options.radius || ['46%', '72%'],
            center: options.center || ['34%', '50%'],
            avoidLabelOverlap: true,
            itemStyle: {borderColor: '#151c2c', borderWidth: 2},
            label: {
                color: '#cbd5e1',
                fontSize: 11,
                formatter: options.labelFormatter || '{b}\n{d}%',
            },
            labelLine: {lineStyle: {color: C.axis}},
            data: items.map((item) => ({
                name: item.name,
                value: item.value,
                itemStyle: {color: item.color},
            })),
        }],
    }
}

// ---------------------------------------------------------------------------
// 周期 K 线技术指标：纯函数计算（MA / BOLL / MACD），数据来自后端雪球周期 K
// ---------------------------------------------------------------------------

const MA_SPECS = [
    {n: 5, color: '#eab308'},
    {n: 10, color: '#4c8dff'},
    {n: 20, color: '#f97316'},
    {n: 30, color: '#a855f7'},
    {n: 60, color: '#14b8a6'},
]

/** 简单移动平均，暖机期置 '-' */
function sma(values, n) {
    const out = new Array(values.length).fill('-')
    let sum = 0
    for (let i = 0; i < values.length; i++) {
        sum += values[i]
        if (i >= n) sum -= values[i - n]
        if (i >= n - 1) out[i] = +(sum / n).toFixed(2)
    }
    return out
}

/** 指数移动平均（首值种子，全程数值） */
function ema(values, n) {
    const k = 2 / (n + 1)
    const out = []
    let prev = values[0] || 0
    for (const v of values) {
        prev = v * k + prev * (1 - k)
        out.push(+prev.toFixed(4))
    }
    return out
}

/** 滚动总体标准差，暖机期置 '-' */
function rollingStd(values, n) {
    const out = new Array(values.length).fill('-')
    for (let i = n - 1; i < values.length; i++) {
        let sum = 0
        for (let j = i - n + 1; j <= i; j++) sum += values[j]
        const mean = sum / n
        let acc = 0
        for (let j = i - n + 1; j <= i; j++) acc += (values[j] - mean) ** 2
        out[i] = +Math.sqrt(acc / n).toFixed(4)
    }
    return out
}

/** MACD(12,26,9)：DIF / DEA / 柱 = 2*(DIF-DEA) */
function macdCalc(closes) {
    const e12 = ema(closes, 12)
    const e26 = ema(closes, 26)
    const dif = closes.map((_, i) => +(e12[i] - e26[i]).toFixed(2))
    const dea = ema(dif, 9).map((v) => +v.toFixed(2))
    const hist = dif.map((v, i) => +(2 * (v - dea[i])).toFixed(2))
    return {dif, dea, hist}
}

/**
 * 指数周期 K 线：蜡烛主图 + 可选叠加（均线 MA5/10/20/30/60、BOLL(20,2)）
 * 与可选副图（MACD、成交量），副图依勾选向下堆叠。
 * @param {{date:string,open:number,close:number,high:number,low:number,volume:number,amount?:number}[]} bars 升序
 * @param {{ma?:boolean,boll?:boolean,macd?:boolean,vol?:boolean}} inds 指标开关
 * @param {number} chartH 容器高度（px），用于像素级堆叠布局
 * @param {{start?:number,end?:number}} [view] dataZoom 视图（bar 索引，绝对值）。
 *   动态分页向头部拼数据后传恢复窗口（旧索引 + 新增条数）；缺省显示最近 60 根
 */
export function klineOption(bars, inds = {}, chartH = 340, view = null) {
    const dates = bars.map((b) => b.date)
    const closes = bars.map((b) => b.close)

    // ---- 布局：主图吃剩余高度，副图（MACD/成交量）依勾选向下堆叠 ----
    const subs = [inds.macd && 'macd', inds.vol && 'vol'].filter(Boolean)
    const top = 28, bottom = 26, gap = 18, subH = 86
    const mainH = Math.max(120, chartH - top - bottom - subs.length * (subH + gap))
    const subTop = (i) => top + mainH + gap + i * (subH + gap)
    const grids = [{left: 8, right: 56, top, height: mainH, containLabel: true}]
    // 副图用边框包起来（grid 自带 show/borderColor），与主图视觉区隔
    subs.forEach((_, i) => grids.push({left: 8, right: 56, top: subTop(i), height: subH,
                                       containLabel: true, show: true,
                                       borderColor: C.axis, borderWidth: 1}))

    // 副图名称标注：图内底部居中小标签（原左上角会与 y 轴数字重叠）
    const SUB_TITLES = {macd: 'MACD(12,26,9)', vol: '成交量(万手)'}
    const titles = subs.map((kind, i) => ({
        text: SUB_TITLES[kind], left: 'center', top: subTop(i) + subH - 15,
        textStyle: {color: C.label, fontSize: 10, fontWeight: 400},
        backgroundColor: 'rgba(21,28,44,.72)',
        padding: [1, 6],
        borderRadius: 3,
    }))

    // ---- 坐标轴：每个 grid 一条 category x 轴（仅最底部的显示日期），y 轴按副图语义 ----
    const lastIndex = grids.length - 1
    const xAxes = grids.map((_, i) => ({
        type: 'category', data: dates, gridIndex: i,
        axisLine: {lineStyle: {color: C.axis}}, axisTick: {show: false},
        axisLabel: i === lastIndex ? {color: C.label, fontSize: 10} : {show: false},
    }))
    const yAxisMain = {type: 'value', scale: true, gridIndex: 0,
        axisLabel: {color: C.label, fontSize: 10},
        splitLine: {lineStyle: {color: C.split, type: 'dashed'}}}
    const yAxes = [yAxisMain]
    for (const kind of subs) {
        yAxes.push(kind === 'macd'
            ? {type: 'value', gridIndex: yAxes.length, scale: true,
               axisLabel: {color: C.label, fontSize: 9},
               splitLine: {lineStyle: {color: C.split, type: 'dashed'}}}
            : {type: 'value', gridIndex: yAxes.length,
               axisLabel: {color: C.label, fontSize: 9}, splitLine: {show: false}})
    }

    // ---- series：蜡烛主图 ----
    const series = [{
        name: 'K线',
        type: 'candlestick',
        xAxisIndex: 0, yAxisIndex: 0,
        data: bars.map((b) => [b.open, b.close, b.low, b.high]),
        itemStyle: {
            color: C.up, color0: C.down,
            borderColor: C.up, borderColor0: C.down,
        },
    }]

    // 主图叠加：均线 + BOLL
    if (inds.ma) {
        for (const {n, color} of MA_SPECS) {
            series.push({
                name: `MA${n}`, type: 'line', xAxisIndex: 0, yAxisIndex: 0,
                data: sma(closes, n), showSymbol: false,
                lineStyle: {width: 1, color}, itemStyle: {color},
            })
        }
    }
    if (inds.boll) {
        const mid = sma(closes, 20)
        const std = rollingStd(closes, 20)
        const upper = mid.map((m, i) => (m === '-' ? '-' : +(m + 2 * std[i]).toFixed(2)))
        const lower = mid.map((m, i) => (m === '-' ? '-' : +(m - 2 * std[i]).toFixed(2)))
        const bollStyle = {width: 1, type: 'dashed', color: '#64748b'}
        series.push(
            {name: 'BOLL上', type: 'line', xAxisIndex: 0, yAxisIndex: 0, data: upper,
             showSymbol: false, lineStyle: bollStyle, itemStyle: {color: '#64748b'}},
            {name: 'BOLL中', type: 'line', xAxisIndex: 0, yAxisIndex: 0, data: mid,
             showSymbol: false, lineStyle: {width: 1, color: C.warn}, itemStyle: {color: C.warn}},
            {name: 'BOLL下', type: 'line', xAxisIndex: 0, yAxisIndex: 0, data: lower,
             showSymbol: false, lineStyle: bollStyle, itemStyle: {color: '#64748b'}},
        )
    }

    // 副图 series
    const subIndex = (kind) => 1 + subs.indexOf(kind)
    if (inds.macd) {
        const gi = subIndex('macd')
        const {dif, dea, hist} = macdCalc(closes)
        series.push(
            {name: 'MACD', type: 'bar', xAxisIndex: gi, yAxisIndex: gi, barWidth: '62%',
             data: hist.map((v) => ({value: v, itemStyle: {color: v >= 0 ? C.up : C.down}}))},
            {name: 'DIF', type: 'line', xAxisIndex: gi, yAxisIndex: gi, data: dif,
             showSymbol: false, lineStyle: {width: 1, color: '#eab308'}, itemStyle: {color: '#eab308'}},
            {name: 'DEA', type: 'line', xAxisIndex: gi, yAxisIndex: gi, data: dea,
             showSymbol: false, lineStyle: {width: 1, color: '#4c8dff'}, itemStyle: {color: '#4c8dff'}},
        )
    }
    if (inds.vol) {
        const gi = subIndex('vol')
        const volVals = bars.map((b) => +(b.volume / 1e4).toFixed(2))
        series.push({
            name: '成交量(万手)', type: 'bar', xAxisIndex: gi, yAxisIndex: gi,
            barWidth: '62%',
            data: bars.map((b, i) => ({
                value: volVals[i],
                itemStyle: {color: b.close >= b.open ? C.up : C.down},
            })),
        }, {
            // 顶部连线（走势线）：同数据叠一条折线，方便观察量能走向
            name: '量走势', type: 'line', xAxisIndex: gi, yAxisIndex: gi,
            data: volVals, showSymbol: false, silent: true,
            lineStyle: {width: 1, color: C.accent}, itemStyle: {color: C.accent},
        })
    }

    // tooltip：默认内容（K线 OHLC / 均线 / BOLL / 副图）+「较最新」= 该根收盘相对最新收盘的涨跌幅
    // （dataIndex 直查 bars，避开 candlestick param.value 结构在各版本间的差异）
    const lastClose = closes.length ? closes[closes.length - 1] : NaN
    const f2 = (v) => (Number.isFinite(+v) ? (+v).toFixed(2) : '-')
    const klineTip = {
        ...tooltip, trigger: 'axis', axisPointer: {type: 'cross'},
        formatter: (params) => {
            const list = Array.isArray(params) ? params : [params]
            if (!list.length) return ''
            const rows = [list[0].name]
            for (const it of list) {
                if (it.seriesName === 'K线') {
                    const b = bars[it.dataIndex]
                    if (b) {
                        rows.push(`${it.marker}开 ${f2(b.open)} 收 ${f2(b.close)} 低 ${f2(b.low)} 高 ${f2(b.high)}`)
                        const pct = Number.isFinite(lastClose) && lastClose > 0
                            ? (b.close / lastClose - 1) * 100 : null
                        if (pct !== null) {
                            const col = pct >= 0 ? C.up : C.down
                            rows.push(`较最新 <span style="color:${col}">${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%</span>`)
                        }
                    }
                    continue
                }
                rows.push(`${it.marker}${it.seriesName} ${f2(it.value)}`)
            }
            return rows.join('<br/>')
        },
    }

    const winSize = 60
    const zoomStart = Number.isFinite(view?.start)
        ? Math.max(0, Math.min(view.start, Math.max(0, dates.length - 1)))
        : Math.max(0, dates.length - winSize)
    const zoomEnd = Number.isFinite(view?.end)
        ? Math.max(zoomStart, Math.min(view.end, dates.length - 1))
        : dates.length - 1
    return {
        grid: grids,
        title: titles,
        tooltip: klineTip,
        legend: {
            top: 0,
            right: 56,
            itemWidth: 12,
            itemHeight: 8,
            textStyle: {color: C.label, fontSize: 11},
        },
        xAxis: xAxes,
        yAxis: yAxes,
        dataZoom: [
            {type: 'inside', xAxisIndex: grids.map((_, i) => i),
             startValue: zoomStart, endValue: zoomEnd,
             zoomOnMouseWheel: true, moveOnMouseWheel: false},
            {type: 'slider', xAxisIndex: grids.map((_, i) => i), bottom: 2, height: 16,
             borderColor: C.axis, fillerColor: 'rgba(76,141,255,.15)',
             brushSelect: false,
             textStyle: {color: C.label, fontSize: 9}},
        ],
        series,
    }
}

/**
 * 指数/个股分时线：价格线按对昨收的相对位置红涨绿跌分段着色 + 昨收虚线 + 面积渐变。
 * 可叠加板块指数线（独立右轴，各自量纲互不影响）。
 * @param {{t:string, price:number}[]} points 分钟点，升序（09:30 起，午休无点）
 * @param {number} [prevClose] 昨收参考价
 * @param {{board?:{name:string, points:{t:string,price:number}[], prevClose?:number}}} [options] 板块叠加线
 */
export function minuteOption(points, prevClose, options = {}) {
    const board = options.board || null
    const times = points.map((p) => p.t)
    const prices = points.map((p) => p.price)
    const hasPrev = Number.isFinite(prevClose) && prevClose > 0
    const lows = hasPrev ? [...prices, prevClose] : prices
    const min = Math.min(...lows)
    const max = Math.max(...lows)
    const pad = Math.max((max - min) * 0.1, 0.5)
    // tooltip：时间 + 指数 + 较昨收 + 较最新（基准=当日最后一点，盘中即实时最新）
    const lastTime = times.length ? times[times.length - 1] : ''
    const lastPrice = prices.length ? prices[prices.length - 1] : undefined
    // 最新点常显：末端涨跌色（无昨收时用中性色）+ 价格 + 较昨收涨幅
    const upDown = hasPrev && Number.isFinite(lastPrice) ? (lastPrice >= prevClose ? C.up : C.down) : C.accent
    const lastText = Number.isFinite(lastPrice) ? lastPrice.toFixed(2) : ''
    const lastPct = hasPrev && Number.isFinite(lastPrice)
        ? `${lastPrice >= prevClose ? '+' : ''}${((lastPrice / prevClose - 1) * 100).toFixed(2)}%`
        : ''
    const signPct = (value, base) => {
        if (!Number.isFinite(base) || base <= 0 || !Number.isFinite(value)) return ''
        const pct = (value / base - 1) * 100
        return `<span style="color:${pct >= 0 ? C.up : C.down}">${pct >= 0 ? '+' : ''}${pct.toFixed(2)}%</span>`
    }
    const tip = {
        ...tooltip, trigger: 'axis',
        formatter: (params) => {
            const list = Array.isArray(params) ? params : [params]
            const p = list[0]
            if (!p) return ''
            const price = Number(p.value)
            const rows = [p.name, `价格 ${Number.isFinite(price) ? price.toFixed(2) : '-'}`]
            const vsPrev = signPct(price, prevClose)
            vsPrev && rows.push(`较昨收 ${vsPrev}`)
            const vsLast = signPct(price, lastPrice)
            vsLast && rows.push(`较最新 ${vsLast}`)
            const ap = list.find((x) => x.seriesName === '均价')
            if (ap) {
                const av = Number(ap.value)
                Number.isFinite(av) && rows.push(`均价 ${av.toFixed(2)}`)
            }
            // 板块叠加线：独立量纲（右轴），行内自带较板块昨收的涨跌幅
            const bp = list.find((x) => x.seriesName === '板块')
            if (bp && board) {
                const bv = Number(bp.value)
                if (Number.isFinite(bv)) {
                    const bPct = Number.isFinite(board.prevClose) && board.prevClose > 0
                        ? (bv / board.prevClose - 1) * 100 : null
                    const bCol = bPct === null ? '' : `<span style="color:${bPct >= 0 ? C.up : C.down}">` +
                        `${bPct >= 0 ? '+' : ''}${bPct.toFixed(2)}%</span>`
                    rows.push(`${board.name} ${bv.toFixed(2)} ${bCol}`)
                }
            }
            return rows.join('<br/>')
        },
    }
    const series = [{
        type: 'line',
        data: prices,
        showSymbol: false,
        lineStyle: {width: 1.2},
        // 末端常显最新指数（鼠标不悬停也可见）：价格 + 较昨收涨幅，涨跌色
        endLabel: {
            show: prices.length > 0,
            formatter: lastPct ? `${lastText}  ${lastPct}` : lastText,
            color: upDown,
            fontSize: 12,
            fontWeight: 600,
            distance: 6,
        },
        areaStyle: {
            opacity: 0.14,
            color: {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [
                    {offset: 0, color: 'rgba(76,141,255,.8)'},
                    {offset: 1, color: 'rgba(76,141,255,0)'},
                ],
            },
        },
    }]
    // 均价线（黄线，后端预计算的分钟成交量加权累计均价；大盘/个股分时共用）
    if (points.some((p) => Number.isFinite(p.avg))) {
        series.push({
            name: '均价', type: 'line',
            data: points.map((p) => (Number.isFinite(p.avg) ? p.avg : null)),
            showSymbol: false, silent: true,
            lineStyle: {width: 1, color: C.warn}, itemStyle: {color: C.warn},
        })
    }
    // 最新点脉冲标记（实时感，收盘中也标出收盘位）
    if (prices.length) {
        series.push({
            name: '最新', type: 'effectScatter', data: [[lastTime, lastPrice]],
            symbolSize: 6, silent: true, z: 12,
            rippleEffect: {scale: 2.8, brushType: 'stroke'},
            itemStyle: {color: upDown},
        })
    }
    // 板块叠加线：独立右轴（指数点数与个股价格量纲不同，不可共轴），虚线弱化
    const boardSeries = board && board.points?.length ? (() => {
        const bMap = new Map(board.points.map((p) => [p.t, p.price]))
        return {
            name: '板块', type: 'line', yAxisIndex: 1,
            data: times.map((t) => (bMap.has(t) ? bMap.get(t) : null)),
            showSymbol: false, silent: true,
            lineStyle: {width: 1, type: 'dashed', color: C.accent},
            itemStyle: {color: C.accent},
        }
    })() : null
    if (boardSeries) series.push(boardSeries)

    // 带板块线时 y 轴升级为数组：主轴（左）+ 板块轴（右）；grid 右侧让位给双 endLabel+右轴
    const yAxis = boardSeries
        ? [{
            type: 'value', min: min - pad, max: max + pad,
            axisLabel: {color: C.label, fontSize: 10},
            splitLine: {lineStyle: {color: C.split, type: 'dashed'}},
        }, {
            type: 'value', position: 'right', scale: true,
            axisLabel: {color: C.label, fontSize: 10},
            splitLine: {show: false},
        }]
        : {type: 'value', min: min - pad, max: max + pad,
           axisLabel: {color: C.label, fontSize: 10},
           splitLine: {lineStyle: {color: C.split, type: 'dashed'}}}
    const gridRight = boardSeries ? 130 : 100

    if (hasPrev) {
        series[0].markLine = {
            silent: true, symbol: 'none',
            lineStyle: {color: C.label, type: 'dashed', width: 1},
            label: {color: C.label, fontSize: 10, formatter: '昨收 {c}'},
            data: [{yAxis: prevClose}],
        }
        // 分段着色：高于昨收红、低于昨收绿（dimension 1 = y 值）
        return {
            grid: {left: 8, right: gridRight, top: 20, bottom: 8, containLabel: true},
            tooltip: tip,
            visualMap: {
                show: false, dimension: 1, seriesIndex: [0],
                // ⚠️ echarts 5.6：pieces 全为单边（gt/lte）时 stops 为空，
                // LineView.getVisualGradient 读 [0].coord 直接崩 ⇒ 至少一个 piece 带有限 min/max
                pieces: [
                    {min: min - pad, max: prevClose, color: C.down},
                    {gt: prevClose, color: C.up},
                ],
            },
            xAxis: {
                type: 'category', data: times, boundaryGap: false,
                axisLine: {lineStyle: {color: C.axis}}, axisTick: {show: false},
                axisLabel: {color: C.label, fontSize: 10, interval: 59},
            },
            yAxis,
            series,
        }
    }
    return {
        grid: {left: 8, right: gridRight, top: 20, bottom: 8, containLabel: true},
        tooltip: tip,
        xAxis: {
            type: 'category', data: times, boundaryGap: false,
            axisLine: {lineStyle: {color: C.axis}}, axisTick: {show: false},
            axisLabel: {color: C.label, fontSize: 10, interval: 59},
        },
        yAxis,
        series,
    }
}

/**
 * 竖向柱状图（连板梯队、板块轮动）
 */
export function vbarOption(categories, values, options = {}) {
    return {
        grid: {left: 8, right: 12, top: 24, bottom: 8, containLabel: true},
        tooltip: {...tooltip, trigger: 'axis', axisPointer: {type: 'shadow'}},
        xAxis: {
            type: 'category',
            data: categories,
            axisLine: {lineStyle: {color: C.axis}},
            axisTick: {show: false},
            axisLabel: {color: C.label, fontSize: 11},
        },
        yAxis: {
            type: 'value',
            axisLine: {show: false},
            axisTick: {show: false},
            axisLabel: {color: C.label, fontSize: 11},
            splitLine: {lineStyle: {color: C.split, type: 'dashed'}},
        },
        series: [{
            type: 'bar',
            barWidth: options.barWidth || 22,
            data: values.map((value) => ({
                value,
                itemStyle: {borderRadius: [4, 4, 0, 0], color: options.color || C.accent},
            })),
            label: {show: true, position: 'top', color: '#e8eefc', fontSize: 11},
        }],
    }
}
