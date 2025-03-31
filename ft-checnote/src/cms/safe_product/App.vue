<template>
    <el-button @click="onClicked"> 发送 </el-button>
</template>

<script setup>
import {useSse} from '@/framework/utils/use/useSse.js';
import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

useSse("/api/sse/connect");

function onClicked() {
    axiosR.get("sse/testPublish", {
        params: {
            message: "哈哈哈"
        },
    }).then(res=>{
        console.log(res);
    }).catch(err=>{
        console.log(err);
    })
}

</script>

<style scoped>

</style>