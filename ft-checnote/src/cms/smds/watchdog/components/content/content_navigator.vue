<template>
  <div class="content_container">
    <div class="content_container">
      <HomePage></HomePage>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import type { TabPaneName } from 'element-plus'
import HomePage from './home_page.vue'

</script>

<style>
.demo-tabs>.el-tabs__content {
  padding: 10px;
  width: 98%;
  height: 100%;
  color: #303133;
  font-size: 16px;
  font-weight: 500;
}

.content_container {
  padding: 0px;
  width: 100%;
  height: 100%;
}
</style>