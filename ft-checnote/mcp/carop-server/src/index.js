#!/usr/bin/env node
/**
 * 车道道闸控制(carop) MCP Server —— stdio 传输
 *
 * 能力来源：前端 carop 入口(ft-checnote/src/cms/smlj/carop)所对应的后端接口
 *   POST {CAROP_BASE_URL}/carOp/openDoor?laneId=&status=
 *   status: 0=开启道闸  1=常开锁定  2=解锁恢复
 *
 * 两条必须遵守的约束：
 *   1. stdio 下 stdout 只能输出 MCP 协议消息 —— 本进程内任何日志都不能用 console.log，必须 console.error
 *   2. lane_control 会真实驱动现场道闸(抬杆/锁定)，因此强制二次确认 + 审计留痕
 */
import {McpServer} from '@modelcontextprotocol/sdk/server/mcp.js'
import {StdioServerTransport} from '@modelcontextprotocol/sdk/server/stdio.js'
import {z} from 'zod'

import {config} from './config.js'
import {loadGd, findLane, findStatus, callOpenDoor} from './carop.js'
import {writeAudit} from './audit.js'

const ok = (text) => ({content: [{type: 'text', text}]})
const fail = (text) => ({content: [{type: 'text', text}], isError: true})

const server = new McpServer({name: 'carop-mcp-server', version: '1.0.0'})

/** 当前 MCP 客户端信息(Trae / Claude Desktop 等)，写进审计日志用于追溯调用来源 */
function clientInfo() {
    try {
        return server.server.getClientVersion() ?? null
    } catch {
        return null
    }
}

/* ---------------- 工具 1：车道与操作字典 ---------------- */
server.tool(
    'list_lanes',
    '列出车道闸控制支持的所有车道(含 id 与中文描述)，以及可选操作。' +
    '调用 lane_control 之前应先调用它，确认目标车道的 lane_id 与描述。',
    {},
    async () => {
        try {
            const {lanes, statuses} = loadGd()
            return ok([
                `可用车道 (共 ${lanes.length} 条)：`,
                ...lanes.map(l => `- ${l.id}  ${l.desc}`),
                '',
                '可选操作 (lane_control 的 status 参数)：',
                ...statuses.map(s => `- ${s.id}  ${s.status}`),
            ].join('\n'))
        } catch (e) {
            return fail(`读取车道字典失败：${e.message}`)
        }
    }
)

/* ---------------- 工具 2：道闸控制（物理动作） ---------------- */
server.tool(
    'lane_control',
    '控制车道道闸 —— 真实物理动作，会驱动现场道闸。' +
    'status: 0=开启道闸(抬杆放行一次)、1=常开锁定(道闸保持抬起不自动落杆)、2=解锁恢复(恢复正常自动落杆)。' +
    '使用前必须先调用 list_lanes 取得 lane_id 及其中文描述，并把该描述原样填入 confirm_text 完成二次确认；' +
    'confirm_text 与车道描述不一致时，本次调用会被拒绝且不会下发任何指令。',
    {
        lane_id: z.number().int().describe('车道 id，取自 list_lanes 返回的 id'),
        status: z.number().int().min(0).max(2).describe('0=开启道闸 1=常开锁定 2=解锁恢复'),
        confirm_text: z.string().describe('二次确认：必须原样传入目标车道的中文描述，例如 "1号门出口"'),
        reason: z.string().optional().describe('本次操作的原因(如 "放行XX车辆")，写入审计日志便于事后追溯'),
    },
    async ({lane_id, status, confirm_text, reason}) => {
        const trace = {
            tool: 'lane_control',
            lane_id,
            status,
            confirm_text,
            reason: reason ?? null,
            client: clientInfo(),
        }

        let lane
        let statusItem
        let gd
        try {
            gd = loadGd()
            lane = findLane(lane_id)
            statusItem = findStatus(status)
        } catch (e) {
            writeAudit({...trace, ok: false, stage: 'load_dict_failed', error: e.message})
            return fail(`读取车道字典失败：${e.message}`)
        }

        // 校验 1：车道必须存在（把可用值回给模型，便于纠正后重试）
        if (!lane) {
            writeAudit({...trace, ok: false, stage: 'reject_lane_not_found'})
            return fail(`车道 ${lane_id} 不存在，未下发任何指令。可用车道：\n` +
                gd.lanes.map(l => `- ${l.id}  ${l.desc}`).join('\n'))
        }

        // 校验 2：操作必须存在
        if (!statusItem) {
            writeAudit({...trace, ok: false, stage: 'reject_status_invalid'})
            return fail(`status=${status} 不合法，未下发任何指令。可选操作：\n` +
                gd.statuses.map(s => `- ${s.id}  ${s.status}`).join('\n'))
        }

        // 校验 3：二次确认 —— confirm_text 必须与车道描述完全一致，防止"打错车道"
        if (confirm_text !== lane.desc) {
            writeAudit({...trace, lane_desc: lane.desc, ok: false, stage: 'reject_confirm_mismatch'})
            return fail(`二次确认不通过：车道 ${lane.id} 的描述是「${lane.desc}」，` +
                `confirm_text 必须原样传入该字符串（当前传入「${confirm_text}」）。本次未下发任何指令。`)
        }

        // 三道校验都过了，才真正下发指令
        try {
            const {httpStatus, body, raw} = await callOpenDoor(lane.id, statusItem.id)
            const bizOk = httpStatus === 200 && body?.code === 200 && body?.data === true

            writeAudit({
                ...trace,
                lane_desc: lane.desc,
                status_desc: statusItem.status,
                ok: bizOk,
                stage: 'executed',
                http_status: httpStatus,
                backend_code: body?.code ?? null,
                backend_data: body?.data ?? null,
                backend_message: body?.message ?? null,
            })

            if (!bizOk) {
                // 注意：后端 openDoor 失败时返回的是 {code:200, data:false}，HTTP 仍是 200
                return fail(`道闸控制未成功。车道：${lane.desc}；操作：${statusItem.status}；` +
                    `后端响应：HTTP ${httpStatus} ${raw}`)
            }
            return ok(`已下发指令：${lane.desc} → ${statusItem.status}。`)
        } catch (e) {
            const aborted = e?.name === 'AbortError'
            writeAudit({...trace, lane_desc: lane.desc, ok: false, stage: 'error', error: e?.message})
            return fail(aborted
                ? `调用后端超时(${config.requestTimeoutMs}ms)：${config.backendUrl}，指令可能未下发。`
                : `调用后端失败：${e?.message}`)
        }
    }
)

/* ---------------- 启动 ---------------- */
await server.connect(new StdioServerTransport())

// 启动自检：字典读不到时只提示、不退出，让工具调用时能返回可读错误
try {
    const {lanes, statuses} = loadGd()
    console.error(`[carop-mcp] 已就绪 | 后端=${config.backendUrl} | 车道=${lanes.length} 条 | 操作=${statuses.length} 种`)
} catch (e) {
    console.error(`[carop-mcp] ⚠️ 车道字典读取失败：${e.message}`)
}
console.error(`[carop-mcp] 字典=${config.gdJsonPath}`)
console.error(`[carop-mcp] 审计=${config.auditLogPath}`)
