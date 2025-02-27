// https://www.bytezonex.com/archives/1pWVGfLf.html
// vue3提供了 v-debounce和v-throttle用于节流和防抖
// https://mp.weixin.qq.com/s/Ulo4HaPOq6cBD3om9Nkopg

const directive = {
    timer: null,

    mounted: (el, bindings) => {
        const cfg = bindings.value;
        const cb = cfg.cd
        const wait = cfg.wait ?? 0.1;
        const immediate = cfg.immediate ?? false

        el.addEventListener('click', (e) => {
            if (immediate) {
                cb?.();
            } else {
                if (!directive.timer) {
                    // 如果是非立即执行，则在wait毫秒内的结束处执行
                    directive.timer = setTimeout(() => {
                        cb?.();
                        directive.timer = null
                    }, wait);
                }
            }
        })
    },
    unmounted: (el) => {
        clearTimeout(directive.timer);
    },
};

export default {
    directive,
}
