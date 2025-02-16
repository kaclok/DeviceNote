// https://www.bilibili.com/video/BV11LftYkEm7?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
// 防止子组件通过v-model修改父组件传递的参数

// https://www.bilibili.com/video/BV1CnftYuEvQ?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
// 子组件接收父组件的所有数据，事件，插槽
// 子组件的"$attrs" 在模版中接收 父组件传递的：数据，事件 ，在<script setup> 中，需要使用 useAttrs 函数来获取 $attrs 对象
// 子组件的"$slots" 在模版中接收 父组件传递的：插槽

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