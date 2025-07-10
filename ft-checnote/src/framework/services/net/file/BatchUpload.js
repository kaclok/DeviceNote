import {post, upload as baseUpload} from "@/framework/services/net/InnerRequest.js"
import SparkMD5 from "spark-md5"

// 实现大文件上传
// https://mp.weixin.qq.com/s/PvIbABjfd7VlnEglxpU2AQ
// https://www.npmjs.com/package/spark-md5
// https://www.bilibili.com/video/BV1MZ421q7dr/?spm_id_from=333.788.recommend_more_video.19&vd_source=5c9f5bd891aee351c325bcf632b5550f
// https://www.51cto.com/article/664707.html
// https://www.bilibili.com/video/BV1q8411R7Cb/?spm_id_from=333.337.search-card.all.click&vd_source=5c9f5bd891aee351c325bcf632b5550f
// https://www.bilibili.com/video/BV1oF411x786/?spm_id_from=333.788.videopod.episodes&vd_source=5c9f5bd891aee351c325bcf632b5550f&p=2
// https://www.bilibili.com/video/BV1q8411R7Cb/?spm_id_from=333.337.search-card.all.click&vd_source=5c9f5bd891aee351c325bcf632b5550f
// https://www.bilibili.com/video/BV1rV4y1e7Vt?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
class BatchUpload {
    constructor(file, onUploadProgress) {
        this.file = file
        this.onUploadProgress = onUploadProgress

        this.chunkCount = Math.ceil(this.file.size / __CHUNK_SIZE__)

        this.chunkBlobs = BatchUpload.calcChunks(this.file, this.chunkCount, __CHUNK_SIZE__);
        const {totalHash, chunkHashs} = BatchUpload.calcHash(this.chunkBlobs)
        this.fileHash = totalHash
        this.chunkHashs = chunkHashs

        this.curChunkIndex = 0
        this.curChunkLoaded = 0

        this.cancelAbort = new AbortController()

        this.calcLoaded = () => {
            if (this.chunkCount <= 1) {
                return this.curChunkLoaded
            }
            return this.curChunkIndex * __CHUNK_SIZE__ + this.curChunkLoaded
        }

        this.onProgressing = progressEvent => {
            this.curChunkLoaded = progressEvent.loaded

            const loaded = this.calcLoaded()
            const total = this.file.size
            this.onUploadProgress?.(loaded, total, loaded / total)
        }
    }

    // https://www.bilibili.com/video/BV1q8411R7Cb/?spm_id_from=333.337.search-card.all.click&vd_source=5c9f5bd891aee351c325bcf632b5550f
    // https://www.bilibili.com/video/BV1rV4y1e7Vt?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
    static calcChunks(file, chunkCount) {
        let chunkBlobs = []
        for (let i = 0; i < chunkCount; i++) {
            const start = i * __CHUNK_SIZE__
            const end = Math.min(file.size, start + __CHUNK_SIZE__)
            const blob = file.slice(start, end)
            chunkBlobs.push(blob)
        }
        return chunkBlobs
    }

    static calcHash(chunkBlobs) {
        const spark = new SparkMD5() // 增量hash算法，防止一次性文件加载到内存中
        const chunkHashs = []

        function _read(i) {
            if (i >= this.chunkBlobs.length) {
                return spark.end()
            }
            const blob = chunkBlobs[i]
            const reader = new FileReader()
            reader.onload = e => {
                const bytes = e.target.result; // 读取到的字节数
                spark.append(bytes)

                const hash = SparkMD5.hash(bytes)
                chunkHashs.push(hash)

                _read(i + 1)
            }
            reader.readAsArrayBuffer(blob)
        }

        const totalHash = _read(0)
        return {totalHash: totalHash, chunkHashs: chunkHashs};
    }

    async cancel() {
        this.cancelAbort.abort()
        await post('batchUploadCancel', this.file.name);
    }

    async merge() {
        await post('batchUploadMerge', this.file.name);
    }

    async upload() {
        if (!(this.file instanceof File)) {
            return
        }

        if (this.file.size <= __CHUNK_SIZE__) { // 文件大小小于切片大小，直接上传
            await this._upload()
        } else {
            for (let i = 0; i < this.chunkCount; i++) {
                await this._batchUpload(i)
            }
        }
    }

    async _batchUpload(i) {
        const res = await this._uploadChunk(i).then(
            async success => {
                if (i === this.chunkCount - 1) {
                    // 最后一片切片上传成功
                    await this.merge().catch(error => {
                        this.cancel()
                    })
                }
            }
        ).catch(err => {
            this.cancel()
        })
    }

    async _uploadChunk(chunkIndex) {
        this.curChunkIndex = chunkIndex;

        const blob = this.chunkBlobs[chunkIndex]
        const blobHash = this.chunkHashs[chunkIndex]
        const chunkFile = new File([blob], this.file.name + chunkIndex)
        const form = {
            chunkIndex: chunkIndex,
            chunkCount: this.chunkCount,
            chunkFile: chunkFile,
            chunkHash: blobHash,
            fileHash: this.fileHash,
        }
        return await baseUpload({
            url: 'http://localhost:8092/train/minioFile/testParams7',
            onUploadProgress: this.onProgressing,
            signal: this.cancelAbort.signal,
            data: form
        })
    }

    async _upload() {
        this.curChunkIndex = 0;

        const form = {
            file: this.file
        }
        await baseUpload({
            url: 'http://localhost:8092/train/minioFile/testParams6',
            onUploadProgress: this.onProgressing,
            signal: this.cancelAbort.signal,
            data: form
        });
    }
}

/*class FileUploader {
    constructor(file) {
        this.file = file;
        this.identifier = this.generateUUID();
        this.chunkSize = 1024 * 1024 * 2; // 2MB
        this.totalChunks = Math.ceil(this.file.size / this.chunkSize);
        this.uploadedChunks = new Set();
        this.status = 'WAITING';
    }

    generateUUID() {
        return "";
    }

    async upload() {
        this.status = 'UPLOADING';
        const uploadPromises = [];

        for (let chunkNumber = 0; chunkNumber < this.totalChunks; chunkNumber++) {
            if (!this.uploadedChunks.has(chunkNumber)) {
                uploadPromises.push(this._uploadChunk(chunkNumber));
            }
        }

        try {
            await Promise.all(uploadPromises);
            this.status = 'COMPLETED';
        } catch (error) {
            this.status = 'FAILED';
            this._handleUploadError(error);
        }
    }

    async _uploadChunk(chunkNumber) {
        const start = chunkNumber * this.chunkSize;
        const end = Math.min(start + this.chunkSize, this.file.size);
        const chunk = this.file.slice(start, end);

        const formData = new FormData();
        formData.append('file', chunk);
        formData.append('identifier', this.identifier);
        formData.append('chunkNumber', chunkNumber);
        formData.append('totalChunks', this.totalChunks);

        return axios.post('/upload', formData, {
            // 配置重试拦截器
            retry: 3,
            retryDelay: (retryCount) => {
                return Math.pow(2, retryCount) * 1000;
            }
        }).then(() => {
            this.uploadedChunks.add(chunkNumber);
        });
    }

    _handleUploadError(error) {
        // 上传错误处理
        if (error.response) {
            // 请求已发出，但服务器响应状态码不在 2xx 范围内
            console.error('上传错误:', error.response.data);
            this.resumeUpload();
        } else if (error.request) {
            // 请求已发出，但未收到响应
            console.error('网络错误', error.request);
            this.resumeUpload();
        }
    }

    // 恢复上传
    resumeUpload() {
        axios.post('/resume', {
            identifier: this.identifier,
            uploadedChunks: Array.from(this.uploadedChunks)
        }).then(r => {
            //
        });
    }
}*/
