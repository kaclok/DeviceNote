const directive = {
    mounted(el, binding, vnode) {
        const {value} = binding // 如 v-permission="['admin', 'editor']"
        if (value && value.length) {
            const userStore = useUserStore()
            const hasPermission = value.some(role => userStore.roles.includes(role))
            if (!hasPermission) {
                el.parentNode?.removeChild(el) // 或 el.style.display = 'none'
            }
        }
    },
};

export default {
    directive,
}
