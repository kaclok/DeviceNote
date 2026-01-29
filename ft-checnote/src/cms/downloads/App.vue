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

                <button @click="item.showVersions = !item.showVersions">
                    历史版本
                </button>

                <ul v-if="item.showVersions">
                    <li v-for="v in item.versions" :key="v.version">
                        {{ v.version }}
                        <a :href="v.downloadUrl">下载</a>
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

onMounted(async () => {
    const res = await fetch('/configs/downloads/list.json')
    appList.value = await res.json()
})

const download = (item) => {
    // 方式一：直接跳转下载（最简单）
    window.open(item.latestDownloadUrl)

    // 或方式二：a 标签方式（可控文件名）
    // const a = document.createElement('a')
    // a.href = item.downloadUrl
    // a.download = item.name
    // a.click()
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
}

.download-btn:hover {
    background: #66b1ff;
}
</style>
