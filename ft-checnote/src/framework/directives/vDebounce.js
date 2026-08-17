// https://www.bytezonex.com/archives/1pWVGfLf.html
// vue3提供了 v-debounce和v-throttle用于节流和防抖
// https://mp.weixin.qq.com/s/Ulo4HaPOq6cBD3om9Nkopg
// https://cn.vuejs.org/guide/typescript/composition-api#typing-global-custom-directives

// 防抖指令 - 防止短时间内重复触发
const directive = {
    // 每个元素独立存储 timer，避免多个元素共用冲突
    mounted(el, binding) {

    },

    unmounted(el) {

    },
};

export default {
    directive,
};
