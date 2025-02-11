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
            // 清除定时器
            if (directive.timer !== null) {
                clearTimeout(directive.timer);
                directive.timer = null
            }

            // 立即执行，此类情况一般用不到
            if (immediate) {
                cb?.();
            } else {
                // 设置定时器，当最后一次操作后，timeout不会再被清除，所以在延时wait毫秒后执行func回调方法
                directive.timer = setTimeout(() => {
                    cb?.();
                    directive.timer = null;
                }, wait);
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
