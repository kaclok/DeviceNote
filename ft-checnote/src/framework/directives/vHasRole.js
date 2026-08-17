// https://cn.vuejs.org/guide/reusability/custom-directives.html

import {useCache, ECacheType} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useCache()
const directive = {
    mounted(el, binding) {
        const {value} = binding
        // 从缓存中获取用户角色列表
        const roles = wsCache.get(ECacheType.ROLES) || []
        // 检查用户是否有任一角色在允许列表中
        const hasRole = roles.some(role => {
            return value.includes(role)
        })

        // ❌ 没有权限：从 DOM 中移除元素
        if (!hasRole) {
            el.remove() // 从父节点中移除el自己, 作用同el.parentNode?.removeChild(el)
        }
    },
};

export default {
    directive,
}
