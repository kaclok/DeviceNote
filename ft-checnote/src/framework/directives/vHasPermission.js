// https://cn.vuejs.org/guide/reusability/custom-directives.html

import {ECacheType, useCache} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useCache()
const directive = {
    mounted(el, binding) {
        const {value} = binding
        if (value == null || (Array.isArray(value) && value.length === 0)) {
            return
        }

        const allowed = Array.isArray(value) ? value : [value]
        // 从缓存中获取用户角色列表
        const userPerms = wsCache.get(ECacheType.ALL_PERMS) || [];
        // 确保allowed是userPerms的子集
        const hasPerm = allowed.every(perm => {
            return userPerms.includes(perm)
        })

        // ❌ 没有权限：从 DOM 中移除元素
        if (!hasPerm) {
            el.remove() // 从父节点中移除el自己, 作用同el.parentNode?.removeChild(el)
        }
    },
};

export default {
    directive,
}
