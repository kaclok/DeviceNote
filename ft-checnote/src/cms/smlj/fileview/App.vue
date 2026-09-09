<script setup>
import {ref, computed, onMounted, onBeforeUnmount} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {axiosInst} from '@/framework/services/net/AxiosInst.js'
import {uploadFile, deleteFile} from '@/framework/services/net/FileX.js'

// ---- Open File Viewer ----
import {OpenFileViewer} from "@open-file-viewer/vue";
import {
    imagePlugin, textPlugin, pdfPlugin, officePlugin,
    videoPlugin, audioPlugin, archivePlugin, emailPlugin, fallbackPlugin,
} from "@open-file-viewer/core";
import "@open-file-viewer/core/style.css";
import pdfWorkerSrc from "pdfjs-dist/build/pdf.worker.mjs?url";

const plugins = [
    imagePlugin(), textPlugin(), pdfPlugin({workerSrc: pdfWorkerSrc}), officePlugin(),
    videoPlugin(), audioPlugin(), archivePlugin(), emailPlugin(), fallbackPlugin(),
]

// ---- 上传 ----
const fileList = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)

// ---- 文件列表(后端 fileview/list) ----
const files = ref([])
const loading = ref(false)
const curFile = ref(null)   // 当前选中预览的文件
const previewing = ref(false)

// 预览数据源(两级):
// 1) 首选: 后端签发 MinIO 预签名直链 → 浏览器直接访问 MinIO
// 2) 降级: 直链 fetch 失败(浏览器到 MinIO 的 CORS/网络受限) → 自动切后端同源 /raw → Blob → viewer
const previewUrl = ref('')        // 当前喂给 viewer 的源(MinIO 直链 或 blob: URL)
const previewLoading = ref(false)
const fallbackTried = ref(false)  // 当前文件是否已尝试过同源降级
const blobObjectUrl = ref('')     // 降级产生的 Blob 对象URL(便于回收)
const isDirect = computed(() => !!previewUrl.value && /^https?:/i.test(previewUrl.value))

// 会话级标记: 首次直连探测被浏览器拦截(ERR_BLOCKED_BY_CLIENT / CORS)后置 true,
// 之后预览直接走同源降级, 不再重复发探测请求, 避免每次预览都产生被拦截的红字请求
let directDisabled = false
// 源从直链切到 blob 时 key 变化, 强制 viewer 重挂载
const previewKey = computed(() => `${curFile.value?.id || ''}-${fallbackTried.value ? 'blob' : 'direct'}`)

/* ---------------- 列表 ---------------- */
async function loadList() {
    loading.value = true
    try {
        const res = await axiosInst.get('x/fileview/list')
        const list = res.data?.data || res.data || []
        files.value = list
    } catch (e) {
        ElMessage.error('加载文件列表失败: ' + (e?.data?.message || e?.message || e))
    } finally {
        loading.value = false
    }
}

async function onPreview(file) {
    curFile.value = file
    previewing.value = true
    previewUrl.value = ''
    fallbackTried.value = false
    revokeBlobObjectUrl()
    previewLoading.value = true
    try {
        const res = await axiosInst.get(`x/fileview/url/${file.id}`)
        const url = res.data?.data || res.data
        if (!url) {
            throw new Error('后端未返回预览地址')
        }
        // 主动探测浏览器→MinIO 跨域是否可用(16 字节 Range,代价极小)
        // 首次失败会置 directDisabled: 该环境浏览器层拦截直连(ERR_BLOCKED_BY_CLIENT/CORS/无ACAO),
        // 后续文件直接同源降级, 不再重复探测
        let directOk = false
        if (!directDisabled) {
            directOk = await probeDirectOk(url)
            if (!directOk) directDisabled = true
        }
        if (directOk) {
            previewUrl.value = url   // 首选: 直链
        } else {
            // 直连受限(无 ACAO/网络不通/被浏览器拦截) → 直接降级同源 Blob,不依赖 viewer 报错
            await fallbackToSameOrigin()
        }
    } catch (e) {
        previewing.value = false
        ElMessage.error('获取预览地址失败: ' + (e?.data?.message || e?.message || e))
    } finally {
        previewLoading.value = false
    }
}

// 探测直链跨域可用性: 服务器是否回带 CORS 头(fetch 失败即代表被 CORS/网络拦截)
async function probeDirectOk(url) {
    try {
        const ctl = new AbortController()
        const timer = setTimeout(() => ctl.abort(), 8000)
        const r = await fetch(url, {mode: 'cors', headers: {Range: 'bytes=0-15'}, signal: ctl.signal})
        clearTimeout(timer)
        return r.ok
    } catch {
        return false
    }
}

function revokeBlobObjectUrl() {
    if (blobObjectUrl.value) {
        URL.revokeObjectURL(blobObjectUrl.value)
        blobObjectUrl.value = ''
    }
}

// 直链被浏览器 CORS/网络拦截时的降级(官方 FAQ 建议的 Blob 方案):
// 改请求后端同源 /raw → 拉取字节 → Blob URL → 交给 viewer
async function fallbackToSameOrigin() {
    if (fallbackTried.value || !curFile.value) return
    fallbackTried.value = true
    try {
        // 注意: AxiosInst 响应拦截器对 Blob 响应直接返回 Blob 本体(return success.data),
        // 因此 res 本身就是 Blob, 不能再用 res.data
        const res = await axiosInst.get(`x/fileview/raw/${curFile.value.id}`, {responseType: 'blob'})
        const blob = res instanceof Blob ? res : (res?.data instanceof Blob ? res.data : null)
        // 后端返回异常(Result JSON 而非文件字节)时兜底提示
        if (!blob || !blob.type || blob.type.includes('json') || blob.type.includes('text/plain')) {
            let msg = '后端返回异常'
            try {
                msg = await blob.text()
            } catch {}
            fallbackTried.value = false
            previewing.value = false
            ElMessage.error('同源预览失败: ' + msg.slice(0, 120))
            return
        }
        revokeBlobObjectUrl()
        blobObjectUrl.value = URL.createObjectURL(blob)
        previewUrl.value = blobObjectUrl.value
        ElMessage.info('当前网络直连 MinIO 受限，已切换为后端同源预览')
    } catch (e) {
        fallbackTried.value = false
        ElMessage.error('同源预览失败: ' + (e?.message || e))
    }
}

// 在浏览器新窗口直接打开 MinIO 原文件(验证直链是否可用)
function openRaw() {
    if (previewUrl.value) {
        window.open(previewUrl.value, '_blank')
    }
}

// 预览源加载失败: 若当前是直链且未降级过 → 自动切同源 Blob; 已降级仍失败才提示
function onPreviewError(err) {
    console.error('预览加载失败:', err)
    if (isDirect.value && !fallbackTried.value) {
        fallbackToSameOrigin()
        return
    }
    const msg = err?.message || (typeof err === 'string' ? err : '')
    ElMessage.error('预览失败: ' + (msg || '文件可能已被删除或后端不可达'))
}

// 直链格式不支持时也先尝试同源降级一次(CORS 拦截可能影响其类型探测)
function onPreviewUnsupported() {
    if (isDirect.value && !fallbackTried.value) {
        fallbackToSameOrigin()
        return
    }
    ElMessage.warning('该文件格式暂不支持在线预览，可尝试下载后查看')
}

/* ---------------- 上传 ---------------- */
function handleUpload(file) {
    fileList.value.push(file)
    return false
}

async function doUpload() {
    if (fileList.value.length === 0) {
        ElMessage.warning('请先选择文件')
        return
    }
    uploading.value = true
    uploadProgress.value = 0
    try {
        const rawFile = fileList.value[0].raw || fileList.value[0]
        const result = await uploadFile(rawFile, (loaded, total, percent) => {
            uploadProgress.value = Math.round(percent * 100)
        })
        ElMessage.success(result.is_exist ? '秒传成功' : '上传成功')
        fileList.value = []
        uploadProgress.value = 0
        await loadList()
        // 上传成功后默认预览新文件
        const newest = files.value.find(f => f.id === result.file_id)
        if (newest) onPreview(newest)
    } catch (e) {
        ElMessage.error('上传失败: ' + (e?.message || e))
    } finally {
        uploading.value = false
    }
}

/* ---------------- 删除(彻底删除) ---------------- */
async function onDelete(file) {
    try {
        await ElMessageBox.confirm(
            `确定彻底删除文件「${file.original_name}」吗？不可恢复。`, '彻底删除确认',
            {confirmButtonText: '彻底删除', cancelButtonText: '取消', type: 'warning'})
    } catch {
        return
    }
    try {
        await deleteFile(file.id)
        ElMessage.success('删除成功')
        if (curFile.value?.id === file.id) {
            curFile.value = null
            previewing.value = false
            previewUrl.value = ''
            revokeBlobObjectUrl()
        }
        await loadList()
    } catch (e) {
        ElMessage.error('删除失败: ' + (e?.message || e))
    }
}

/* ---------------- 工具 ---------------- */
function formatSize(bytes) {
    if (bytes == null) return '-'
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

function formatTime(ts) {
    if (!ts) return '-'
    const d = new Date(ts)
    const p = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(() => {
    loadList()
})

onBeforeUnmount(() => {
    revokeBlobObjectUrl()
})
</script>

<template>
    <div class="file-view-page">
        <div class="page-header">
            <span>文件预览测试 (Open File Viewer)</span>
        </div>

        <div class="page-body">
            <!-- 上传区 -->
            <el-card shadow="never">
                <template #header><span style="font-weight: bold">文件上传(上传成功后自动预览)</span></template>
                <el-upload
                    :auto-upload="false" :on-change="handleUpload" :file-list="fileList" :limit="1" drag>
                    <div style="font-size: 13px; color: #909399">拖拽文件到此处 或 <em>点击选择</em></div>
                    <template #tip>
                        <div style="font-size: 12px; color: #909399">支持 PDF / Office / 图片 / 视频 / 压缩包等，上传到 MinIO 后由 Open File Viewer 网页预览</div>
                    </template>
                </el-upload>
                <div style="margin-top: 12px; display: flex; gap: 12px; align-items: center">
                    <el-button type="primary" :loading="uploading" @click="doUpload">
                        {{ uploading ? '上传中...' : '开始上传' }}
                    </el-button>
                    <el-progress v-if="uploading || uploadProgress > 0" :percentage="uploadProgress"
                                 style="flex: 1" :stroke-width="8"/>
                </div>
            </el-card>

            <!-- 文件列表 -->
            <el-card shadow="never" style="margin-top: 16px">
                <template #header><span style="font-weight: bold">文件列表(可预览文件)</span></template>
                <el-table :data="files" v-loading="loading" border stripe highlight-current-row
                          row-key="id" empty-text="暂无文件"
                          @row-click="onPreview"
                          :row-class-name="({row}) => curFile?.id === row.id ? 'cur-preview-row' : ''">
                    <el-table-column label="文件名" min-width="220" show-overflow-tooltip>
                        <template #default="{row}">{{ row.original_name }}</template>
                    </el-table-column>
                    <el-table-column label="大小" width="110">
                        <template #default="{row}">{{ formatSize(row.file_size) }}</template>
                    </el-table-column>
                    <el-table-column label="上传时间" width="160">
                        <template #default="{row}">{{ formatTime(row.create_time) }}</template>
                    </el-table-column>
                    <el-table-column label="操作" width="150" align="center">
                        <template #default="{row}">
                            <el-button size="small" type="primary" @click.stop="onPreview(row)">预览</el-button>
                            <el-button size="small" type="danger" @click.stop="onDelete(row)">删除</el-button>
                        </template>
                    </el-table-column>
                </el-table>
            </el-card>

            <!-- 预览区: file 为 MinIO 预签名直链, 浏览器直接访问 MinIO -->
            <el-card shadow="never" style="margin-top: 16px" v-if="previewing && previewUrl" v-loading="previewLoading">
                <template #header>
                    <div style="display: flex; justify-content: space-between; align-items: center">
                        <span style="font-weight: bold">预览: {{ curFile.original_name }}</span>
                        <!-- 仅直链模式可用: 新窗口打开能直接验证 MinIO 直链本身是否可用 -->
                        <el-button v-if="isDirect" size="small" type="primary" plain @click="openRaw">新窗口打开原文件</el-button>
                    </div>
                </template>
                <OpenFileViewer
                    :key="previewKey"
                    :file="previewUrl"
                    :file-name="curFile.original_name"
                    :plugins="plugins"
                    :toolbar="true"
                    theme="auto"
                    height="72vh"
                    width="100%"
                    :on-error="onPreviewError"
                    :on-unsupported="onPreviewUnsupported"
                />
            </el-card>
        </div>
    </div>
</template>

<style scoped>
.file-view-page {
    width: 100%;
    min-height: 100vh;
    background-color: #f5f5f5;
    display: flex;
    flex-direction: column;
}

.page-header {
    width: 100%;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #1c4785;
    color: #fff;
    font-size: 18px;
    font-weight: bolder;
}

.page-body {
    flex: 1;
    width: 100%;
    max-width: 1100px;
    margin: 0 auto;
    padding: 24px 16px;
    box-sizing: border-box;
}

:deep(.cur-preview-row) {
    --el-table-tr-bg-color: #ecf5ff;
}

:deep(.el-upload-dragger) {
    padding: 16px;
}
</style>
