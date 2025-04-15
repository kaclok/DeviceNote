<template>
  <div class="flex justify-between items-center flex-wrap header-content">
    <div
      v-for="(shadow, i) in shadowGroup"
      :key="i"
      class="flex flex-col justify-center items-center header-content"
      m="auto"
      w="46"
    >
      <el-menu
        :default-active="activeIndex2"
        class="header-content"
        mode="horizontal"
        background-color="#545c64"
        text-color="#fff"
        active-text-color="#ffd04b"
        @select="handleSelect"
      >
        <!-- 第一个菜单项 -->
        <el-menu-item index="1">
          <div
            class="inline-flex header-content"
            h="30"
            w="30"
            m="2"
            :style="{
              boxShadow: `var(${getCssVarName(shadow.type)})`,
            }"
          >
            <div style="width: 20px;"></div>
            <br />
            <img src="../../assets/smds.png" width="66" height="40" />
            <b style="color: aliceblue; font-size: 20px;">
              异常工况预警系统
            </b>
          </div>
        </el-menu-item>
        <!-- 第二个菜单项，使用 CSS 推动到右侧 -->
        <!-- <el-menu-item index="2" class="menu-item-right">管理</el-menu-item>-->
      </el-menu>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref } from "vue";
import router from "../../router/router";

const shadowGroup = ref([
  {
    name: "Basic Shadow",
    type: "",
  },
]);

const activeIndex2 = ref("1");

const handleSelect = (key: string, keyPath: string[]) => {
  if (key == '1') {
    router.push('/home')
  } else if (key == '2') {
    router.push('/settings')
  }
};

const getCssVarName = (type: string) => {
  return `--el-box-shadow${type ? "-" : ""}${type}`;
};
</script>

<style scoped>
.header-content {
  display: flex;
  width: 100%; /* 让内部元素占满 header 的宽度 */
  height: 100%; /* 让内部元素高度与 header 保持一致 */
  align-items: center; /* 垂直居中 */
}

.menu-item-right {
  margin-left: auto; /* 推动到右侧 */
}
</style>
