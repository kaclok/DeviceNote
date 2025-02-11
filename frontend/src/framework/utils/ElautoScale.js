// 大屏自适应解决方案
// https://www.bilibili.com/video/BV1bT42117Fc?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f

function elAutoScaleByWindow(selector, options, listen) {
    const el = document.getElementById(selector);
    const {width, height} = options;

    el.style.transformOrigin = "top left";
    el.style.transition = "transform 0.2s"

    function refresh() {
        const scaleX = innerWidth / width;
        const scaleY = innerHeight / height;
        // 获取宽高缩放最小值
        const scale = Math.min(scaleX, scaleY);

        // 居中处理
        const left = (innerWidth - width * scale) / 2;
        const top = (innerHeight - height * scale) / 2;
        el.style.transform = `translate(${left}px, ${top}px) scale(${scale})`;
    }

    refresh()

    // 监听窗口缩放事件
    if (listen) {
        addEventListener('resize', refresh)
    } else {
        removeEventListener('resize', refresh)
    }

    // addEventListener是全局函数，为什么不需要import就可以直接使用，是因为window.xxx对象都是不需要import就可以直接使用的全局变量或者函数
}

export {
    elAutoScaleByWindow,
}