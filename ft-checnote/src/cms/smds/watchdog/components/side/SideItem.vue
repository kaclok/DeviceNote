<template>
    <div>
        <el-button>
            {{'[' + msg.batchIndex + '-' + msg.dataIndex + '] ' + msg.branchId + '-' + msg.branchName + ' 趋势异常:'}}
        </el-button>
    </div>
</template>

<style scoped>
.content_container {
    width: 100%;
    height: 100%;
    padding: 0px;
}

</style>

<script setup>
import {computed, defineProps, ref} from 'vue';
import {branchInfo} from '@/cms/smds/watchdog/store/global.js'
const socketInfo = branchInfo()
const props = defineProps(['msg', 'branchId']);

function getMsgs() {
    if (props.branchId.value === 0) {
        return socketInfo.socketInfo
    }
    return socketInfo.getBranchInfo_2(props.branchId.value)
}

function onClearBtnClicked() {
    if (props.branchId.value === 0) {
        socketInfo.removeAll()
    } else {
        socketInfo.removeByBranchId(props.branchId.value)
    }
}

function removeAlertItem(index, msg) {
    socketInfo.removeSocketInfo(msg)
}

</script>
