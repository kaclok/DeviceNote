<template>
    <div class="container">
        <!-- 父容器 -->
        <div class="rectangle-container" :style="containerStyle">
            <div v-for="(rectangle, index) in rectangles" :key="index" :style="getRectangleStyle(rectangle.flag)">
                <el-button v-if="hasMsg(index)" type="primary" size="small" round :icon="Delete" @click="onClearBtnClicked(rectangle, index)"
                           style="position: absolute; left: 0; top: 0; width: 5%;">
                </el-button>

                <div>
                    {{ showMsg(rectangle, index) }}
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import {computed, defineProps, ref} from 'vue';
import {branchInfo} from '@/cms/smds/watchdog/store/global.js'
import {Delete} from "@element-plus/icons-vue";

const props = defineProps(['id']);

const socketInfo = ref(branchInfo())

function getMsgs() {
    let id = props.id;
    return socketInfo.value.getBranchInfo_1(id).reverse()
}

function showMsg(rectangle, index) {
    let msgs = getMsgs()
    let show = rectangle.idx
    if (0 <= index && index < msgs.length) {
        let msg = msgs[index];
        show = '[' + msg.batchIndex + '-' + msg.dataIndex + '] ' + msg.branchId + "-" + msg.message
    }
    return show
}

function hasMsg(index) {
    let msgs = getMsgs()
    return 0 <= index && index < msgs.length
}

function onClearBtnClicked(rectangle, index) {
    let msgs = getMsgs().reverse()
    if (0 <= index && index < msgs.length) {
        socketInfo.value.removeSocketInfo(msgs[index])
    }
}

const rectangles = ref([
    {idx: '1', flag: true},
    {idx: '2', flag: true},
    {idx: '3', flag: true},
    {idx: '4', flag: true},
    {idx: '5', flag: true},
    {idx: '6', flag: true},
]);

// 最大列数，确保宽度动态调整
const maxColumns = 1;

// 动态生成 grid 布局的样式
const containerStyle = computed(() => {
    const num = rectangles.value.length;
    const columns = Math.min(num, maxColumns); // 计算实际列数
    return {
        display: 'grid',
        gridTemplateColumns: `repeat(${columns}, 1fr)`, // 每行最多显示 maxColumns 个元素
        gap: '4px', // 元素之间的间距
    };
});


function getRectangleStyle(flag) {
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

        fontSize: '13px',

        position: 'relative',
        left: "0",
        top: "0",
    };
}

function getIndex(n) {
    return (n - 1) % (rectangles.value.length) + 1
}

</script>

<style scoped>
.container {
    width: 100%;
    height: 91.5%;
    display: flex;
    flex-direction: column;
    margin-top: 4px; /* 按钮与矩形容器的间距 */
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
