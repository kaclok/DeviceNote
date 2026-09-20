export class TreeUtil {
    //  扁平列表 → 树
    static array2Tree(array, {getId, getParentId, rootId = null, childrenKeyName = 'children'}) {
        const src = Array.isArray(array) ? array : []
        const map = new Map(src.map(i => [getId(i), {...i}]))
        const roots = []
        for (const i of src) {
            const id = getId(i)
            const parentId = getParentId(i)
            const node = map.get(id)
            const parent = map.get(parentId)

            const isRoot = parentId === rootId || !parent || parent === node
            if (isRoot) {
                roots.push(node)
            } else {
                // 第一次有子节点时才创建数组
                ;(parent[childrenKeyName] ||= []).push(node)
            }
        }
        return roots
    }
}

// export function buildDeptTree(list) {
//     return TreeUtil.array2Tree(list, {
//         getId: (dept) => dept.dept_code, getParentId: (dept) => dept.parent_dept_code, // 如果根部门的 parent_dept_code 是 0，则填写 0；
//         // 如果父编码不存在就自然视为根，可不传或传 null
//         rootId: 0
//     })
// }
