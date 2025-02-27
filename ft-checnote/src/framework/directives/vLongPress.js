// https://juejin.cn/post/6968996649515515917

const directive = {
    beforeMount(el, binding) {
        const cb = binding.value;
        el.$duration = binding.arg || 1500; // 获取长按时长, 默认1.5秒执行长按事件

        let timer = null;

        // 重置计时器
        const end = () => {
            if (timer !== null) {
                clearTimeout(timer);
                timer = null;
            }
        }

        const begin = (e) => {
            // 排除点击与右键情况, event.button: 0-左键  2-右键
            if (e.type === 'click' && e.button !== 0) {
                return;
            }
            e.preventDefault();
            if (timer === null) {
                timer = setTimeout(() => {
                    cb?.();
                    timer = null;
                }, el.$duration)
            }
        }

        // pc端 接触begin
        el.addEventListener('mousedown', begin);
        // 移动端 接触begin
        el.addEventListener('touchstart', begin);

        // pc端 接触end
        el.addEventListener('click', end);
        el.addEventListener('mouseout', end);
        el.addEventListener('mouseup', end);

        // 移动端 接触end
        el.addEventListener('touchend', end)
        el.addEventListener('touchcancel', end)
    },

    updated(el, binding) {
        // 可以实时更新时长
        el.$duration = binding.arg;
    },
};

export default {
    directive,
}
