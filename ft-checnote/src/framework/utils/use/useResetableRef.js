// https://www.bilibili.com/video/BV1KSmhYoE6x/?spm_id_from=333.1007.tianma.3-4-10.click&vd_source=5c9f5bd891aee351c325bcf632b5550f
// 如何重置vue组件的状态

import {ref} from "vue"

function useResetableRefFunc(cb) {
    const state = ref(cb());

    function reset() {
        state.value = cb();
    }

    return {state, reset}
}

function useResetableRef(value) {
    const initValue = JSON.parse(JSON.stringify(value));
    const state = ref(value);

    function reset() {
        state.value = JSON.parse(JSON.stringify(initValue));
    }

    return {state, reset}
}

export {
    useResetableRef,
    useResetableRefFunc,
}