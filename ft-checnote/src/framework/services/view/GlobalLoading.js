// https://www.bilibili.com/video/BV13byLYLEVq?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
// 全局loading
import {computed, ref} from "vue"

const _loadingCnt = ref(0);

const globalLoading = computed({
    get() {
        return _loadingCnt.value > 0;
    },
    set(value) {
        _loadingCnt.value += (value ? 1 : -1);
        _loadingCnt.value = Math.max(0, _loadingCnt.value);
    }
})

export {
    globalLoading,
}

