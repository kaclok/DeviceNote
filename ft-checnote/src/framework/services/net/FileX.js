/**
 * 文件操作框架层封装
 * 交互流程:
 *   上传: initUpload → (秒传则跳过) → uploadToMinio → completeUpload
 *   下载: getDownloadUrl → 直连 MinIO 下载
 *
 * 下一个项目直接复制此文件即可使用，只需修改 baseURL
 */
import axios from 'axios'
import SparkMD5 from 'spark-md5'

// ---- 配置 ----
const BASE_URL = import.meta.env.VITE_BASE_API || ''

// ---- 内部工具 ----

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
 * 初始化上传
 * @param {{originalName: string, fileSize: number, fileMd5: string}} fileInfo
 * @returns {Promise<{fileId: string, uploadUrl: string, downloadUrl: string, isExist: boolean}>}
 */
export async function initUpload(fileInfo) {
    const res = await axios.post(BASE_URL + '/x/minio/init', {
        originalName: fileInfo.originalName,
        fileSize: fileInfo.fileSize,
        fileMd5: fileInfo.fileMd5,
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
 * @returns {Promise<axios.AxiosResponse<any>>}
 */
export function uploadToMinio(file, presignedUrl, onProgress) {
    return axios.put(presignedUrl, file, {
        headers: {'Content-Type': 'application/octet-stream'},
        onUploadProgress: e => {
            onProgress?.(e.loaded, e.total, e.total ? e.loaded / e.total : 0)
        },
    })
}

/**
 * 确认上传完成
 * @param {string} fileId
 * @returns {Promise<void>}
 */
export async function completeUpload(fileId) {
    await axios.post(BASE_URL + `/x/minio/complete/${fileId}`)
}

/**
 * 获取下载地址
 * @param {string} fileId
 * @returns {Promise<string>} presigned download URL
 */
export async function getDownloadUrl(fileId) {
    const res = await axios.get(BASE_URL + `/x/minio/download/${fileId}`)
    return res.data?.data || res.data
}

/**
 * 删除文件
 * @param {string} fileId
 * @returns {Promise<void>}
 */
export async function deleteFile(fileId) {
    await axios.delete(BASE_URL + `/x/minio/${fileId}`)
}

// ---- 一站式封装 ----

/**
 * 一站式上传文件
 * 自动: 计算 MD5 → initUpload → (秒传跳过) → uploadToMinio → completeUpload
 *
 * @param {File} file
 * @param {(loaded, total, percent) => void} [onProgress]
 * @returns {Promise<{fileId: string, isExist: boolean}>}
 */
export async function uploadFile(file, onProgress) {
    // 1. 计算 MD5
    const fileMd5 = await calcFileMd5(file)

    // 2. 初始化上传
    const result = await initUpload({
        originalName: file.name,
        fileSize: file.size,
        fileMd5,
    })

    // 3. 秒传命中，直接返回
    if (result.isExist) {
        onProgress?.(file.size, file.size, 1)
        return {fileId: result.fileId, isExist: true}
    }

    // 4. 直传 MinIO
    await uploadToMinio(file, result.uploadUrl, onProgress)

    // 5. 确认上传完成
    await completeUpload(result.fileId)

    return {fileId: result.fileId, isExist: false}
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
