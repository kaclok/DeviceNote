<script setup lang="js">
import { ref, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadFile, downloadFile, deleteFile, getDownloadUrl } from '@/framework/services/net/FileX.js'
import { UploadFilled } from '@element-plus/icons-vue'

// ---- 上传相关 ----
const fileList = ref([])
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadResult = ref(null)

// ---- 已上传文件列表 ----
const uploadedFiles = ref([])
const downloading = ref(false)

// ---- 上传 ----
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

    // el-upload 的 on-change 回调传的是 UploadFile 包装对象，原生 File 在 .raw 里
    const rawFile = fileList.value[0].raw || fileList.value[0]
    try {
        const result = await uploadFile(rawFile, (loaded, total, percent) => {
            uploadProgress.value = Math.round(percent * 100)
        })
        uploadResult.value = result
        if (result.is_exist) {
            ElMessage.success('秒传成功！文件已存在')
        } else {
            ElMessage.success('上传成功')
        }
        uploadedFiles.value.push({
            file_id: result.file_id,
            name: rawFile.name,
            size: rawFile.size,
            is_exist: result.is_exist,
        })
        fileList.value = []
        uploadProgress.value = 0
    } catch (e) {
        ElMessage.error('上传失败: ' + (e?.message || e))
    } finally {
        uploading.value = false
    }
}

// ---- 下载 ----
async function doDownload(file) {
    downloading.value = true
    try {
        await downloadFile(file.file_id, file.name)
        ElMessage.success('下载已触发')
    } catch (e) {
        ElMessage.error('下载失败: ' + (e?.message || e))
    } finally {
        downloading.value = false
    }
}

// ---- 获取下载地址 ----
async function doGetUrl(file) {
    try {
        const url = await getDownloadUrl(file.file_id)
        ElMessageBox.alert(url, '下载地址', { confirmButtonText: '关闭' })
    } catch (e) {
        ElMessage.error('获取地址失败: ' + (e?.message || e))
    }
}

// ---- 彻底删除(MinIO 对象 + DB 记录一并移除, 不可恢复) ----
async function doDelete(file, index) {
    try {
        await ElMessageBox.confirm(
            `确定彻底删除文件「${file.name}」吗？\n删除后将同时移除 MinIO 存储对象与数据库记录，不可恢复。`,
            '彻底删除确认',
            { confirmButtonText: '彻底删除', cancelButtonText: '取消', type: 'warning' }
        )
    } catch {
        return
    }
    try {
        await deleteFile(file.file_id)
        uploadedFiles.value.splice(index, 1)
        ElMessage.success('删除成功')
    } catch (e) {
        ElMessage.error('删除失败: ' + (e?.message || e))
    }
}

// ---- 格式化 ----
function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B'
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
    return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

onUnmounted(() => {
    uploading.value = false
})
</script>

<template>
<div class="file-test-page">
    <div class="page-header">
        <span>MinIO 文件服务测试</span>
    </div>

    <div class="page-body">
        <!-- 上传区 -->
        <el-card class="upload-card" shadow="never">
            <template #header>
                <span style="font-weight: bold">文件上传</span>
            </template>

            <el-upload
                :auto-upload="false"
                :on-change="handleUpload"
                :file-list="fileList"
                :limit="1"
                drag>
                <el-icon style="font-size: 48px; color: #c0c4cc; margin-bottom: 8px"><UploadFilled /></el-icon>
                <div>拖拽文件到此处 或 <em>点击选择</em></div>
                <template #tip>
                    <div style="font-size: 12px; color: #909399">
                        选择文件后点击「开始上传」按钮，支持秒传（相同 MD5 自动跳过）
                    </div>
                </template>
            </el-upload>

            <div style="margin-top: 16px; display: flex; gap: 12px; align-items: center">
                <el-button type="primary" :loading="uploading" @click="doUpload">
                    {{ uploading ? '上传中...' : '开始上传' }}
                </el-button>
                <el-progress
                    v-if="uploading || uploadProgress > 0"
                    :percentage="uploadProgress"
                    style="flex: 1"
                    :stroke-width="8"
                />
            </div>

            <el-alert
                v-if="uploadResult"
                style="margin-top: 12px"
                :title="uploadResult.is_exist ? '秒传命中' : '上传完成'"
                :description="`file_id: ${uploadResult.file_id}`"
                :type="uploadResult.is_exist ? 'warning' : 'success'"
                :closable="false"
            />
        </el-card>

        <!-- 文件列表 -->
        <el-card class="file-list-card" shadow="never" style="margin-top: 16px">
            <template #header>
                <span style="font-weight: bold">已上传文件</span>
            </template>

            <el-table :data="uploadedFiles" border stripe row-key="file_id" empty-text="暂无文件">
                <el-table-column label="文件名" prop="name" min-width="200" show-overflow-tooltip />
                <el-table-column label="大小" width="120">
                    <template #default="{ row }">{{ formatSize(row.size) }}</template>
                </el-table-column>
                <el-table-column label="秒传" width="80" align="center">
                    <template #default="{ row }">
                        <el-tag :type="row.is_exist ? 'warning' : 'info'" size="small">
                            {{ row.is_exist ? '是' : '否' }}
                        </el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="file_id" prop="file_id" min-width="200" show-overflow-tooltip />
                <el-table-column label="操作" width="240" align="center">
                    <template #default="{ row, $index }">
                        <el-button size="small" type="primary" @click="doDownload(row)" :loading="downloading">
                            下载
                        </el-button>
                        <el-button size="small" @click="doGetUrl(row)">
                            获取地址
                        </el-button>
                        <el-button size="small" type="danger" @click="doDelete(row, $index)">
                            删除
                        </el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</div>
</template>

<style scoped>
.file-test-page {
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
    max-width: 900px;
    margin: 0 auto;
    padding: 24px 16px;
    box-sizing: border-box;
}

.upload-card :deep(.el-upload-dragger) {
    padding: 20px;
}
</style>
