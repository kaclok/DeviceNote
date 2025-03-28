<template>
</template>

<script setup>
import {useSse} from '@/framework/utils/use/useSse.js';

useSse(import.meta.env.VITE_BASE_API + "/sse/connect");

</script>

<style scoped>

</style>