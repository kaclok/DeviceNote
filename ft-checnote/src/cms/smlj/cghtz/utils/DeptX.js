/**
 * 组织架构（部门字典）通用工具
 *
 * 字典来自 /cghtz/dept/list，后端数据源是 train.t_org（@DS("train")），
 * 每条记录形如 {dept_code, dept_name, dept_all_name, parent_dept_code}。
 *
 * 关键：train.t_org 的 org_all_name 只覆盖部分节点（大量叶子为空、且最多两级），
 * 所以「公司/部门」全路径统一由本模块按 parent_dept_code 自行拼接，不依赖该字段。
 */

/** 扁平字典 → 组织树。parent_dept_code 找不到对应节点者视为根节点。 */
export function buildDeptTree(list) {
    const src = Array.isArray(list) ? list : []
    const map = new Map()
    src.forEach(d => map.set(d.dept_code, {...d, children: []}))
    const roots = []
    src.forEach(d => {
        const node = map.get(d.dept_code)
        const parent = map.get(d.parent_dept_code)
        if (parent && parent !== node) parent.children.push(node)
        else roots.push(node)
    })
    // 删掉空 children，避免 el-tree 给叶子节点也画展开箭头；
    // seen 兜底防御脏数据造成的环，防止无限递归
    const seen = new Set()
    const prune = n => {
        if (!n || seen.has(n)) return
        seen.add(n)
        if (!n.children.length) delete n.children
        else n.children.forEach(prune)
    }
    roots.forEach(prune)
    return roots
}

/**
 * 部门编码 → 「公司/部门」全路径。
 * 例：1030015006 → 陕西金泰化学科技集团有限公司/金泰化学本部/采购供应部
 */
export function buildDeptPathMap(list) {
    const src = Array.isArray(list) ? list : []
    const byCode = new Map()
    src.forEach(d => byCode.set(d.dept_code, d))
    const paths = {}
    const resolve = (code, depth = 0) => {
        if (!code || depth > 20) return ''
        if (paths[code] !== undefined) return paths[code]
        const d = byCode.get(code)
        if (!d) return ''
        paths[code] = ''   // 先占位，防御脏数据形成的环
        const parent = byCode.get(d.parent_dept_code)
        const prefix = parent && parent.dept_code !== code ? resolve(parent.dept_code, depth + 1) : ''
        paths[code] = prefix ? `${prefix}/${d.dept_name || ''}` : (d.dept_name || '')
        return paths[code]
    }
    src.forEach(d => resolve(d.dept_code))
    return paths
}

/**
 * 部门编码 → 展示文本（表格/详情回显用）。
 * - 命中字典 → 「公司/部门」全路径
 * - 未命中（字典未加载完 / 该部门不在启用集合里）→ 「未知部门(code)」
 *
 * 刻意不再静默退回裸编码：编码长得像正常数据，会把"字典没对齐"整个掩盖掉。
 */
export function deptDisplay(pathMap, code) {
    if (!code) return ''
    return (pathMap && pathMap[code]) || `未知部门(${code})`
}

/** 该部门编码是否未在字典中命中（供 UI 标灰，提示字典与业务数据不一致） */
export function isUnknownDept(pathMap, code) {
    return !!code && !(pathMap && pathMap[code])
}

/**
 * 「公司/部门」全路径的**末级**（只取部门名本身）。
 * 列表列位窄，只展示部门名更清爽；完整「公司/部门」路径交给 tooltip。
 * 未命中字典时与 deptDisplay 保持一致，返回「未知部门(code)」。
 */
export function deptShort(pathMap, code) {
    if (!code) return ''
    const full = (pathMap && pathMap[code]) || ''
    if (!full) return `未知部门(${code})`
    const segs = full.split('/').filter(Boolean)
    return segs.length ? segs[segs.length - 1] : full
}

/** 关键字匹配：部门名 / 全路径 / 部门编码 任一命中即算符合（大小写不敏感） */
export function matchDept(d, kw, pathMap) {
    const k = String(kw || '').trim().toLowerCase()
    if (!k) return true
    if (!d) return false
    const full = (pathMap && pathMap[d.dept_code]) || d.dept_all_name || ''
    return String(d.dept_name || '').toLowerCase().includes(k)
        || String(full).toLowerCase().includes(k)
        || String(d.dept_code || '').toLowerCase().includes(k)
}
