// carop 后端调用 + 车道字典读取
import {readFileSync} from 'node:fs'
import {config} from './config.js'

let _gd = null

/**
 * 读取车道/操作字典(前端 gd.json)，首次读取后缓存在内存。
 * 注意 gd.json 的键名原文就是 lans / lanStatus。
 * 读不到或解析失败会抛异常，由调用方转成可读的 tool 错误返回。
 */
export function loadGd() {
    if (_gd) return _gd
    const parsed = JSON.parse(readFileSync(config.gdJsonPath, 'utf8'))
    _gd = {
        lanes: parsed.lans ?? [],
        statuses: parsed.lanStatus ?? [],
    }
    return _gd
}

export function findLane(laneId) {
    const id = Number(laneId)
    return loadGd().lanes.find(l => Number(l.id) === id) ?? null
}

export function findStatus(statusId) {
    const id = Number(statusId)
    return loadGd().statuses.find(s => Number(s.id) === id) ?? null
}

/**
 * 调用后端 POST /carOp/openDoor?laneId=&status=
 * @returns {Promise<{httpStatus:number, body:any, raw:string}>}
 */
export async function callOpenDoor(laneId, status) {
    // 归一化 base，保证相对路径拼在根上(即使配置里带子路径也不会丢掉)
    const base = config.backendUrl.endsWith('/') ? config.backendUrl : config.backendUrl + '/'
    const url = new URL('carOp/openDoor', base)
    url.searchParams.set('laneId', String(laneId))
    url.searchParams.set('status', String(status))

    const ac = new AbortController()
    const timer = setTimeout(() => ac.abort(), config.requestTimeoutMs)
    try {
        const resp = await fetch(url, {method: 'POST', signal: ac.signal})
        const raw = await resp.text()
        let body = null
        try {
            body = JSON.parse(raw)
        } catch {
            // 非 JSON 响应(网关错误页/代理返回 HTML)，body 留 null，由调用方按 raw 展示
        }
        return {httpStatus: resp.status, body, raw}
    } finally {
        clearTimeout(timer)
    }
}
