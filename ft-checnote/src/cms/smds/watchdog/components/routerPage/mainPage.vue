<script setup>
import LeftSide from '../side/LeftSide.vue'
import Header from '../header/Header.vue';
import Content from '../content/content_navigator.vue'
</script>

<template>
  <div class="custom-content">
    <el-container class="custom-content">
        <el-aside class="layout-side">
            <LeftSide></LeftSide>   
        </el-aside>
        <el-main class="layout-content">
            <Content></Content>
        </el-main>
    </el-container>
  </div>
</template>

<style scoped>
.layout-side {
  width: 300px;
  background-color: #545c64;
  border: 1px solid rgba(125, 123, 123, 0.946);
  border-radius: 5px;
}

.custom-content {
  padding: 0; 
  height: 94%;
  width: 100%;
  background-color:#545c64
}

.layout-content {
  padding: 0; 
  height: 100%;
  width: 100%;
  border: 1px solid rgba(125, 123, 123, 0.946);
  border-radius: 5px;
}
</style>
