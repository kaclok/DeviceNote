// https://www.bytezonex.com/archives/1pWVGfLf.html
// vue3提供了 v-debounce和v-throttle用于节流和防抖

const directive = {
    timer: null,
    mounted: (el, bindings) => {
        const delay = bindings.value * 1000
        el.addEventListener('click', (e) => {
            // e是点击事件参数，内容为鼠标点击位置之类的信息
            el.disabled = true;
            // 这里不能是this.timer
            directive.timer = setTimeout(() => {
                el.disabled = false;
                directive.timer = null;
            }, delay);
        })
    },
    unmounted: (el) => {
        clearTimeout(directive.timer);
    },
};


export default {
    directive,
}
