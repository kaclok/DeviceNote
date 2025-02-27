// https://blog.csdn.net/weixin_46472771/article/details/122603738

/**
 * 防抖函数
 * 防抖原理：一定时间内，只有最后一次操作，再过wait毫秒后才执行函数
 *
 * @param {Function} cb 要执行的回调函数
 * @param {Number} wait 延时的时间
 * @param {Boolean} immediate 是否立即执行
 * @return null
 */
let timer = null;
function debounce(cb, wait = 0.1, immediate = false) {
    // 清除定时器
    if (timer !== null) {
        clearTimeout(timer);
        timer = null
    }

    // 立即执行，此类情况一般用不到
    if (immediate) {
        cb?.();
    } else {
        // 设置定时器，当最后一次操作后，timeout不会再被清除，所以在延时wait毫秒后执行func回调方法
        timer = setTimeout(function () {
            cb?.();
            timer = null;
        }, wait);
    }
}

export {
    debounce,
}
