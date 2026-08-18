// https://cn.vuejs.org/guide/reusability/custom-directives.html

// v-hasRole="['super_admin']" 或 v-hasRole="'inputter'"
// 判断当前用户是否拥有任一指定角色（按 role_code 匹配），无则移除元素。
//
// 数据来源：ECacheType.ROLES，role 为数组，元素结构 { role_name, role_code, dataScope, perms }，

import {useCache, ECacheType} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useCache()

const directive = {
    mounted(el, binding) {
        const {value} = binding
        if (value == null || (Array.isArray(value) && value.length === 0)) {
            return
        }

        const allowed = Array.isArray(value) ? value : [value]
        const userRole = wsCache.get(ECacheType.ACCOUNT)?.role.role_code
        const hasRole = allowed.includes(userRole);

        // ❌ 没有角色：从 DOM 中移除元素
        if (!hasRole) {
            el.remove() // 从父节点中移除el自己, 作用同el.parentNode?.removeChild(el)
        }
    },
};

export default {
    directive,
}
