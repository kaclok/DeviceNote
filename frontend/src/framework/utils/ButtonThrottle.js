// https://blog.csdn.net/weixin_46472771/article/details/122603738
/**
 * 节流原理：在一定时间内，只能触发一次
 *
 * @param {Function} func 要执行的回调函数
 * @param {Number} wait 延时的时间
 * @param {Boolean} immediate 是否立即执行
 * @return null
 */
let timer = null

function throttle(cb, wait = 0.1, immediate = false) {
    if (immediate) {
        cb?.();
    } else {
        if (!timer) {
            // 如果是非立即执行，则在wait毫秒内的结束处执行
            timer = setTimeout(() => {
                cb?.();
                timer = null
            }, wait);
        }
    }
};

export {
    throttle,
}
