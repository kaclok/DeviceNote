<template>
    <el-row class="tac">
      <el-col>
        <el-menu
          active-text-color="#ffd04b"
          background-color="#545c64"
          class="el-menu-vertical-demo"
          default-active="2"
          text-color="#fff"
          @open="handleOpen"
          @close="handleClose"
          collapse-transition=false
        >
          <el-sub-menu index="1">
            <template #title>
              <el-icon><location /></el-icon>
              <span>电石一分厂</span>
            </template>
            <el-menu-item-group title="一工段">
              <el-menu-item index="1-1">1号电石炉</el-menu-item>
              <el-menu-item index="1-2">电炉液压站</el-menu-item>
            </el-menu-item-group>
            <el-menu-item-group title="二工段">
              <el-menu-item index="1-3">3号电石炉</el-menu-item>
            </el-menu-item-group>
            <el-sub-menu index="1-4">
              <template #title>4号电石炉</template>
            </el-sub-menu>
          </el-sub-menu>
          <el-menu-item index="2">
            <el-icon><location /></el-icon>
            <span>电石二分厂</span>
          </el-menu-item>
          <el-menu-item index="3">
            <el-icon><location /></el-icon>
            <span>兰炭分厂</span>
          </el-menu-item>
          <el-menu-item index="4">
            <el-icon><location /></el-icon>
            <span>白灰分厂</span>
          </el-menu-item>
        </el-menu>
      </el-col>
    </el-row>
  </template>
  
  <script lang="ts" setup>
  import {
    Document,
    Menu as IconMenu,
    Location,
    Setting,
  } from '@element-plus/icons-vue'
  const handleOpen = (key: string, keyPath: string[]) => {
    console.log(key, keyPath)
  }
  const handleClose = (key: string, keyPath: string[]) => {
    console.log(key, keyPath)
  }
  </script>
  