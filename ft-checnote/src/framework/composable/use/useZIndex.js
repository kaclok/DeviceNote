// import { useZIndex } from 'element-plus';
// Element Plus有自己的useZIndex
import {inject, provide, ref} from 'vue'

function useZIndex(base = __Z_BASE__, step = __Z_STEP__) {
    const currentZIndex = ref(base)

    const next = () => {
        currentZIndex.value += step
        return currentZIndex.value
    }

    return {currentZIndex, next}
}

const zIndexSymbol = Symbol()

function useZIndexProvide(base = __Z_BASE__, step = __Z_STEP__) {
    const zIndex = ref(base)
    const next = () => zIndex.value += step
    provide(zIndexSymbol, {zIndex, next})
}

function useZIndexInject() {
    return inject(zIndexSymbol)
}

export {
    useZIndex,
    useZIndexProvide,
    useZIndexInject,
}