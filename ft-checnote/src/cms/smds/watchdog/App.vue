<script setup>

import LeftSide from './components/side/LeftSide.vue'
import Header from './components/header/Header.vue';
import Content from './components/content/content_navigator.vue'

</script>

<template>
  <div class="common-layout custom-container">
      <el-container class="custom-container">
          <el-header class="custom-header">
              <Header></Header>
          </el-header>
          <router-view>
        
          </router-view>
          
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

.custom-container {
  padding: 0; 
  height: 100%;
  width: 100%;
  background-color: #303133;
}

.custom-header {
  padding: 0; 
  height: 6%;
  width: 100%;
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

