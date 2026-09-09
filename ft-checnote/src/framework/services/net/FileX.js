/**
 * 文件操作框架层封装
 * 交互流程:
 *   上传: beginUpload → (秒传则跳过) → uploadToMinio → endUpload
 *   分块上传: chunkUploadFile(内部: MD5 → chunk/begin → 并发直传各分块 → chunk/complete 服务端合并)
 *   分块下载: chunkDownloadFile(内部: /download 拿直链 → Range 并发分块直连 MinIO → 重组 Blob)
 *   下载: getDownloadUrl → 直连 MinIO 下载
 *
 * 下一个项目直接复制此文件即可使用，只需修改 baseURL
 */
import axios from 'axios'
import {axiosInst} from '@/framework/services/net/AxiosInst.js'
import SparkMD5 from 'spark-md5'

// ---- 配置 ----
const BASE_URL = import.meta.env.VITE_BASE_API || ''

const MiB = 1024 * 1024
const DEFAULT_CHUNK_SIZE = 8 * MiB // 默认分块大小(字节)，须 >= 5MB(MinIO合并限制)

// ---- 内部工具 ----

/**
 * 分块并发执行器: 限制同时运行任务数，任一失败即中止后续任务
 * @param {number} total 任务总数
 * @param {number} limit 并发数
 * @param {(index:number)=>Promise<void>} task
 */
function runPool(total, limit, task) {
    return new Promise((resolve, reject) => {
        let next = 0
        let aborted = false
        let firstError = null
        const worker = async () => {
            while (!aborted && next < total) {
                const i = next++
                try {
                    await task(i)
                } catch (e) {
                    aborted = true
                    firstError = e
                }
            }
        }
        const n = Math.max(1, Math.min(limit, total))
        Promise.all(Array.from({length: n}, worker)).then(() => {
            if (firstError) reject(firstError)
            else resolve()
        })
    })
}

/**
 * 增量计算文件整体 MD5(边切块边哈希，避免整文件二次读取)
 */
async function md5ByChunks(file, chunkCount, chunkSize) {
    const spark = new SparkMD5.ArrayBuffer()
    for (let i = 0; i < chunkCount; i++) {
        const start = i * chunkSize
        const end = Math.min(file.size, start + chunkSize)
        const buf = await file.slice(start, end).arrayBuffer()
        spark.append(buf)
    }
    return spark.end(false)
}

/**
 * 计算文件 MD5
 */
function calcFileMd5(file) {
    return new Promise((resolve, reject) => {
        const spark = new SparkMD5.ArrayBuffer()
        const reader = new FileReader()
        reader.onload = e => {
            spark.append(e.target.result)
            resolve(spark.end(false))
        }
        reader.onerror = reject
        reader.readAsArrayBuffer(file)
    })
}

// ---- API 接口 ----

/**
 * 开始上传
 * @param {{original_name: string, file_size: number, file_md5: string}} fileInfo
 * @returns {Promise<{file_id: string, upload_url: string, download_url: string, is_exist: boolean}>}
 */
export async function beginUpload(fileInfo) {
    const res = await axiosInst.post('x/minio/begin', {
        original_name: fileInfo.original_name,
        file_size: fileInfo.file_size,
        file_md5: fileInfo.file_md5,
    }, {
        headers: {'Content-Type': 'application/json'},
    })
    return res.data?.data || res.data
}

/**
 * 直传文件到 MinIO（通过预签名 URL）
 * @param {File|Blob} file
 * @param {string} presignedUrl
 * @param {(loaded, total, percent) => void} [onProgress]
 */
export function uploadToMinio(file, presignedUrl, onProgress) {
    // MinIO presigned URL 是外部地址，用裸 axios 避免拦截器干扰
    // axiosInst 的响应拦截器要求 {code,data,message} 格式，MinIO 返回的不是
    return axios.put(presignedUrl, file, {
        headers: {'Content-Type': 'application/octet-stream'},
        onUploadProgress: e => {
            onProgress?.(e.loaded, e.total, e.total ? e.loaded / e.total : 0)
        },
    })
}

/**
 * 结束上传
 * @param {string} fileId
 * @returns {Promise<void>}
 */
export async function endUpload(fileId) {
    await axiosInst.post('x/minio/end', fileId, {
        headers: {
            'Content-Type': 'text/plain'  // 或者 'application/json'
        }
    })
}

/**
 * 获取下载地址
 * @param {string} fileId
 * @returns {Promise<string>} presigned download URL
 */
export async function getDownloadUrl(fileId) {
    const res = await axiosInst.post('x/minio/download', fileId, {
        headers: {
            'Content-Type': 'text/plain'  // 或者 'application/json'
        }
    })
    return res.data?.data || res.data
}

// ---- 分块 API(与后端 FileService.beginChunkUpload/completeChunkUpload/abortChunkUpload 对应) ----

/**
 * 查询已上传完成文件列表(后端 fileview/list)
 * @returns {Promise<Array<{id:string, original_name:string, file_size:number}>>}
 */
export async function listFiles() {
    const res = await axiosInst.get('x/fileview/list')
    return res.data?.data || res.data || []
}

/**
 * 开始分块上传: 后端为每个分块签发预签名 PUT 直链
 * @param {{original_name: string, file_size: number, file_md5: string, chunk_size: number}} fileInfo
 * @returns {Promise<{file_id: string, chunk_size: number, chunk_count: number, is_exist: boolean, parts: Array<{index:number, url:string}>}>}
 */
export async function beginChunkUpload(fileInfo) {
    const res = await axiosInst.post('x/minio/chunk/begin', {
        original_name: fileInfo.original_name,
        file_size: fileInfo.file_size,
        file_md5: fileInfo.file_md5,
        chunk_size: fileInfo.chunk_size,
    }, {
        headers: {'Content-Type': 'application/json'},
    })
    return res.data?.data || res.data
}

/**
 * 结束分块上传: 后端触发 MinIO 服务端合并分块并登记完成
 * @param {string} fileId
 */
export async function completeChunkUpload(fileId) {
    await axiosInst.post('x/minio/chunk/complete', fileId, {
        headers: {'Content-Type': 'text/plain'},
    })
}

/**
 * 中止分块上传: 清理已上传临时分块与 DB 记录
 * @param {string} fileId
 */
export async function abortChunkUpload(fileId) {
    await axiosInst.post('x/minio/chunk/abort', fileId, {
        headers: {'Content-Type': 'text/plain'},
    })
}

/**
 * 直传单个分块到 MinIO(预签名 URL, 裸 axios 避免拦截器干扰)
 * @param {Blob} partBlob
 * @param {string} presignedUrl
 * @param {(loaded:number, total:number)=>void} [onProgress]
 */
export function putPartToMinio(partBlob, presignedUrl, onProgress) {
    return axios.put(presignedUrl, partBlob, {
        headers: {'Content-Type': 'application/octet-stream'},
        onUploadProgress: e => onProgress?.(e.loaded, e.total || partBlob.size),
    })
}

/**
 * Range 分块直连 MinIO 下载一块
 * 注: 预签名 GET 直链为 query 签名，Range 头无需参与签名，MinIO 正常返回 206
 */
function getRange(presignedUrl, start, end, onProgress) {
    return axios.get(presignedUrl, {
        responseType: 'arraybuffer',
        headers: {'Range': `bytes=${start}-${end}`},
        onDownloadProgress: e => onProgress?.(e.loaded, end - start + 1),
    })
}

/**
 * 一站式分块上传(支持断点续传)
 * 流程: 边切块算MD5 → chunk/begin(后端探测已存在分块并标记 exists) → 只并发直传缺失分块到 MinIO → chunk/complete
 * 断点续传: 中断后再次对同一文件调用本函数，后端按 MD5 复用原 fileId，
 *           已完整上传的分块标记 exists=true 被跳过，仅补传缺失分块。
 * 数据流: 分块字节始终由浏览器直传 MinIO，不经后端；合并由 MinIO 服务端完成
 *
 * @param {File} file
 * @param {{chunkSize?: number, concurrency?: number, retries?: number, onProgress?: (loaded:number, total:number)=>void, onPart?: (index:number, status:'uploading'|'done'|'fail')=>void}} [opts]
 * @returns {Promise<{file_id: string, is_exist: boolean, chunk_count: number, skipped?: number}>}
 */
export async function chunkUploadFile(file, {
    chunkSize = DEFAULT_CHUNK_SIZE,
    concurrency = 3,
    retries = 2,
    onProgress,
    onPart,
} = {}) {
    const size = file.size
    const chunkCount = Math.max(1, Math.ceil(size / chunkSize))
    let skipped = 0 // 断点续传跳过的已存在分块数(须在 try 外声明，return 在 try 外引用)

    // 1. 增量 MD5(秒传/断点续传判定)
    const fileMd5 = await md5ByChunks(file, chunkCount, chunkSize)

    // 2. 开始分块上传(秒传命中则直接返回)
    const beginRes = await beginChunkUpload({
        original_name: file.name,
        file_size: size,
        file_md5: fileMd5,
        chunk_size: chunkSize,
    })
    if (beginRes.is_exist) {
        onProgress?.(size, size)
        return {file_id: beginRes.file_id, is_exist: true, chunk_count: 0}
    }
    const parts = beginRes.parts || []
    if (parts.length !== chunkCount) {
        throw new Error('后端返回的分块数量不一致')
    }

    try {
        // 3. 并发直传缺失分块(exists=true 为已完整上传的分块 → 跳过直传，实现断点续传)
        const loaded = new Array(chunkCount).fill(0)
        const emitProgress = () => onProgress?.(loaded.reduce((a, b) => a + b, 0), size)

        await runPool(chunkCount, concurrency, async i => {
            const start = i * chunkSize
            const end = Math.min(size, start + chunkSize)
            const blob = file.slice(start, end)
            if (parts[i] && parts[i].exists) {
                // 已传分块: 直接计入进度(断点续传)
                loaded[i] = end - start
                skipped++
                onPart?.(i, 'done')
                emitProgress()
                return
            }
            onPart?.(i, 'uploading')
            let lastErr = null
            for (let attempt = 0; attempt <= retries; attempt++) {
                try {
                    await putPartToMinio(blob, parts[i].url, l => {
                        loaded[i] = l
                        emitProgress()
                    })
                    loaded[i] = end - start
                    emitProgress()
                    onPart?.(i, 'done')
                    return
                } catch (e) {
                    lastErr = e
                    loaded[i] = 0
                }
            }
            onPart?.(i, 'fail')
            throw lastErr
        })

        // 4. 结束: 后端触发 MinIO 服务端合并
        await completeChunkUpload(beginRes.file_id)
        if (skipped > 0) {
            console.log(`断点续传完成: 跳过已存在分块 ${skipped}/${chunkCount}`)
        }
    } catch (e) {
        // 失败时挂上 file_id，页面可据此中止清理残留临时分块
        if (e && typeof e === 'object') e.fileId = e.fileId || beginRes.file_id
        throw e
    }
    onProgress?.(size, size)
    return {file_id: beginRes.file_id, is_exist: false, chunk_count: chunkCount, skipped}
}

/**
 * 一站式分块下载(测试/校验用途)
 * 流程: /download 拿预签名 GET 直链 → Range 并发分块直连 MinIO → 按序重组 Blob
 * 注意: 重组 Blob 会占用约等于文件大小的内存，超大文件请谨慎使用
 *
 * @param {string} fileId
 * @param {number} fileSize
 * @param {{chunkSize?: number, concurrency?: number, retries?: number, onProgress?: (loaded:number, total:number)=>void, onPart?: (index:number, status:'downloading'|'done'|'fail')=>void}} [opts]
 * @returns {Promise<{blob: Blob, partCount: number}>}
 */
export async function chunkDownloadFile(fileId, fileSize, {
    chunkSize = DEFAULT_CHUNK_SIZE,
    concurrency = 3,
    retries = 2,
    onProgress,
    onPart,
} = {}) {
    if (!fileSize || fileSize <= 0) {
        throw new Error('文件大小未知，无法分块下载')
    }
    const presignedUrl = await getDownloadUrl(fileId)
    const chunkCount = Math.max(1, Math.ceil(fileSize / chunkSize))
    const loaded = new Array(chunkCount).fill(0)
    const blobs = new Array(chunkCount)
    const emitProgress = () => onProgress?.(loaded.reduce((a, b) => a + b, 0), fileSize)

    await runPool(chunkCount, concurrency, async i => {
        const start = i * chunkSize
        const end = Math.min(fileSize - 1, start + chunkSize - 1)
        onPart?.(i, 'downloading')
        let lastErr = null
        for (let attempt = 0; attempt <= retries; attempt++) {
            try {
                const res = await getRange(presignedUrl, start, end, l => {
                    loaded[i] = l
                    emitProgress()
                })
                const buf = res.data instanceof ArrayBuffer ? res.data : null
                if (!buf) {
                    throw new Error('响应不是二进制数据(可能是被拦截/跨域问题)')
                }
                blobs[i] = new Blob([buf], {type: 'application/octet-stream'})
                loaded[i] = end - start + 1
                emitProgress()
                onPart?.(i, 'done')
                return
            } catch (e) {
                lastErr = e
                loaded[i] = 0
                blobs[i] = null
            }
        }
        onPart?.(i, 'fail')
        throw lastErr
    })

    const blob = new Blob(blobs, {type: 'application/octet-stream'})
    return {blob, partCount: chunkCount}
}

/**
 * 彻底删除文件
 * 后端语义: 删除 MinIO 对象 + 物理删除 DB 记录(不可恢复, 非逻辑删除)
 * @param {string} fileId
 * @returns {Promise<void>}
 */
export async function deleteFile(fileId) {
    await axiosInst.post('x/minio/delete', fileId, {
        headers: {
            'Content-Type': 'text/plain'  // 或者 'application/json'
        }
    })
}

// ---- 一站式封装 ----

/**
 * 一站式上传文件
 * 自动: 计算 MD5 → beginUpload → (秒传跳过) → uploadToMinio → endUpload
 *
 * @param {File} file
 * @param {(loaded, total, percent) => void} [onProgress]
 * @returns {Promise<{file_id: string, is_exist: boolean}>}
 */
export async function uploadFile(file, onProgress) {
    // 1. 计算 MD5
    const fileMd5 = await calcFileMd5(file)

    // 2. 开始上传
    const result = await beginUpload({
        original_name: file.name,
        file_size: file.size,
        file_md5: fileMd5,
    })

    // 3. 秒传命中，直接返回
    if (result.is_exist) {
        onProgress?.(file.size, file.size, 1)
        return {file_id: result.file_id, is_exist: true}
    }

    // 4. 直传 MinIO
    await uploadToMinio(file, result.upload_url, onProgress)

    // 5. 结束上传
    await endUpload(result.file_id)

    return {file_id: result.file_id, is_exist: false}
}

/**
 * 一站式下载文件
 * 获取下载 URL → 触发浏览器下载
 *
 * @param {string} fileId
 * @param {string} [filename] 下载文件名（不传则用 fileId）
 */
export async function downloadFile(fileId, filename) {
    const url = await getDownloadUrl(fileId)
    const a = document.createElement('a')
    a.href = url
    a.download = filename || fileId
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
}
