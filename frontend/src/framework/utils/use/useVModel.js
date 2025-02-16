// https://www.bilibili.com/video/BV11LftYkEm7?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
// 防止子组件通过v-model修改父组件传递的参数

import {computed} from "vue";

function useVModel(props, propName, emit) {
    return computed({
        get() {
            return new Proxy(props[propName], {
                get(target, key, receiver) {
                    return Reflect.get(target, key, receiver);
                },
                set(target, key, value) {
                    emit(`update.${propName}`, {...target, [key]: value});
                    return true
                }
            })
        },
        set(val) {
            emit(`update.${propName}`, val)
        }
    })
}

export {
    useVModel,
}