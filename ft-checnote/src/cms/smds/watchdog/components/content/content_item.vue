<template>
    <div class="container">
        <!-- <span class="el-row-status">电石二分厂</span> -->
      <!-- 父容器 -->
      <div class="rectangle-container" :style="containerStyle">
        <div v-for="(rectangle, index) in rectangles" :key="index" :style="getRectangleStyle(rectangle.flag)">
          {{ rectangle.label }}
        </div>
      </div>
    </div>
  </template>
  
  <script>
  import { defineComponent, ref, computed } from 'vue';
  
  export default defineComponent({
    setup() {
      const rectangles = ref([
        {id:'1', label: '1', flag: true },
        {id:'2', label: '2', flag: true },
        {id:'3', label: '3', flag: true },
        {id:'4', label: '4', flag: true },
        {id:'5', label: '5', flag: true },
        {id:'6', label: '6', flag: true }
      ]);
  
      // 最大列数，确保宽度动态调整
      const maxColumns = 3;
  
      // 动态生成 grid 布局的样式
      const containerStyle = computed(() => {
        const num = rectangles.value.length;
        const columns = Math.min(num, maxColumns); // 计算实际列数
        return {
          display: 'grid',
          gridTemplateColumns: `repeat(${columns}, 1fr)`, // 每行最多显示 maxColumns 个元素
          gap: '5px', // 元素之间的间距
        };
      });
  
      // 添加矩形元素
      const addElement = () => {
        const newLabel = `矩形 ${rectangles.value.length + 1}`;
        rectangles.value.push({ label: newLabel });
      };
  
      // 删除最后一个矩形元素
      const removeElement = () => {
        rectangles.value.pop();
      };

      const getRectangleStyle = (flag) => {
      let backgroundColor = '#42b983'; // 默认正常状态
      if (flag === true) {
        backgroundColor = 'green';
      } else if (flag === false) {
        backgroundColor = 'red';
      }
      return {
        backgroundColor,
        color: 'white',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: '5px',
        boxSizing: 'border-box',
        border: '1px solid #ddd',
      };
    };
  
      return {
        rectangles,
        addElement,
        removeElement,
        containerStyle,
        getRectangleStyle
      };
    }
  });
  </script>
  
  <style scoped>
  .container {
    width: 100%;
    height: 100%; /* 父容器占满整个页面 */
    display: flex;
    flex-direction: column;
    gap: 10px; /* 按钮与矩形容器的间距 */
  }
  
  .rectangle-container {
    flex: 1;
  }

  .el-row-status {
    display: flex;
    justify-content: center;
    text-align: center;
    line-height: 30px;
    color: white;
    background-color: #04b3fe;
}
  </style>
  