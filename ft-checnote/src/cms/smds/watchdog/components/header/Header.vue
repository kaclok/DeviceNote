<template>
  <div class="page-container">
      <div class="page-title">
          <img src="@/assets/st_logo.png" class="page-logo-content" alt="">
          <span class="page-title-content">{{ "异常工况预警系统" }}</span>

          <div class="left-menu">
              <el-menu mode="horizontal" background-color="#1C4785" text-color="#DCDCDC" active-text-color="#ffffff"
                       :default-active="activeIndex2"
                       @select="handleSelect">
                  <el-menu-item index="1">详情</el-menu-item>
                  <el-menu-item index="2">设置</el-menu-item>
                  <el-menu-item index="3">查询</el-menu-item>
              </el-menu>
          </div>
      </div>
  </div>
</template>

<script lang="ts" setup>
import {ref} from "vue";
import router from "../../router/router";

const activeIndex2 = ref("1")

const handleSelect = (key: string, keyPath: string[]) => {
  if (key == '1') {
    router.push('/home')
  } else if (key == '2') {
    router.push('/settings')
  }
  else if (key == '3') {
      window.open("http://10.8.54.110:8900/#/real_time");
  }
};

</script>

<style scoped>
.page-container {
    width: 100%;
    height: 100%;
    background-color: #F5F5F5;
    display: flex;
    flex-direction: column;

    .page-title {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        background-color: #1C4785;

        .page-logo-content {
            position: relative;
            left: 20px;

            width: 40px;
            height: 40px;
            object-fit: contain;
        }

        .page-title-content {
            min-width: 250px;
            color: #ffffff;
            font-size: 18px;
            font-weight: bolder;
            text-align: center;
        }

        .left-menu {
            position: relative;
            left: -60px;

            width: 850px;
            padding-left: 56px;
        }
    }

    .page-content {
        flex: 1;
        width: 100%;
        display: flex;
        overflow: hidden;
    }
}
</style>
