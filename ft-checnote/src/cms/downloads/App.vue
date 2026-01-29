<template>
    <div class="app-store">
        <h1 class="title">资源下载中心</h1>

        <div class="app-grid">
            <div
                class="app-card"
                v-for="item in appList"
                :key="item.id"
            >
                <img class="app-icon" :src="logo_book" alt="icon"/>

                <div class="app-name">{{ item.name }}</div>

                <button class="download-btn" @click="download(item)">
                    下载最新版
                </button>

                <!-- 修改点击事件 -->
                <button @click="toggleVersions(item.id)">
                    历史版本
                </button>

                <!-- 判断当前item是否是被选中的那个 -->
                <ul v-if="selectedAppId === item.id">
                    <li v-for="v in item.versions" :key="v.version">
                        {{ v.version }}
                        <a :href="v.downloadUrl" @click.stop>下载</a>
                    </li>
                </ul>
            </div>
        </div>
    </div>
</template>

<script setup>
import {ref} from 'vue'
import logo_book from '@/assets/image/answer_marking.png';

const appList = ref([])
// 新增：记录当前被选中的app的id
const selectedAppId = ref(null)

// 修改onMounted
import {onMounted} from 'vue'

onMounted(async () => {
    try {
        const res = await fetch('/configs/downloads/list.json')
        const data = await res.json()
        // 初始化时给每个item添加id属性（如果数据中没有）
        appList.value = data.map((item, index) => ({
            ...item,
            id: item.id || `app-${index}` // 如果数据中没有id，自动生成
        }))
    } catch (error) {
        console.error('加载应用列表失败:', error)
    }
})

// 修改toggle函数
const toggleVersions = (appId) => {
    if (selectedAppId.value === appId) {
        // 如果点击的是已展开的app，则收起
        selectedAppId.value = null
    } else {
        // 如果点击的是其他app，则展开这个，收起其他的
        selectedAppId.value = appId
    }
}

const download = (item) => {
    window.open(item.latestDownloadUrl)
}
</script>

<style scoped>
.app-store {
    padding: 20px;
}

.title {
    text-align: center;
    margin-bottom: 20px;
}

.app-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 20px;
}

.app-card {
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    text-align: center;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    position: relative;
}

.app-icon {
    width: 64px;
    height: 64px;
    object-fit: contain;
    margin-bottom: 10px;
}

.app-name {
    font-size: 14px;
    margin-bottom: 12px;
}

.download-btn {
    background: #409eff;
    color: #fff;
    border: none;
    padding: 6px 14px;
    border-radius: 4px;
    cursor: pointer;
    margin-right: 8px;
}

.download-btn:hover {
    background: #66b1ff;
}

/* 历史版本按钮样式 */
button:not(.download-btn) {
    background: #f0f0f0;
    border: 1px solid #dcdcdc;
    padding: 6px 12px;
    border-radius: 4px;
    cursor: pointer;
}

button:not(.download-btn):hover {
    background: #e0e0e0;
}

/* 历史版本列表样式 */
ul {
    margin-top: 12px;
    padding: 0;
    list-style: none;
    background: #f9f9f9;
    border-radius: 4px;
    padding: 8px;
    text-align: left;
}

li {
    padding: 6px 0;
    border-bottom: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
}

li:last-child {
    border-bottom: none;
}

li a {
    color: #409eff;
    text-decoration: none;
    padding: 2px 8px;
    border-radius: 3px;
}

li a:hover {
    background: #ecf5ff;
}
</style>
