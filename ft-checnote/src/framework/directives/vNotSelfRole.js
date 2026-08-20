// 自定义指令 v-not-self
// 用法：<el-button v-not-self="row.account">操作</el-button>
// 当绑定值（行账号）等于当前登录账号时，移除该元素，禁止操作自己。
// 场景：账号管理页中 admin 不能编辑/重置密码/停用自己的账号

import {ECacheType, useCache} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useCache()

const directive = {
    mounted(el, binding) {
        const {value, modifiers} = binding
        // 当前登录账号
        const currentRoleCode = wsCache.get(ECacheType.ACCOUNT)?.role.role_code
        // 绑定值与当前账号相同 => 禁止操作自己，移除元素
        if (!value || value !== currentRoleCode) {
            return
        }

        if (modifiers.readonly) {
            // 只读模式：可见但禁用操作
            // el-button 渲染为原生 <button>，设置 disabled 可阻止 click 事件触发
            el.disabled = true
            // 同步 Element Plus 的禁用样式类
            el.classList.add('is-disabled')
            el.style.cursor = 'not-allowed'
        } else {
            // 默认模式：直接移除元素
            el.remove()
        }
    },
}

export default {
    directive,
}
