<template>
    <el-button @click="onClicked"> 发送 </el-button>>
</template>

<script setup>
import {useSse} from '@/framework/utils/use/useSse.js';
import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

useSse(import.meta.env.VITE_BASE_API + "/sse/connect");

function onClicked() {
    console.log("<UNK>");
    axiosR.get("sse/testPublish", {
        params: {
            message: "哈哈哈"
        },
    })
}

</script>

<style scoped>

</style>