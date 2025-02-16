// 封装resize指令 https://www.bilibili.com/video/BV1fWrqYjEhm?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
const map = new WeakMap();
const ro = new ResizeObserver((entries) => {
    for (let entry of entries) {
        let el = entry.target;
        let handler = map.get(el);
        if (!handler) {
            // width:height
            handler(entry.borderBoxSize[0].inlineSize,
                entry.borderBoxSize[0].blockSize);
        }
    }
});

const directive = {
    // https://cn.vuejs.org/guide/reusability/custom-directives.html#directive-hooks
    mounted: (el, bindings) => {
        map.set(el, bindings.value);
        ro.observe(el);
    },
    unmounted: (el) => {
        map.delete(el);
        ro.unobserve(el);
    },
};


export default {
    directive,
}
