const directive = {
    mounted(el, binding, vnode) {
        let permTypes = vnode.context.$route.meta.permTypes;
        if (permTypes && !permTypes.includes(binding.value)) {
            el.remove();
        }
    },
};

export default {
    directive,
}
