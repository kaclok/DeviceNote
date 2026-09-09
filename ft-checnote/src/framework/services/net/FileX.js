/**
 * 文件操作框架层封装
 * 交互流程:
 *   上传: beginUpload → (秒传则跳过) → uploadToMinio → endUpload
 *   下载: getDownloadUrl → 直连 MinIO 下载
 *
 * 下一个项目直接复制此文件即可使用，只需修改 baseURL
 */
import axios from 'axios'
import {axiosInst} from '@/framework/services/net/AxiosInst.js'
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
