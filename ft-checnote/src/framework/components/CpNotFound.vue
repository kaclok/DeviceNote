<template>
    <div>
        糟糕！页面没有找到。。。呜呜呜
    </div>
</template>

<script setup lang="ts">
import {getCurrentInstance, onMounted} from 'vue'

const instance = getCurrentInstance()
if (instance != null) {
    const _this = instance.appContext.config.globalProperties //vue3获取当前this

    onMounted(() => {
        console.log(_this.$route.params)
    })
}
</script>

<style scoped>
</style>
