<template>
    <div class="content_container">

        <el-row style="height: 48%;">
            <el-col v-for="(item, index) in allElem1" :key="index" :span="8">
                <div class="content_container">
                    <span class="el-row-status">{{ item.label }}</span>
                    <ContentItem></ContentItem>
                </div>
            </el-col>
        </el-row>
        <el-row style="height: 48%;">
            <el-col v-for="(item, index) in allElem2" :key="index" :span="8">
                <div class="content_container">
                    <span class="el-row-status">{{ item.label }}</span>
                    <ContentItem></ContentItem>
                </div>
            </el-col>
        </el-row>
    </div>

</template>

<script setup>

import {reactive} from 'vue'
import ContentItem from './content_item.vue'

const allElem1 = reactive([
    {label: '电石一分厂'},
    {label: '电石二分厂'},
    {label: '电石三分厂'}
]);
const allElem2 = reactive([
    {label: '白灰分厂'},
    {label: '兰炭分厂'},
    {label: '热电分厂'}
]);

</script>

<style>
.el-row {
    margin-bottom: 0px;
    margin-left: 0px;
    margin-right: 0px;
}

.el-row-status {
    display: flex;
    justify-content: center;
    text-align: center;
    line-height: 30px;
    color: white;
    background-color: #04b3fe;
}

.el-col {
    padding: 1px;
    border-radius: 10px;
    border: 1px solid rgba(125, 123, 123, 0.946);
}

.grid-content {
    border-radius: 10px;
    min-height: 36px;
}


.content_container {
    padding: 0px;
    width: 100%;
    height: 100%;
}
</style>