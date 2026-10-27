<template>
    <div class="setting-page">
        <el-tabs v-model="tab" class="setting-tabs">
            <el-tab-pane label="磅房与绑定" name="scales">
                <Scales/>
            </el-tab-pane>
            <el-tab-pane label="地图配置" name="config" lazy>
                <Config/>
            </el-tab-pane>
        </el-tabs>
    </div>
</template>

<script setup lang="js">
import {ref} from 'vue'
import Scales from './scales.vue'
import Config from './config.vue'

const tab = ref('scales')
</script>

<style scoped>
.setting-page {
    min-height: 100%;
}

.setting-tabs :deep(.el-tabs__header) {
    margin-bottom: 12px;
}

.setting-tabs :deep(.el-tabs__item) {
    font-size: 14px;
}
</style>
