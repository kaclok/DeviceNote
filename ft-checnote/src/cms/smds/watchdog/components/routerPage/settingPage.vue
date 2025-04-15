<template>
    <div class="custom-content">
        <el-container class="custom-content">
            <el-aside class="layout-side">
                <el-row class="tac">
                    <el-col>
                    <el-menu
                        active-text-color="#ffd04b"
                        background-color="#545c64"
                        class="el-menu-vertical-demo"
                        default-active="1"
                        text-color="#fff"
                        @select="selectItem"
                    >
                        <el-menu-item index="1">
                            <el-icon><icon-menu /></el-icon>
                            <span>所有指标</span>
                        </el-menu-item>
                        <el-menu-item index="2">
                            <el-icon><icon-menu /></el-icon>
                            <span>新增指标</span>
                        </el-menu-item>
                        <el-menu-item index="3">
                            <el-icon><document /></el-icon>
                            <span>移除指标</span>
                        </el-menu-item>
                        <el-menu-item index="4">
                            <el-icon><setting /></el-icon>
                            <span>修改指标</span>
                        </el-menu-item>
                    </el-menu>
                    </el-col>
                </el-row>
            </el-aside>
            <el-main class="layout-content">
                <div style="background-color: aqua;"></div>
            </el-main>
        </el-container>

    
    </div>
</template>
  
<script lang="ts" setup>
    import {
    Document,
    Menu as IconMenu,
    Location,
    Setting,
    } from '@element-plus/icons-vue'
    const selectItem = (key: string, keyPath: string[]) => {
        console.log(key, keyPath)
    }
</script>

<style scoped>
.layout-side {
  width: 300px;
  background-color: #545c64;
  border: 1px solid rgba(125, 123, 123, 0.946);
  border-radius: 5px;
}

.custom-content {
  padding: 0; 
  height: 100%;
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