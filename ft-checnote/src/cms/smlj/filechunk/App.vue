<script setup lang="js">
import {ref, computed, onMounted} from 'vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import {UploadFilled} from '@element-plus/icons-vue'
import {
    listFiles,
    chunkUploadFile,
    chunkDownloadFile,
    abortChunkUpload,
    deleteFile,
} from '@/framework/services/net/FileX.js'

const MiB = 1024 * 1024
const MIN_CHUNK_MIB = 5 // MinIO 服务端合并要求: 除末块外每块 >= 5MB

// ---- 参数 ----
const chunkMiB = ref(8)      // 分块大小(MB)
const concurrency = ref(3)   // 并发数
const chunkSizeBytes = computed(() => chunkMiB.value * MiB)

// ---- 上传 ----
const fileList = ref([])
const uploading = ref(false)

// ---- 下载 ----
const downloading = ref(false)

// ---- 传输过程 ----
const transferTitle = ref('')   // 当前传输标题(文件名)
const transferTotal = ref(0)    // 总字节
const transferLoaded = ref(0)   // 已完成字节
const partRows = ref([])        // 分块状态明细
const transferBusy = computed(() => uploading.value || downloading.value)

// ---- 文件列表(后端已上传完成) ----
const files = ref([])
const loadingList = ref(false)

// 清理未完成任务(按 file_id 中止分块上传)
const cleanupId = ref('')
const cleaning = ref(false)

/* ---------------- 工具 ---------------- */
function fmtBytes(n) {
    if (n < 1024) return n + ' B'
    if (n < MiB) return (n / 1024).toFixed(1) + ' KB'
    if (n < 1024 * MiB) return (n / MiB).toFixed(2) + ' MB'
    return (n / 1024 / MiB).toFixed(2) + ' GB'
}

function fmtTime(t) {
    if (!t) return ''
    const s = String(t).replace('T', ' ')
    return s.length >= 19 ? s.substring(0, 19) : s
}

const partStatusText = {
    wait: '等待', uploading: '上传中', downloading: '下载中', done: '完成', fail: '失败',
}
const partStatusType = {
    wait: 'info', uploading: 'warning', downloading: 'warning', done: 'success', fail: 'danger',
}

/** 预生成分块状态明细(展示用) */
function buildPartRows(count, chunkSize, totalSize) {
    return Array.from({length: count}, (_, i) => {
        const start = i * chunkSize
        const end = Math.min(totalSize - 1, start + chunkSize - 1)
        return {index: i, start, end, size: end - start + 1, status: 'wait'}
    })
}

function updateProgress(loaded, total) {
    transferLoaded.value = loaded
    transferTotal.value = total
}

/* ---------------- 列表 ---------------- */
async function loadList() {
    loadingList.value = true
    try {
        files.value = await listFiles()
    } catch (e) {
        ElMessage.error('加载文件列表失败: ' + (e?.data?.message || e?.message || e))
    } finally {
        loadingList.value = false
    }
}

/* ---------------- 分块上传 ---------------- */
function handleUpload(file) {
    fileList.value.push(file)
    return false
}

const pickedFile = computed(() => {
    const f = fileList.value[0]
    return f ? (f.raw || f) : null
})

const uploadChunkCount = computed(() => {
    const f = pickedFile.value
    return f ? Math.max(1, Math.ceil(f.size / chunkSizeBytes.value)) : 0
})

async function doChunkUpload() {
    const file = pickedFile.value
    if (!file) {
        ElMessage.warning('请先选择文件')
        return
    }
    if (uploadChunkCount.value > 1 && chunkMiB.value < MIN_CHUNK_MIB) {
        ElMessage.warning(`需要分块时单块大小不能小于 ${MIN_CHUNK_MIB}MB(MinIO合并限制)`)
        return
    }

    uploading.value = true
    transferTitle.value = '分块上传: ' + file.name
    transferTotal.value = file.size
    transferLoaded.value = 0
    partRows.value = buildPartRows(uploadChunkCount.value, chunkSizeBytes.value, file.size)

    try {
        const result = await chunkUploadFile(file, {
            chunkSize: chunkSizeBytes.value,
            concurrency: concurrency.value,
            onProgress: updateProgress,
            onPart: (i, status) => {
                if (partRows.value[i]) partRows.value[i].status = status
            },
        })
        if (result.is_exist) {
            ElMessage.success('秒传成功！文件已存在')
        } else if (result.skipped > 0) {
            ElMessage.success(`断点续传完成: 跳过已上传分块 ${result.skipped}/${result.chunk_count}，仅补传缺失分块`)
        } else {
            ElMessage.success(`分块上传成功，共 ${result.chunk_count} 块，已由 MinIO 服务端合并`)
        }
        fileList.value = []
        partRows.value = []
        await loadList()
    } catch (e) {
        const msg = e?.data?.message || e?.message || e
        ElMessage.error('分块上传失败: ' + msg)
        // 失败即把 file_id 填入清理框，便于中止并清理残留临时分块
        if (e?.fileId) {
            cleanupId.value = e.fileId
            ElMessage.warning('可再次「开始上传」续传，或点击「中止并清理」清除已上传的临时分块')
        }
    } finally {
        uploading.value = false
        transferTitle.value = ''
    }
}

/* ---------------- 分块下载 ---------------- */
async function doChunkDownload(file) {
    if (!file || !file.file_size) {
        ElMessage.warning('该文件无有效大小，无法分块下载')
        return
    }
    downloading.value = true
    transferTitle.value = '分块下载: ' + file.original_name
    transferTotal.value = file.file_size
    transferLoaded.value = 0
    const count = Math.max(1, Math.ceil(file.file_size / chunkSizeBytes.value))
    partRows.value = buildPartRows(count, chunkSizeBytes.value, file.file_size)

    try {
        const {blob, partCount} = await chunkDownloadFile(file.id, file.file_size, {
            chunkSize: chunkSizeBytes.value,
            concurrency: concurrency.value,
            onProgress: updateProgress,
            onPart: (i, status) => {
                if (partRows.value[i]) partRows.value[i].status = status
            },
        })
        // 触发浏览器保存重组后的文件
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = file.original_name || file.id
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        setTimeout(() => URL.revokeObjectURL(url), 1000)
        ElMessage.success(`分块下载完成(${partCount} 块)，已触发保存，请自行对比文件是否完整`)
    } catch (e) {
        const msg = e?.data?.message || e?.message || e
        ElMessage.error('分块下载失败: ' + msg)
    } finally {
        downloading.value = false
        transferTitle.value = ''
        partRows.value = []
    }
}

/* ---------------- 删除(彻底删除) ---------------- */
async function doDelete(file) {
    try {
        await ElMessageBox.confirm(
            `确定彻底删除文件「${file.original_name}」吗？\n删除后将同时移除 MinIO 存储对象与数据库记录，不可恢复。`,
            '彻底删除确认',
            {confirmButtonText: '彻底删除', cancelButtonText: '取消', type: 'warning'}
        )
    } catch {
        return
    }
    try {
        await deleteFile(file.id)
        ElMessage.success('删除成功')
        await loadList()
    } catch (e) {
        ElMessage.error('删除失败: ' + (e?.data?.message || e?.message || e))
    }
}

/* ---------------- 中止/清理未完成的分块上传 ---------------- */
async function doAbort() {
    if (!cleanupId.value) {
        ElMessage.warning('请输入要清理的 file_id')
        return
    }
    try {
        await ElMessageBox.confirm(
            `将中止并清理 file_id=${cleanupId.value} 的未完成上传(删除临时分块与DB记录)，确认？`,
            '中止分块上传',
            {confirmButtonText: '中止清理', cancelButtonText: '取消', type: 'warning'}
        )
    } catch {
        return
    }
    cleaning.value = true
    try {
        await abortChunkUpload(cleanupId.value)
        ElMessage.success('已中止并清理')
        cleanupId.value = ''
    } catch (e) {
        ElMessage.error('清理失败: ' + (e?.data?.message || e?.message || e))
    } finally {
        cleaning.value = false
    }
}

onMounted(loadList)
</script>

<template>
<div class="chunk-page">
    <div class="page-header">
        <span>MinIO 分块上传 / 分块下载测试</span>
    </div>

    <div class="page-body">
        <!-- 参数区 -->
        <el-card class="block-card" shadow="never">
            <template #header>
                <span style="font-weight: bold">传输参数</span>
            </template>
            <div class="param-row">
                <div class="param-item">
                    <span class="param-label">分块大小</span>
                    <el-select v-model="chunkMiB" style="width: 130px">
                        <el-option v-for="v in [8, 16, 32, 64]" :key="v" :label="v + ' MB'" :value="v"/>
                    </el-select>
                </div>
                <div class="param-item">
                    <span class="param-label">并发数</span>
                    <el-select v-model="concurrency" style="width: 130px">
                        <el-option v-for="v in [1, 2, 3, 5]" :key="v" :label="v + ' 个'" :value="v"/>
                    </el-select>
                </div>
                <div class="param-tip">
                    文件 ≤ 分块大小时自动单块直传；需分块时单块须 ≥ 5MB(MinIO 服务端合并限制)。
                    <br>分块字节始终由浏览器直连 MinIO，不经后端；合并由 MinIO 服务端完成。
                </div>
            </div>
        </el-card>

        <!-- 上传区 -->
        <el-card class="block-card" shadow="never" style="margin-top: 16px">
            <template #header>
                <span style="font-weight: bold">分块上传</span>
            </template>

            <el-upload
                :auto-upload="false"
                :on-change="handleUpload"
                :file-list="fileList"
                :limit="1"
                drag>
                <el-icon style="font-size: 48px; color: #c0c4cc; margin-bottom: 8px"><UploadFilled/></el-icon>
                <div>拖拽文件到此处 或 <em>点击选择</em></div>
                <template #tip>
                    <div style="font-size: 12px; color: #909399">
                        选择大文件测试分块上传(小文件自动单块直传)；支持按 MD5 秒传 / 断点续传
                        <template v-if="pickedFile">
                            ，{{ pickedFile.name }} 将分为 {{ uploadChunkCount }} 块
                        </template>
                    </div>
                </template>
            </el-upload>

            <div style="margin-top: 16px">
                <el-button type="primary" :loading="uploading" :disabled="downloading" @click="doChunkUpload">
                    {{ uploading ? '上传中...' : '开始分块上传' }}
                </el-button>
            </div>

            <!-- 中止清理未完成任务 -->
            <div style="margin-top: 16px; display: flex; gap: 8px; align-items: center">
                <span style="font-size: 13px; color: #909399">清理未完成上传:</span>
                <el-input
                    v-model="cleanupId"
                    placeholder="输入 file_id(上传中断后遗留的临时分块)"
                    clearable
                    style="width: 340px"/>
                <el-button type="warning" plain :loading="cleaning" @click="doAbort">中止并清理</el-button>
            </div>
        </el-card>

        <!-- 传输过程 -->
        <el-card v-if="transferBusy" class="block-card" shadow="never" style="margin-top: 16px">
            <template #header>
                <span style="font-weight: bold">{{ transferTitle }}</span>
            </template>
            <el-progress :percentage="Math.round(transferTotal ? transferLoaded / transferTotal * 100 : 0)"
                         :stroke-width="14"/>
            <div style="font-size: 12px; color: #909399; margin-top: 6px">
                {{ fmtBytes(transferLoaded) }} / {{ fmtBytes(transferTotal) }}
                ({{ partRows.filter(r => r.status === 'done').length }}/{{ partRows.length }} 块)
            </div>
            <el-table :data="partRows" size="small" border max-height="260"
                      style="margin-top: 10px" empty-text="暂无分块">
                <el-table-column label="分块" width="90" align="center">
                    <template #default="{row}">#{{ row.index }}</template>
                </el-table-column>
                <el-table-column label="字节区间" min-width="220">
                    <template #default="{row}">{{ row.start }} ~ {{ row.end }}</template>
                </el-table-column>
                <el-table-column label="大小" width="120">
                    <template #default="{row}">{{ fmtBytes(row.size) }}</template>
                </el-table-column>
                <el-table-column label="状态" width="110" align="center">
                    <template #default="{row}">
                        <el-tag :type="partStatusType[row.status]" size="small">
                            {{ partStatusText[row.status] || row.status }}
                        </el-tag>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>

        <!-- 文件列表(已上传完成，可测分块下载) -->
        <el-card class="block-card" shadow="never" style="margin-top: 16px">
            <template #header>
                <div style="display: flex; align-items: center; justify-content: space-between">
                    <span style="font-weight: bold">已上传文件(分块下载测试)</span>
                    <el-button size="small" :loading="loadingList" @click="loadList">刷新</el-button>
                </div>
            </template>

            <el-table :data="files" border stripe row-key="id" v-loading="loadingList" empty-text="暂无文件">
                <el-table-column label="文件名" prop="original_name" min-width="220" show-overflow-tooltip/>
                <el-table-column label="大小" width="110">
                    <template #default="{row}">{{ fmtBytes(row.file_size) }}</template>
                </el-table-column>
                <el-table-column label="上传时间" width="170">
                    <template #default="{row}">{{ fmtTime(row.create_time) }}</template>
                </el-table-column>
                <el-table-column label="file_id" prop="id" min-width="220" show-overflow-tooltip/>
                <el-table-column label="操作" width="200" align="center">
                    <template #default="{row}">
                        <el-button size="small" type="primary" :loading="downloading" @click="doChunkDownload(row)">
                            分块下载
                        </el-button>
                        <el-button size="small" type="danger" @click="doDelete(row)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>
        </el-card>
    </div>
</div>
</template>

<style scoped>
.chunk-page {
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
    max-width: 1000px;
    margin: 0 auto;
    padding: 24px 16px;
    box-sizing: border-box;
}

.block-card :deep(.el-upload-dragger) {
    padding: 20px;
}

.param-row {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 20px;
}

.param-item {
    display: flex;
    align-items: center;
    gap: 8px;
}

.param-label {
    font-size: 13px;
    color: #606266;
    white-space: nowrap;
}

.param-tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.6;
}
</style>
