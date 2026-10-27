/**
 * 厂区路网寻路：Dijkstra 求最优，Yen 算法求 K 条互不相同的备选路线。
 *
 * 路网数据里，一条 road 就是一条边；道路上的折点只影响绘制与里程，
 * 不额外拆成图节点，避免地图数据与拓扑结构互相耦合。
 */

const LEVEL_SPEED = {main: 25, secondary: 25, branch: 25} // km/h
const JUNCTION_DELAY = 0 // 秒，每个路口的起步等待
const TURN_DELAY_SMALL = 0 // 秒，转向一次
const TURN_DELAY_LARGE = 0 // 秒，大角度转向/掉头

export function polylineLength(points) {
    let total = 0
    for (let i = 1; i < points.length; i++) {
        total += Math.hypot(points[i][0] - points[i - 1][0], points[i][1] - points[i - 1][1])
    }
    return total
}

export function buildNetwork(config, closedRoadIds = new Set(), materialId = '') {
    const nodes = new Map((config.nodes || []).map((n) => [n.id, n]))
    const metersPerPixel = Number(config.plant?.metersPerPixel) || 1
    const roadsById = new Map()
    const roads = []
    for (const road of config.roads || []) {
        if (road.enabled === 0 || closedRoadIds.has(road.id)) continue
        const distance = polylineLength(road.points) * metersPerPixel
        const speedMs = (road.speed ?? LEVEL_SPEED[road.level] ?? 15) / 3.6
        const item = {...road, distance, cost: distance / speedMs + JUNCTION_DELAY}
        roads.push(item)
        roadsById.set(road.id, item)
    }

    const adj = new Map((config.nodes || []).map((n) => [n.id, []]))
    // 大门通行方向单独配置在 gate 表：in 仅进（禁止驶出） / out 仅出（禁止驶入） / none 禁行（不让进不让出）
    // 大门允许物资同属大门属性：配置了白名单的大门，其它物资的车辆不能进出
    const gateList = config.gates || []
    const gateModes = new Map(gateList.map((gate) => [gate.nodeId, gate.gateMode]))
    const parseIds = (value) => {
        if (Array.isArray(value)) return value
        if (typeof value !== 'string' || !value.trim()) return []
        try {
            const parsed = JSON.parse(value)
            return Array.isArray(parsed) ? parsed : []
        } catch (e) {
            return []
        }
    }
    const gateMaterials = new Map(gateList.map((gate) => [gate.nodeId, parseIds(gate.allowedMaterialIds)]))
    // 禁行(none)大门两个方向都禁止：车辆不能经它进厂也不能经它出厂
    const blockFromOutGate = (id) => gateModes.get(id) === 'out' || gateModes.get(id) === 'none'
    const blockToInGate = (id) => gateModes.get(id) === 'in' || gateModes.get(id) === 'none'
    const blockMaterial = (id) => {
        if (!materialId) return false
        const allowed = gateMaterials.get(id) || []
        return allowed.length > 0 && !allowed.includes(materialId)
    }
    // 仅出大门不能驶入；仅进大门不能驶出；物资白名单不符时两个方向都禁止
    const canLeave = (id) => !blockFromOutGate(id) && !blockMaterial(id)
    const canEnter = (id) => !blockToInGate(id) && !blockMaterial(id)
    for (const road of roads) {
        if (canLeave(road.from) && canEnter(road.to)) {
            adj.get(road.from)?.push({to: road.to, road})
        }
        if (canLeave(road.to) && canEnter(road.from)) {
            adj.get(road.to)?.push({to: road.from, road})
        }
    }
    return {nodes, roadsById, roads, adj, plant: config}
}

function dijkstra(net, start, end, bannedEdges = new Set(), bannedNodes = new Set()) {
    if (!net.adj.has(start) || !net.adj.has(end)) return null
    if (bannedNodes.has(start) || bannedNodes.has(end)) return null
    if (start === end) return {nodes: [start], edges: [], cost: 0}

    const dist = new Map()
    const prev = new Map()
    for (const id of net.adj.keys()) dist.set(id, Infinity)
    dist.set(start, 0)

    const visited = new Set()
    while (true) {
        let cur = null
        let best = Infinity
        for (const [id, d] of dist) {
            if (!visited.has(id) && d < best) {
                best = d
                cur = id
            }
        }
        if (cur === null) break
        if (cur === end) break
        visited.add(cur)
        for (const edge of net.adj.get(cur) || []) {
            if (bannedEdges.has(edge.road.id)) continue
            if (bannedNodes.has(edge.to)) continue
            if (visited.has(edge.to)) continue
            const next = best + edge.road.cost
            if (next < dist.get(edge.to)) {
                dist.set(edge.to, next)
                prev.set(edge.to, {from: cur, roadId: edge.road.id})
            }
        }
    }

    if (!prev.has(end)) return null
    const nodes = [end]
    const edges = []
    let cur = end
    while (cur !== start) {
        const step = prev.get(cur)
        if (!step) return null
        edges.unshift(step.roadId)
        nodes.unshift(step.from)
        cur = step.from
    }
    return {nodes, edges, cost: dist.get(end)}
}

function pathKey(nodes) {
    return nodes.join('>')
}

function prefixEqual(a, b) {
    for (let i = 0; i < b.length; i++) {
        if (a[i] !== b[i]) return false
    }
    return true
}

function pathCost(net, edges) {
    let cost = 0
    for (const id of edges) {
        const road = net.roadsById.get(id)
        if (!road) return Infinity
        cost += road.cost
    }
    return cost
}

/**
 * 返回按通行时间排序、节点序列互不相同的若干条路线。
 * K 只是上限；路网本身没有那么多合理路径时会少于 K 条。
 */
export function kShortestPaths(net, start, end, K = 4) {
    if (start === end) return []
    const first = dijkstra(net, start, end)
    if (!first) return []

    const result = [first]
    const candidates = []
    const seen = new Set([pathKey(first.nodes)])

    for (let k = 1; k < K; k++) {
        const prevPath = result[k - 1]
        for (let i = 0; i < prevPath.nodes.length - 1; i++) {
            const rootNodes = prevPath.nodes.slice(0, i + 1)
            const rootEdges = prevPath.edges.slice(0, i)
            const bannedEdges = new Set()

            for (const path of result) {
                if (path.nodes.length > i && prefixEqual(path.nodes, rootNodes)) {
                    bannedEdges.add(path.edges[i])
                }
            }
            const bannedNodes = new Set(rootNodes.slice(0, -1))
            const spur = dijkstra(net, prevPath.nodes[i], end, bannedEdges, bannedNodes)
            if (!spur) continue

            const nodes = [...rootNodes.slice(0, -1), ...spur.nodes]
            const edges = [...rootEdges, ...spur.edges]
            const key = pathKey(nodes)
            if (seen.has(key)) continue
            seen.add(key)
            candidates.push({nodes, edges, cost: pathCost(net, edges)})
        }

        if (!candidates.length) break
        candidates.sort((a, b) => a.cost - b.cost)
        result.push(candidates.shift())
    }
    return result
}

function orientedPoints(road, fromNode) {
    return road.from === fromNode ? road.points : [...road.points].reverse()
}

function direction(a, b) {
    return {x: b[0] - a[0], y: b[1] - a[1]}
}

/**
 * 屏幕坐标系 y 轴向下：叉积为正表示顺时针，即右转。
 */
function turnText(net, prevEdgeId, prevFrom, nextEdgeId, nextFrom) {
    const prevRoad = net.roadsById.get(prevEdgeId)
    const nextRoad = net.roadsById.get(nextEdgeId)
    if (!prevRoad || !nextRoad) return {text: '继续', degrees: 0}

    const prevPts = orientedPoints(prevRoad, prevFrom)
    const nextPts = orientedPoints(nextRoad, nextFrom)
    if (prevPts.length < 2 || nextPts.length < 2) return {text: '继续', degrees: 0}

    const v1 = direction(prevPts[prevPts.length - 2], prevPts[prevPts.length - 1])
    const v2 = direction(nextPts[0], nextPts[1])
    const len1 = Math.hypot(v1.x, v1.y)
    const len2 = Math.hypot(v2.x, v2.y)
    if (!len1 || !len2) return {text: '继续', degrees: 0}

    const cross = v1.x * v2.y - v1.y * v2.x
    const dot = v1.x * v2.x + v1.y * v2.y
    const degrees = Math.abs((Math.atan2(cross, dot) * 180) / Math.PI)

    if (degrees < 25) return {text: '直行', degrees}
    if (degrees > 150) return {text: '掉头', degrees}
    return {text: cross > 0 ? '右转' : '左转', degrees}
}

function stitchPoints(net, nodes, edges) {
    const points = []
    for (let i = 0; i < edges.length; i++) {
        const road = net.roadsById.get(edges[i])
        if (!road) continue
        const pts = orientedPoints(road, nodes[i])
        if (points.length) points.push(...pts.slice(1))
        else points.push(...pts)
    }
    return points
}

function fmtMeters(meters) {
    if (meters >= 1000) return `${(meters / 1000).toFixed(2)} 公里`
    return `${Math.round(meters)} 米`
}

/**
 * 把 Dijkstra/Yen 的节点 + 边序列还原成可绘制、可展示的路线。
 */
export function routeDetail(net, nodePath, edgePath) {
    const points = stitchPoints(net, nodePath, edgePath)
    const roads = []
    let distance = 0
    let seconds = edgePath.length * JUNCTION_DELAY

    for (const id of edgePath) {
        const road = net.roadsById.get(id)
        if (!road) continue
        const speedMs = (road.speed ?? LEVEL_SPEED[road.level] ?? 15) / 3.6
        distance += road.distance
        seconds += road.distance / speedMs
        roads.push({id: road.id, name: road.name, group: road.group, level: road.level, distance: road.distance})
    }

    const steps = []
    const startNode = net.nodes.get(nodePath[0])
    const endNode = net.nodes.get(nodePath[nodePath.length - 1])
    steps.push(`从「${startNode?.name || '起点'}」出发`)

    for (let i = 0; i < roads.length; i++) {
        if (i === 0) {
            steps.push(`沿「${roads[i].name}」行驶 ${fmtMeters(roads[i].distance)}`)
            continue
        }
        const turn = turnText(net, edgePath[i - 1], nodePath[i - 1], edgePath[i], nodePath[i])
        if (turn.degrees > 25) {
            seconds += turn.degrees > 100 ? TURN_DELAY_LARGE : TURN_DELAY_SMALL
        }
        steps.push(`${turn.text}进入「${roads[i].name}」，继续行驶 ${fmtMeters(roads[i].distance)}`)
    }
    steps.push(`到达「${endNode?.name || '终点'}」`)

    const gates = nodePath
        .slice(1, -1)
        .map((id) => net.nodes.get(id))
        .filter((node) => node?.type === 'gate')
        .map((node) => node.name)

    return {points, distance, seconds, roads, gates, steps}
}

/**
 * 拼接两段路线（如「起点 → 磅房」+「磅房 → 装卸点」），用于按物资寻路。
 */
export function mergeDetails(first, second, viaName) {
    const points = [...first.points]
    for (const point of second.points) {
        const last = points[points.length - 1]
        if (!last || last[0] !== point[0] || last[1] !== point[1]) points.push(point)
    }

    const steps = [...first.steps.slice(0, -1)]
    if (viaName) steps.push(`在「${viaName}」过磅称重`)
    steps.push(...second.steps)

    return {
        points,
        distance: first.distance + second.distance,
        seconds: first.seconds + second.seconds,
        roads: [...first.roads, ...second.roads],
        gates: [...new Set([...first.gates, ...second.gates])],
        steps,
    }
}
