<script setup>
import {ref, computed, onMounted} from 'vue'
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

// 预览数据源: 后端签发 MinIO 预签名直链，浏览器直接访问 MinIO(与上传直传同机制)
const previewUrl = ref('')       // MinIO 预签名 GET 直链
const previewLoading = ref(false)
const previewKey = computed(() => curFile.value?.id || '')

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
    previewLoading.value = true
    try {
        const res = await axiosInst.get(`x/fileview/url/${file.id}`)
        const url = res.data?.data || res.data
        if (!url) {
            throw new Error('后端未返回预览地址')
        }
        previewUrl.value = url
    } catch (e) {
        previewing.value = false
        ElMessage.error('获取预览地址失败: ' + (e?.data?.message || e?.message || e))
    } finally {
        previewLoading.value = false
    }
}

// 在浏览器新窗口直接打开 MinIO 原文件(验证直链是否可用)
function openRaw() {
    if (previewUrl.value) {
        window.open(previewUrl.value, '_blank')
    }
}

// 预览源加载失败(如文件已被删/后端异常)时回调: 避免 viewer 静默卡在加载态
function onPreviewError(err) {
    console.error('预览加载失败:', err)
    const msg = err?.message || (typeof err === 'string' ? err : '')
    ElMessage.error('预览失败: ' + (msg || '文件可能已被删除或后端不可达'))
}

// 格式不支持时的兜底提示
function onPreviewUnsupported() {
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
                        <el-button size="small" type="primary" plain @click="openRaw">新窗口打开原文件</el-button>
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
