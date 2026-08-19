// 自定义指令 v-not-self
// 当绑定值（行账号）等于当前登录账号时，禁止操作自己。
//
// 两种模式：
// 1. 默认（移除）：v-not-self="row.account"            => 直接从 DOM 移除元素
// 2. 只读（禁用）：v-not-self.readonly="row.account"    => 元素可见但不可操作（el.disabled = true）
//
// 场景：账号管理页中 admin 不能编辑/重置密码/停用自己的账号

import {useCache, ECacheType} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useCache()

const directive = {
    mounted(el, binding) {
        const {value, modifiers} = binding
        // 当前登录账号
        const currentAccount = wsCache.get(ECacheType.ACCOUNT)?.account
        // 绑定值与当前账号不同 => 不处理
        if (!value || value !== currentAccount) {
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
