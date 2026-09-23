/**
 * 组织架构（部门字典）通用工具
 *
 * 字典来自 /cghtz/dept/list，后端数据源是 train.t_org（@DS("train")），
 * 每条记录形如 {dept_code, dept_name, dept_all_name, parent_dept_code}。
 *
 * 关键：train.t_org 的 org_all_name 只覆盖部分节点（大量叶子为空、且最多两级），
 * 所以「公司/部门」全路径统一由本模块按 parent_dept_code 自行拼接，不依赖该字段。
 */

import gd from "../data/gd.json"

/** 扁平字典 → 组织树。parent_dept_code 找不到对应节点者视为根节点。 */
export function buildDeptTree(list) {
    const src = Array.isArray(list) ? list : []
    // 先只建节点本身，不带 children
    const map = new Map(src.map(d => [d.dept_code, { ...d }]))
    const roots = []
    for (const d of src) {
        const node = map.get(d.dept_code)
        const parent = map.get(d.parent_dept_code)
        if (parent && parent !== node) {
            // 第一次有子节点时才创建数组
            ;(parent.children ||= []).push(node)
        } else {
            roots.push(node)
        }
    }
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

/* ---------------- 数据范围（data_scope） ---------------- */

/**
 * 档位常量，与后端 CCGHT.SCOPE_* 一一对应。
 * ⚠️ 编号即**严格**包含序（2 ⊂ 3 ⊂ 4）：前端「不能分配高于自身的范围」靠这个顺序做数值比较，别乱改。
 *   2 本部门已含全部下级，所以没有单独的「本部门及下级」档 —— 两者展开结果完全一致。
 * 编号与文案的唯一来源 = data/gd.json 的 dataScope.levels：
 *   改档位要同步后端 CCGHT.SCOPE_* 常量与库里 t_user.data_scope 的存量值。
 */
export const SCOPE = Object.fromEntries(gd.dataScope.levels.map(l => [l.name, l.id]))

/** 组织树索引：byCode 用于上溯、children 用于下探 */
function _deptIndex(list) {
    const src = Array.isArray(list) ? list : []
    const byCode = new Map()
    const children = new Map()
    src.forEach(d => byCode.set(d.dept_code, d))
    src.forEach(d => {
        const p = d.parent_dept_code
        // 父必须真实存在于字典且不是自己，避免脏数据造出环
        if (p && byCode.has(p) && p !== d.dept_code) {
            if (!children.has(p)) children.set(p, [])
            children.get(p).push(d.dept_code)
        }
    })
    return {src, byCode, children}
}

/**
 * 按数据范围算出「可见/可选」的部门列表。
 *
 * 与后端 CCGHT.expandScope 同一口径（档位编号即严格包含序）：
 *   4 全集团 -> 不限制，返回 null（调用方直接用全量字典）
 *   3 本公司 -> 起点向上定位到「公司节点」（集团根的直接子），取其整棵子树
 *   2 本部门 -> 起点整棵子树（含下级部门 / 分厂 / 中心等更深层）
 *   1 本人   -> 部门边界与 2 本部门 相同；可见性由后端再叠加「归属=本人」那一维
 *               （合同比 sign_person、账号比 account），前端这一层只负责收窄候选
 *   取不到起点 / 未知档位 -> 空数组（fail-closed，与后端一致）
 *
 * 第 3 参传账号自己的 dept_code：展开起点就是人事归属部门。
 * （曾经还有「范围锚点」scope_dept_code，已随列删除。）
 *
 * ⚠️ 这里只是把下拉候选收窄，属于体验优化，**不构成安全边界** ——
 * 真正的拦截在后端，改前端参数绕不过后端的范围校验。
 */
export function deptScopeDepts(list, dataScope, deptCode) {
    const scope = Number(dataScope)
    if (scope === SCOPE.ALL) return null
    if (!deptCode) return []
    const {src, byCode, children} = _deptIndex(list)

    /** 某节点的整棵子树（含自身），out.has 兼作防重复入队与防环 */
    const subtree = rootCode => {
        const out = new Set()
        if (!rootCode) return out
        const queue = [rootCode]
        while (queue.length) {
            const cur = queue.shift()
            if (out.has(cur)) continue
            out.add(cur)
            ;(children.get(cur) || []).forEach(c => {
                if (!out.has(c)) queue.push(c)
            })
        }
        return out
    }

    let allow
    if (scope === SCOPE.DEPT || scope === SCOPE.SELF) {
        // 本部门：起点整棵子树（部门天然含下级 —— 这正是没有「本部门及下级」档的原因）。
        // 「本人」档的**部门边界**与它一致（这一维同时是写入边界，不能收窄），可见性由后端
        // 另叠加「归属=本人」；前端跟着一致，避免两侧对"能选哪个部门"的判断分叉。
        allow = subtree(deptCode)
    } else if (scope === SCOPE.COMPANY) {
        // 集团根：没有父、或父不在字典里的那个节点
        let root = ''
        for (const d of src) {
            const p = d.parent_dept_code
            if (!p || !byCode.has(p)) {
                root = d.dept_code
                break
            }
        }
        // 从起点向上走到「公司节点」（父正好是集团根的那一层）。
        // 「父不在字典里」同样算到顶 —— 集团根的父是 1、而 1 不在启用集合里，
        // 只看"父为空"会走出树外，得到比「本部门」档还窄的结果（与后端 companyOf 同一处修正）。
        let cur = deptCode
        for (let i = 0; i < 16 && cur; i++) {
            const d = byCode.get(cur)
            const p = d && d.parent_dept_code
            if (!p || !byCode.has(p)) {                       // 已到顶：公司即自身
                allow = subtree(cur)
                break
            }
            if (root && p === root) {
                allow = subtree(cur)
                break
            }
            cur = p
        }
    }
    // 未知档位 / 定位失败：都只给起点（只会收窄，不会放大）
    if (!allow) allow = new Set([deptCode])
    return src.filter(d => allow.has(d.dept_code))
}

/**
 * 「可见集 + 祖先链」→ 带路径的组织树（组织架构展示 / 逐层选择用）。
 *
 * 为什么需要它：可见集是按 data_scope 展开出来的一片部门，把它直接交给 buildDeptTree，
 * 父节点不在集合里 → 每个节点都会被当成根节点，树上只剩孤零零一个部门，看不出层级。
 * 这里把每个可见节点沿 parent_dept_code 上溯到根补齐，路径节点只作层级上下文（selectable=false），
 * 只有可见集内的节点可选。
 *
 * ⚠️ list 必须是**全量**字典，否则上溯会断在半路。
 *
 * @param {Array} list 全量部门字典（dept_code / dept_name / parent_dept_code）
 * @param {Set|null} visibleCodes 可见部门编码集合；**null = 不限制（全量可选）**，空集 = 全不可选
 * @returns {Array} 树节点数组，每个节点额外带 selectable: boolean
 */
export function buildScopedDeptTree(list, visibleCodes) {
    const src = Array.isArray(list) ? list : []
    const scopeAll = visibleCodes === null
    const byCode = new Map(src.map(d => [d.dept_code, d]))

    let shown = src
    if (!scopeAll) {
        // 可见节点 ∪ 其祖先链。keep 兼作「这条链已补过」的剪枝标记，整体是 O(n)。
        const keep = new Set()
        visibleCodes.forEach(code => {
            let cur = code
            for (let i = 0; i < 32 && cur && !keep.has(cur); i++) {
                keep.add(cur)
                const d = byCode.get(cur)
                cur = d ? d.parent_dept_code : ''
            }
        })
        shown = src.filter(d => keep.has(d.dept_code))
    }

    const tree = buildDeptTree(shown)
    const mark = nodes => nodes.forEach(n => {
        n.selectable = scopeAll || visibleCodes.has(n.dept_code)
        if (n.children && n.children.length) mark(n.children)
    })
    mark(tree)
    return tree
}

/* ---------------- 数据范围（范围的唯一来源 = 账号行） ---------------- */

/** 档位 → 中文文案，唯一来源 = data/gd.json（与 SCOPE 同一份 levels） */
const SCOPE_TEXT = Object.fromEntries(gd.dataScope.levels.map(l => [String(l.id), l.desc]))

/**
 * 账号的「有效数据范围」：只取账号行自己的 data_scope —— 它是范围的唯一来源。
 * 与后端 CCGHT.dataScopeOf 同口径（角色侧已无该字段）。
 * 前端只用于收窄下拉与展示，不构成安全边界。
 */
export function effectiveScope(acct) {
    const v = Number(acct?.data_scope ?? 0)
    return Number.isFinite(v) ? v : 0
}

/** 档位 → 中文文案；未配置/未知档位返回「未配置」 */
export function scopeText(scope) {
    return SCOPE_TEXT[Number(scope)] || '未配置'
}

/* ---------------- 合同模版 × 部门：组织树过滤 + 节点标签 ---------------- */

/**
 * 各模版 → 持有部门的**原始配对** —— 「合同模板 × 部门」这一口径的唯一来源。
 *
 * 口径：一个部门持有模板 ⇔ 它**自己**绑定了某套模板（t_dept_contract_template 的 PK 是 dept_code，
 * 故一个部门至多一套）。系统**不存在**部门间继承（见 CCGHT.holderGroups）—— 上级配了模板不会
 * 顺延给下级，所以这里就是把各模版的 dept_codes 收集起来，不做任何组织树扩散。
 * 与后端 /template/list 同源 —— 于是"树上能选的部门"「节点上挂的模板名」与"后端认的持有部门"
 * 天然一致，不会一个说行一个说不行。
 *
 * 返回 null = **给不出可信结论**（旧缓存 / 后端漏发），调用方自行决定退让策略：
 * 收窄（tplHolderCodes）宁可不收窄，装饰（deptTplBadges）宁可不挂标签。
 *
 * 为什么不能只看 dept_codes 在不在：后端全局 NON_EMPTY 会把**空数组整条丢掉**，
 * 于是"没人持有"与"字段没下发"在 JSON 里长得一模一样 —— 只能靠永不为空的 holder_count 判。
 */
function tplDeptPairs(tplList) {
    const src = Array.isArray(tplList) ? tplList : []
    const pairs = []
    for (const t of src) {
        const n = Number(t?.holder_count)
        if (!Number.isFinite(n)) return null       // 旧缓存 / 后端漏发：不给结论
        if (n <= 0) continue                       // 这套模版没人持有（dept_codes 已被 NON_EMPTY 吞掉）
        const codes = t?.dept_codes
        if (!Array.isArray(codes)) return null     // 说有 n 个却没给数组 —— 自相矛盾，同样不给结论
        codes.forEach(c => {
            if (c) pairs.push([String(c), t])
        })
    }
    return pairs
}

/**
 * 「持有合同模板」的部门全集 —— 组织树的「仅看有模板」用它收窄。
 *
 * 三态（与 buildScopedDeptTree 第 2 参同语义）：
 *   Set  —— 拿得到结论；**空集**表示"范围内没有任何部门配置模板"（调用方据此显示空树）
 *   null —— 给不出结论；调用方**不要收窄**（退回可见集），否则一次取数失败就把整棵树清空
 */
export function tplHolderCodes(tplList) {
    const pairs = tplDeptPairs(tplList)
    return pairs === null ? null : new Set(pairs.map(p => p[0]))
}

/**
 * 部门 → 持有的合同模版（**对象**）—— 「节点标签」与「点部门补选模版」共用这一份映射。
 *
 * 与 tplHolderCodes 同源同口径（同一个 tplDeptPairs），区别只是这里要保留"哪个部门对应哪套模版"。
 * 一处部门至多一套模版（PK = dept_code），映射本应单值；万一上游真出现冲突，取先到者，
 * 免得被后到的覆盖成不稳定结果（同一次取数两次渲染挂出不同的名字）。
 *
 * 返回普通对象而非三态：两个消费点都是**装饰/便利**性质 —— 取不到就少挂几个标签、少补一次模版，
 * 绝不会因此把树画错（"要不要收窄树"的退让策略由 tplHolderCodes 单独决定，别混）。
 */
export function deptTplMap(tplList) {
    const pairs = tplDeptPairs(tplList)
    const m = {}
    if (pairs === null) return m
    for (const [code, t] of pairs) {
        if (!(code in m)) m[code] = t
    }
    return m
}

/**
 * 部门 → 持有的合同模版名 —— 组织树**节点标签**用它。
 *
 * 名字从 deptTplMap 取，所以"挂得出标签的部门"必然也是"能补出模版对象的部门" ——
 * 台账页点部门补选模版时，不会出现"看着有标签、点下去却补不出来"的落差。
 * name 缺失的模版不挂标签（不拿 #id 之类充数），但它在 deptTplMap 里仍有一席之地。
 */
export function deptTplBadges(tplList) {
    const m = {}
    for (const [code, t] of Object.entries(deptTplMap(tplList))) {
        if (t?.name) m[code] = t.name
    }
    return m
}
