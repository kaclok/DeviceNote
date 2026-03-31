<template>
    <div class="content_container">
        <div class="top_layout">
            <el-select v-model="branchId" filterable placeholder="Select" style="width: 50%">
                <el-option
                    v-for="item in options"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                />
            </el-select>

            <el-button type="primary" size="small" round :icon="Delete" @click="onClearBtnClicked"
                       style="margin-left: 5px; margin-top: 5px">
                {{getMsgs().length}}
            </el-button>
        </div>

        <el-alert
            v-for="(msg, index) in getMsgs()"
            :key="msg.index"
            :title="'[' + msg.batchIndex + '-' + msg.dataIndex + '] ' + msg.branchId + '-' + msg.branchName + ' 趋势异常:'"
            :description="msg.message"
            :type="'warning'"
            :effect="'dark'"
            :closeable="true"
            @close="removeAlertItem(index, msg)"
        >
        </el-alert>

<!--        <SideItem
            v-for="(msg, index) in getMsgs()"
            :key="msg.index"
            :msg="msg"
            :branchId="branchId"
        >
        </SideItem>-->
    </div>
</template>

<script lang="ts" setup>
import {onMounted} from 'vue';
import {connectWebSocket} from '../../socket/webSocket.js'
import SideItem from './SideItem.vue'
import {branchInfo} from '../../store/global.js'

import {Delete} from "@element-plus/icons-vue";

const options = [
    {
        id: 0,
        name: '所有',
    },
    {
        id: 1,
        name: '电石一分厂',
    },
    {
        id: 2,
        name: '电石二分厂',
    },
    {
        id: 3,
        name: '电石三分厂',
    },
    {
        id: 4,
        name: '白灰分厂',
    },
    {
        id: 5,
        name: '兰炭分厂',
    },
    {
        id: 6,
        name: '热电分厂',
    },
]

onMounted(() => {
    connectWebSocket()
})

const socketInfo = branchInfo()
const branchId = ref(0)

function getMsgs() {
    if (branchId.value === 0) {
        return socketInfo.socketInfo
    }
    return socketInfo.getBranchInfo_2(branchId.value).reverse()
}

function onClearBtnClicked() {
    if (branchId.value === 0) {
        socketInfo.removeAll()
    } else {
        socketInfo.removeByBranchId(branchId.value)
    }
}

function removeAlertItem(index, msg) {
    socketInfo.removeSocketInfo(msg)
}

</script>

<style scoped>

.el-alert {
    margin: 20px 0 0;
}

.el-alert:first-child {
    margin: 0;
}

.top_layout {
    display: flex;
    justify-content: center;
    width: 100%;
    height: 2%;
    color: Red;
    font-size: large;
}

.item_layout {
    width: 100%;
    height: 50px;
    display: flex;
    justify-content: center;
    align-items: center;
    color: black;
    font-size: medium;
    background-color: red;
}

.bottom_layout {
    display: flex;
    justify-content: center;
    width: 100%;
    height: 95%;
}

.content_container {
    width: 100%;
    height: 100%;
    padding: 0px;
}

</style>
