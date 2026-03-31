<template>
    <div class="content_container">
        <el-row style="height: 94%;">
            <el-col v-for="(item, index) in socketInfo.allBranchInfo" :key="index" :span="8">
                <div class="content_container">
                    <span class="el-row-status">{{ item.id + '-' + item.name + '(' + getMsgLength(item.id) + '/6)' }}</span>
                    <ContentItem :id="item.id"/>
                </div>
            </el-col>
        </el-row>
    </div>

</template>

<script setup>

import ContentItem from './content_item.vue'
import {branchInfo} from '@/cms/smds/watchdog/store/global.js'

const socketInfo = ref(branchInfo())

function getMsgLength(id) {
    return socketInfo.value.getBranchInfo_1(id).length
}

</script>

<style>
.el-row {
    margin-bottom: 3px;
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
    padding: 30px;
    width: 100%;
    height: 100%;
}
</style>
