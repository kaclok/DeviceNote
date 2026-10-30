import gd from "../data/gd.json"

/**
 * 逐页面分页参数（唯一来源 gd.json#pager）。
 *
 * 每个页面在 gd.json 的 pager 下各有一段独立配置：
 *   "spotcheck": {"pageSize": 14, "pageSizes": [14, 30, 50, 100]}
 * 刻意不做「全局共用」，任意一页调参不影响其它页；下面的 14 / [14,30,50,100]
 * 只是键缺失时的兜底，正常路径一律读配置。
 */
export function pagerOf(key) {
    const cfg = (gd.pager || {})[key] || {}
    const size = Number(cfg.pageSize)
    const sizes = Array.isArray(cfg.pageSizes) && cfg.pageSizes.length > 0 ? cfg.pageSizes : [14, 30, 50, 100]
    return {pageSize: Number.isFinite(size) && size > 0 ? size : 14, pageSizes: sizes}
}
