import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

async function innerGet(url, paras, signal) {
    return axiosR.get(url, {
        params: paras,
        signal: signal,
    })
}

async function innerPost(url, paras, signal, onUploading, onDownloading) {
    return axiosR.post(url, paras, {
        headers: {
            // https://www.axios-http.cn/docs/multipart#-%E8%87%AA%E5%8A%A8%E5%BA%8F%E5%88%97%E5%8C%96
            // 从 v0.27.0 版本开始，当请求头中的 Content-Type 是 multipart/form-data 时，Axios 支持自动地将普通对象序列化成一个 FormData 对象
            'Content-Type': 'multipart/form-data charset=utf-8',
        },
        signal: signal,
        onUploadProgress: onUploading,
        onDownloadProgress: onDownloading,
    })
}

async function doGet(url, paras, signal, onBefore, onAfter) {
    onBefore?.();
    innerGet(url, paras, signal).then(succ => {
        onAfter?.(true, succ.data);
    }).catch(fail => {
        onAfter?.(false, fail);
    });
}

async function doPost(url, paras, signal, onBefore, onAfter, onUploading, onDownloading) {
    onBefore?.();
    innerPost(url, paras, signal, onUploading, onDownloading).then(succ => {
        onAfter?.(true, succ.data);
    }).catch(fail => {
        onAfter?.(false, fail);
    });
}

export {
    innerGet,
    innerPost,

    doGet,
    doPost,
}