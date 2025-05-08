import {useZIndex} from '@/framework/composable/use/useZIndex.js'

const directive = {
    mounted(el, bindings) {
        // 参数传递的base数值
        const base = bindings.value?.base || __Z_BASE__
        const step = bindings.value?.step || __Z_STEP__
        const {next} = useZIndex(base, step)
        el.style.zIndex = next().toString()

        // 存储到元素上以便更新时使用
        el._zIndexCtx = {next}
    }, updated(el) {
        if (el._zIndexCtx) {
            el.style.zIndex = el._zIndexCtx.next().toString()
        }
    }
}

/*
<template>
    <!-- 指定基准值 -->
    <div v-z-index="{ base: 2000 }">从2000开始递增</div>
    <!-- 不指定基准值 -->
    <div v-z-index></div>
</template>
*/

export default {
    directive
}