<template>
    <el-container class="layout-container-demo" style="height: 100%">
        <el-aside width="80px;">
            <el-scrollbar>
                <el-menu :default-active="curzzId"
                         unique-opened
                         @select="onZZClicked">
                    <el-menu-item index="1">公辅装置</el-menu-item>
                    <el-menu-item index="2">热动力站装置</el-menu-item>
                    <el-menu-item index="3">烧碱装置</el-menu-item>
                    <el-sub-menu index="4">
                        <template #title>聚氯乙烯装置</template>
                        <el-menu-item index="4-1">聚合</el-menu-item>
                        <el-menu-item index="4-2">乙炔</el-menu-item>
                        <el-menu-item index="4-3">VCM</el-menu-item>
                        <el-menu-item index="4-4">固液分离</el-menu-item>
                    </el-sub-menu>
                </el-menu>
            </el-scrollbar>
        </el-aside>

        <el-container>
            <div>
                <div class="flex flex-wrap gap-4 items-center">
                    <el-select
                        v-model="curLevelId"
                        placeholder="请选择:"
                        style="width: 240px"
                        @change="onLevelClicked"
                    >
                        <el-option
                            v-for="item in options"
                            :key="item.value"
                            :label="item.label"
                            :value="item.value"
                        />
                    </el-select>
                </div>

                <el-main>
                    <el-scrollbar>
                        <el-table :data="tableData">
                            <el-table-column prop="name" label="设备名称" width="200"/>
                            <el-table-column prop="weihao" label="设备位号" width="200"/>
                            <el-table-column prop="size" label="设备型号" width="200"/>
                            <el-table-column prop="prev_fix_a_time" label="上次轴伸端时间" width="180"/>
                            <el-table-column prop="a_duration" label="下次轴伸端时间" width="180"/>
                            <el-table-column prop="prev_fix_b_time" label="上次非轴伸端时间" width="180"/>
                            <el-table-column prop="b_duration" label="下次轴伸端时间" width="180"/>
                        </el-table>
                    </el-scrollbar>
                </el-main>
            </div>
        </el-container>
    </el-container>

</template>

<script lang="js" setup>
import {ref, onUnmounted, onMounted} from "vue";
import {doGet, doPost} from "@/framework/services/net/Request.js"
import {Menu as IconMenu, Message, Setting} from '@element-plus/icons-vue'

const PAGE_SIZE = 19

const curzzId = ref("1")       // 菜单默认选中第一项
const curLevelId = ref("1")      // 下拉框默认选中高压电机

let AC_getList = new AbortController();
let loadingList = ref(false);
let curPageIndex = ref(1)

const tableData = ref([])
// 所选装置的特定电机的列表个数
let tableTotal = 0

const options = [
    {
        value: "1",
        label: '高压电动机',
    },
    {
        value: '2',
        label: '低压电动机',
    },
    {
        value: '3',
        label: '免维护电机',
    },
]

onMounted(() => {
    _reqList();
});

function onZZClicked(zzId) {
    curzzId.value = zzId

    // 重置本地数据
    curPageIndex.value = 1
    tableTotal = 0

    // 请求服务器最新的列表数据
    _reqList();
}

function onLevelClicked(levelId) {
    curLevelId.value = levelId

    // 重置本地数据
    curPageIndex.value = 1
    tableTotal = 0

    // 请求服务器最新的列表数据
    _reqList();
}

function _reqList() {
    doGet("x/sbrhjy/getList", {
        zzId: curzzId.value,
        levelId: curLevelId.value,
        pageNum: curPageIndex.value,
        pageSize: PAGE_SIZE
    }, AC_getList.signal, () => {
        loadingList.value = true;
    }, (r, data) => {
        loadingList.value = false;

        if (r) {
            tableData.value = data.data.list
            tableTotal = data.data.total
        } else {

        }
    })
}

</script>

<style scoped>
.layout-container-demo .el-header {
    position: relative;
    background-color: var(--el-color-primary-light-7);
    color: var(--el-text-color-primary);
}

.layout-container-demo .el-aside {
    color: var(--el-text-color-primary);
    background: #fff;
    width: 160px;
}

.layout-container-demo .el-menu {
    border-right: none;
}

.layout-container-demo .el-main {
    padding-top: 10px;
    padding-left: 0;
}

.layout-container-demo .toolbar {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    right: 20px;
}

/*https://element-plus.org/zh-CN/component/menu.html*/
.el-menu--horizontal {
    --el-menu-horizontal-height: 40px;
}

.el-menu--vertical {
    --el-menu-horizontal-height: 30px;
}
</style>