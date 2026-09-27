// 审计日志：道闸控制(物理动作)的每次调用都留痕，含被拒绝的调用
import {appendFileSync, mkdirSync} from 'node:fs'
import {dirname} from 'node:path'
import {config} from './config.js'

/**
 * 追加一行 JSONL 审计记录。
 * 写文件失败不影响主流程(否则磁盘问题会导致指令发不出去)，但会在 stderr 明确提示。
 */
export function writeAudit(record) {
    const line = JSON.stringify({
        ts: new Date().toISOString(),
        os_user: process.env.USERNAME || process.env.USER || null,
        ...record,
    })

    // 同步回显到 stderr，便于本地调试与客户端日志收集
    console.error(`[carop-mcp][audit] ${line}`)

    try {
        mkdirSync(dirname(config.auditLogPath), {recursive: true})
        appendFileSync(config.auditLogPath, line + '\n', 'utf8')
    } catch (e) {
        console.error(`[carop-mcp] 审计日志写入失败: ${e.message}`)
    }
}
