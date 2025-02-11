// 在 vue 中如何优雅的处理 loading
// https://www.bilibili.com/video/BV1SJHgexEgk?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f

const _loading = ref(false);

async function onLoading(cb, ...args) {
    _loading.value = true;
    try {
        await cb?.(args);
    } finally {
        _loading.value = false;
    }
}

export {
    onLoading,
}

