<template>
    <div class="content_container">
        <div class="top_layout">
            <b>异常工况</b>
        </div>

        <el-alert
            v-for="(alert, index) in socketInfo.socketInfo"
            :key="index"
            :title="'请确认正常后再关闭该条目'"
            :description="alert.message"
            :type="'warning'"
            :effect="'dark'"
            :closeable="true"
            @close="removeAlertItem(index)"
            show-icon
            center/>
    </div>
</template>

<script lang="ts" setup>
import {
    Document,
    Menu as IconMenu,
    Location,
    Setting,
} from '@element-plus/icons-vue'
import {effect, reactive, onMounted} from 'vue';
import {connectWebSocket, sendMessage} from '../../socket/webSocket.js'
import {branchInfo} from '../../store/global.js'

onMounted(() => {
    connectWebSocket()
})

const socketInfo = branchInfo()


function removeAlertItem(index) {
    // this.sideItems[iwndex].visible = false;
}

// const sideItems = reactive([
//   {
//     info: '电石一分厂1#电石炉氧气浓度异常'
//   },
//   {
//     info: '电石一分厂1#电石炉1号电极电流激增'
//   },
//   {
//     info: '电石三分厂5#电石炉氢浓度异常'
//   },
// ])

const handleOpen = (key: string, keyPath: string[]) => {
    console.log(key, keyPath)
}
const handleClose = (key: string, keyPath: string[]) => {
    console.log(key, keyPath)
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
    height: 5%;
    color: white;
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