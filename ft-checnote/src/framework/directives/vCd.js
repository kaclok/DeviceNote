// https://cn.vuejs.org/guide/reusability/custom-directives.html

/*
<template>
    <button v-cd="2" @click="handleClick">点击我</button>
</template>
*/

const directive = {
    mounted: (el, binding) => {
        const delay = (typeof binding.value === 'number' ? binding.value : 0.1) * 1000;

        // 每个元素独立存储 timer，避免多个元素共用冲突
        // 初始化
        el._timer = null;
        el._clickHandler = clickHandler;

        const clickHandler = (e) => {
            // 如果仍在cd之内
            if (el._timer) {
                e.preventDefault(); // 阻止浏览器对事件执行的默认操作。
                e.stopPropagation(); // 阻止事件从当前元素向上传播到父元素。
                return;
            }

            el.disabled = true;

            // 将定时器存储到 el._timer
            el._timer = setTimeout(() => {
                el.disabled = false;
                el._timer = null;  // 清除引用
            }, delay);
        };

        el.addEventListener('click', clickHandler);
    },

    unmounted: (el) => {
        // 清除定时器
        if (el._timer) {
            clearTimeout(el._timer);
            el._timer = null;
            delete el._timer;
        }

        // 移除事件监听
        if (el._clickHandler) {
            el.removeEventListener('click', el._clickHandler);
            delete el._clickHandler;
        }

        el.disabled = false;
    },

    // 当指令的参数更新时
    updated: (el, binding) => {
        // 如果绑定的值发生变化，可以重新设置延迟时间
        // 但这里简单处理：只更新时重置状态
        if (binding.oldValue !== binding.value) {
            // 清除现有的定时器
            if (el._timer) {
                clearTimeout(el._timer);
                el._timer = null;
                el.disabled = false;
            }
        }
    },
};


export default {
    directive,
}
