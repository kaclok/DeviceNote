<template>
    <div class="dashboard-container">
        <!-- 顶部标题栏 -->
        <div class="dashboard-header">
            <div class="logo">
                <div class="logo-icon">🏭</div>
                <div class="logo-text">
                    <h1>化工厂趋势检测系统</h1>
                </div>
            </div>
            <div class="header-right">
                <input type="file" ref="fileList" accept=".xlsx, .xls" @change="onFileChange"
                    style="display: none;" />
                <div class="control-buttons">
                    <el-button size="small" @click="coverCfg" type="primary">
                        更新点位配置
                    </el-button>
                    <el-button size="small" @click="reloadService" type="primary">
                        重启后端服务
                    </el-button>
                </div>
                <div class="time-info">
                    <el-icon>
                        <Clock />
                    </el-icon>
                    {{ currentTime }}
                </div>
            </div>
        </div>

        <!-- 主体内容：左侧（饼图+异常流） + 右侧6个子厂 -->
        <div class="main-content">
            <!-- 左侧区域：饼图 + 实时异常消息流 -->
            <div class="left-panel">
                <!-- 实时异常消息流卡片 - 自动延伸到浏览器底部 -->
                <div class="alert-card">
                    <div class="alert-header">
                        <div class="alert-title">
                            <el-icon>
                                <BellFilled />
                            </el-icon>
                            <span>实时异常消息流</span>
                            <span class="alert-count">{{ alertQueue.length }}</span>
                        </div>
                        <el-button size="small" @click="clearAlerts" :icon="Delete">清空</el-button>
                    </div>
                    <div class="alert-list" v-if="alertQueue.length > 0">
                        <div v-for="alert in alertQueue" :key="alert.id" class="alert-item">
                            <div class="alert-content">
                                <div class="alert-info" @click="handleClickAlert(alert)">
                                    <span class="alert-factory">{{ getFactoryName(alert.factoryId) }}</span>
                                    <span class="alert-point">{{ alert.pointName }}</span>
                                </div>
                                <div class="alert-message">当前值:{{ alert.av }} {{ alert.dw }}</div>
                                <div class="alert-time">{{ formatTime(alert.timestamp) }}</div>
                            </div>
                        </div>
                    </div>
                    <div v-else class="alert-empty">
                        <el-empty description="暂无异常消息，系统运行正常" :image-size="60" />
                    </div>
                </div>
            </div>

            <!-- 右侧6个子厂区域（两列三行布局，每个分厂6个点位） -->
            <div class="factories-section">
                <div class="factories-grid">
                    <div v-for="factory in factories" :key="factory.id" class="factory-card-wrapper">
                        <div class="factory-card">
                            <div class="factory-header">
                                <div class="factory-name">
                                    <span class="factory-icon">🏭</span>
                                    <span>{{ factory.name }}</span>
                                </div>
                            </div>
                            <div class="factory-points">
                                <div v-for="point in getFactoryPoints(factory.id)" :key="point.id" class="point-row">
                                    <div class="point-name" @click="handleClickAlert(point)">
                                        {{ point.pointName }}
                                    </div>
                                    <div class="point-values">
                                        <span class="current-value">
                                            当前值:{{ point.av }} {{ point.dw }}
                                        </span>
                                        <span class="trend-icon" :class="getTrendClass(point)">
                                            {{ getTrendIcon(point) }}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
    Clock,
    BellFilled,
    Delete
} from '@element-plus/icons-vue'
import axios from "axios"
import { axiosInst as axiosR } from "@/framework/services/net/AxiosInst.js"

// ==================== 异常消息队列 ====================
const alertQueue = ref([])
let nextAlertId = 1

const addAlert = (factoryId, pointName, pointId, av, dw, failReason, detail) => {
    const newAlert = {
        id: nextAlertId++,
        factoryId,
        pointName,
        pointId,
        av,
        dw,
        failReason,
        timestamp: new Date(),
        detail: detail,
    }
    alertQueue.value.unshift(newAlert)
    if (alertQueue.value.length > 100) {
        alertQueue.value = alertQueue.value.slice(0, 100)
    }
}

const clearAlerts = () => {
    alertQueue.value = []
    ElMessage.success({
        message: '已清空所有异常消息',
        offset: 80,
        duration: 3000
    })
}

const wsUrl = "ws://10.8.54.24:8091/api/webSocket/029567"

const fileList = ref(null)

const _reqCover = (fs) => {
    const formData = new FormData()
    formData.append('file', fs)

    axiosR.post("apiWatchdog/x/watchdog/importExcel", formData).then(res => {
        window.alert("导入成功")
    }).catch(err => {
        window.alert("导入失败")
    })
}

function onFileChange() {
    const fs = fileList.value.files[0]
    _reqCover(fs)
    // 解决第二次选择同样的文件时不调用的问题
    fileList.value.value = null
}

const coverCfg = () => {
    fileList.value.click();
}

const reloadService = () => {
    axiosR.get("apiWatchdog/x/watchdog/reloadDB").then(res => {
        window.alert("刷新成功")
    }).catch(err => {
        window.alert("刷新失败")
    })
}

let ws = null
function connectWebSocket() {
    // 建立WebSocket连接
    // ws = new WebSocket("ws://10.8.54.244:8080/smds-pre-warning-server-1.0-SNAPSHOT/api/webSocket/hahah");
    ws = new WebSocket(wsUrl);
    // ws = new WebSocket("ws://10.10.22.158:8080/api/webSocket/hahah");
    // 当WebSocket连接成功时
    ws.onopen = () => {
        console.log("WebSocket连接已建立");
        // ws.send("Hello from the client!"); // 发送测试消息
    };

    // 当收到来自服务端的消息时
    ws.onmessage = (event) => {
        let r = JSON.parse(event.data)
        addAlert(r.point.branchId, r.point.indicatorName, r.point.id, r.av, r.point.dw, r.failReason, r)
        console.log(event.data)
    };

    // 当WebSocket连接关闭时
    ws.onclose = () => {
        console.log("WebSocket连接已关闭");
    };

    // 当WebSocket出现错误时
    ws.onerror = (error) => {
        console.error("WebSocket发生错误:", error);
    };
}

// ==================== 生命周期 ====================
onMounted(() => {
    connectWebSocket()
    updateClock()

    autoRefreshTimeInterval = setInterval(updateClock, 1000)
})

onUnmounted(() => {
    if (autoRefreshInterval) {
        clearInterval(autoRefreshInterval)
    }
    if (autoRefreshTimeInterval) {
        clearInterval(autoRefreshTimeInterval)
    }
})

// ==================== 数据配置 - 每个分厂6个点位 ====================
const factories = ref([
    { id: 1, name: "电石一分厂", },
    { id: 2, name: "电石二分厂", },
    { id: 3, name: "电石三分厂", },
    { id: 4, name: "白灰分厂", },
    { id: 5, name: "兰炭分厂", },
    { id: 6, name: "热电分厂", }
])

// ==================== 点位数据管理 ====================
const getFactoryPoints = (factoryId) => {
    let ls = Object.values(alertQueue.value).filter(p => p.factoryId === factoryId)
    return ls.slice(0, 6)
}

// ==================== 辅助函数 ====================
const getFactoryName = (id) => {
    return factories.value.find(f => f.id === id)?.name || "未知厂区"
}

const handleClickAlert = (alert) => {
    console.log(alert)
}

const getTrendClass = (trend) => {
    if (trend.failReason === 1) return "trend-up"
    if (trend.failReason === 2) return "trend-down"
    if (trend.failReason === 3) return "trend-down"
    return "trend-stable"
}

const getTrendIcon = (trend) => {
    if (trend.failReason === 1) return "spike"
    if (trend.failReason === 2) return "cusum"
    if (trend.failReason === 3) return "z-score"
    return "●"
}

const formatTime = (timestamp) => {
    const date = new Date(timestamp)
    return date.toLocaleTimeString('zh-CN', { hour12: false })
}

// ==================== 自动刷新控制 ====================
const currentTime = ref("")
let autoRefreshInterval = null
let autoRefreshTimeInterval = null

const updateClock = () => {
    const now = new Date()
    currentTime.value = now.toLocaleString('zh-CN')
}
</script>

<style scoped>
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

.dashboard-container {
    min-height: 100vh;
    background: #f5f7fa;
    padding: 20px;
    display: flex;
    flex-direction: column;
}

/* 顶部标题栏 */
.dashboard-header {
    background: linear-gradient(135deg, #0f2b3d 0%, #1a4a6f 100%);
    color: white;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 24px;
    border-radius: 12px;
    margin-bottom: 20px;
    flex-shrink: 0;
}

.logo {
    display: flex;
    align-items: center;
    gap: 12px;
}

.logo-icon {
    font-size: 1.4rem;
}

.logo-text h1 {
    font-size: 1.2rem;
    margin: 0;
}

.logo-text p {
    font-size: 0.65rem;
    opacity: 0.8;
    margin: 2px 0 0;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 16px;
}

.control-buttons {
    display: flex;
    gap: 8px;
}

.time-info {
    display: flex;
    align-items: center;
    gap: 6px;
    background: rgba(255, 255, 255, 0.15);
    padding: 6px 12px;
    border-radius: 20px;
    font-size: 0.85rem;
}

/* 主内容区域 - 占满剩余高度 */
.main-content {
    display: flex;
    gap: 20px;
    flex: 1;
    min-height: 0;
}

/* 左侧面板（饼图 + 异常流） */
.left-panel {
    width: 260px;
    flex-shrink: 0;
    display: flex;
    flex-direction: column;
    gap: 20px;
    height: calc(100vh);
    /* 减去顶部标题栏高度和padding */
}

.stat-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 0.7rem;
    color: #64748b;
}

.stat-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
}

.stat-dot.abnormal {
    background: #f56c6c;
}

.stat-dot.normal {
    background: #67c23a;
}

.pie-chart-container {
    width: 100%;
    height: 155px;
}

.total-info {
    text-align: center;
    margin-top: 8px;
    padding-top: 8px;
    border-top: 1px solid #e2e8f0;
    font-size: 0.8rem;
    color: #475569;
}

/* 实时异常消息流卡片 - 自动延伸到浏览器底部 */
.alert-card {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
    max-height: calc(100% - 180px);
    /* 减去饼图卡片的高度 */
}

.alert-header {
    padding: 10px 16px;
    background: #f8fafc;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e2e8f0;
    flex-shrink: 0;
}

.alert-title {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    font-size: 0.9rem;
    color: #1e293b;
}

.alert-count {
    background: #f56c6c;
    color: white;
    padding: 1px 6px;
    border-radius: 10px;
    font-size: 0.65rem;
    margin-left: 6px;
}

.alert-list {
    flex: 1;
    overflow-y: auto;
    padding: 4px 0;
    max-height: 100%;
}

.alert-item {
    padding: 10px 12px;
    border-bottom: 1px solid #f1f5f9;
    display: flex;
    gap: 10px;
    transition: background 0.2s;
    animation: slideIn 0.3s ease;
}

.alert-item:hover {
    background: #fafcff;
}

.alert-item.critical {
    background: #fef0f0;
    border-left: 3px solid #f56c6c;
}

.alert-item.warning {
    background: #fdf6ec;
    border-left: 3px solid #e6a23c;
}

@keyframes slideIn {
    from {
        opacity: 0;
        transform: translateX(-20px);
    }

    to {
        opacity: 1;
        transform: translateX(0);
    }
}

.alert-icon {
    flex-shrink: 0;
}

.alert-content {
    flex: 1;
}

.alert-info {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
    flex-wrap: wrap;
}

.alert-factory {
    font-weight: 700;
    font-size: 0.8rem;
    color: #1e293b;
}

.alert-point {
    font-size: 0.75rem;
    color: #475569;
}

.alert-level {
    font-size: 0.6rem;
    padding: 1px 6px;
    border-radius: 10px;
    font-weight: 500;
}

.alert-level.critical {
    background: #fef0f0;
    color: #f56c6c;
}

.alert-level.warning {
    background: #fdf6ec;
    color: #e6a23c;
}

.alert-message {
    font-size: 0.7rem;
    color: #475569;
    margin-bottom: 3px;
    line-height: 1.3;
}

.alert-time {
    font-size: 0.55rem;
    color: #94a3b8;
}

.alert-empty {
    padding: 20px;
    text-align: center;
}

/* 右侧6个子厂区域 - 每个分厂6个点位 */
.factories-section {
    flex: 1;
    min-width: 0;
    height: 100%;
    overflow-y: auto;
}

.factories-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
}

.factory-card-wrapper {
    display: flex;
}

.factory-card {
    background: white;
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    transition: all 0.3s;
    border-left: 4px solid;
    display: flex;
    flex-direction: column;
    width: 100%;
}

.factory-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.factory-card.status-critical {
    border-left-color: #f56c6c;
}

.factory-card.status-warning {
    border-left-color: #e6a23c;
}

.factory-card.status-normal {
    border-left-color: #67c23a;
}

.factory-header {
    padding: 10px 12px;
    background: #f8fafc;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e2e8f0;
    flex-shrink: 0;
}

.factory-name {
    display: flex;
    align-items: center;
    gap: 6px;
    font-weight: 600;
    font-size: 0.85rem;
    color: #1e293b;
}

.factory-icon {
    font-size: 0.9rem;
}

.factory-status {
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 0.65rem;
    font-weight: 600;
}

.factory-status.status-critical-text {
    background: #fef0f0;
    color: #f56c6c;
}

.factory-status.status-warning-text {
    background: #fdf6ec;
    color: #e6a23c;
}

.factory-status.status-normal-text {
    background: #f0f9eb;
    color: #67c23a;
}

.factory-points {
    padding: 4px 0;
}

.point-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 12px;
    border-bottom: 1px solid #f1f5f9;
}

.point-row:last-child {
    border-bottom: none;
}

.point-name {
    font-size: 0.75rem;
    color: #334155;
    display: flex;
    align-items: center;
    gap: 5px;
    flex: 1;
}

.point-badge {
    font-size: 0.55rem;
    padding: 1px 4px;
    border-radius: 8px;
    font-weight: 500;
}

.point-badge.critical {
    background: #fef0f0;
    color: #f56c6c;
}

.point-badge.warning {
    background: #fdf6ec;
    color: #e6a23c;
}

.point-values {
    display: flex;
    align-items: center;
    gap: 6px;
    flex-shrink: 0;
}

.current-value {
    font-weight: 600;
    font-size: 0.8rem;
    color: #1e293b;
}

.current-value.value-critical {
    color: #f56c6c;
}

.current-value.value-warning {
    color: #e6a23c;
}

.trend-icon {
    font-size: 0.65rem;
}

.trend-up {
    color: #f56c6c;
}

.trend-down {
    color: #67c23a;
}

.trend-stable {
    color: #94a3b8;
}

/* 滚动条样式 */
.left-panel::-webkit-scrollbar,
.factories-section::-webkit-scrollbar,
.alert-list::-webkit-scrollbar {
    width: 4px;
}

.left-panel::-webkit-scrollbar-track,
.factories-section::-webkit-scrollbar-track,
.alert-list::-webkit-scrollbar-track {
    background: #f1f1f1;
}

.left-panel::-webkit-scrollbar-thumb,
.factories-section::-webkit-scrollbar-thumb,
.alert-list::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 2px;
}

/* 响应式 */
@media (max-width: 1200px) {
    .main-content {
        flex-direction: column;
    }

    .left-panel {
        width: 100%;
        flex-direction: row;
        gap: 16px;
        height: auto;
    }

    .pie-card {
        flex: 1;
    }

    .alert-card {
        flex: 1;
        max-height: 400px;
    }

    .factories-grid {
        grid-template-columns: repeat(2, 1fr);
    }
}

@media (max-width: 768px) {
    .dashboard-container {
        padding: 12px;
    }

    .dashboard-header {
        flex-direction: column;
        gap: 10px;
        text-align: center;
    }

    .header-right {
        width: 100%;
        justify-content: center;
        flex-wrap: wrap;
    }

    .control-buttons {
        order: 1;
    }

    .left-panel {
        flex-direction: column;
    }

    .factories-grid {
        grid-template-columns: 1fr;
    }
}
</style>
