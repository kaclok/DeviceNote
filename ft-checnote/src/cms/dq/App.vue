<script setup>

import {SysX} from "@/cms/daily_paper/system/SysX.js";
import {Singleton} from "@/framework/services/Singleton.js";
import {ref, onUnmounted, onMounted} from "vue";
import {ElMessage} from "element-plus";
import {ExcelService} from "@/framework/services/ExcelService.js";
import {DateTimeUtil} from "@/framework/utils/DateTimeUtil.js";

const PAGE_SIZE = 13

let AC_getList = new AbortController();
let AC_add = new AbortController();
let AC_del = new AbortController();
let AC_export = new AbortController();

let loadingList = ref(false);
let loadingAdd = ref(false);
let loadingDel = ref(false);
let loadingExport = ref(false)

let curBJId = ref("3");
let curDate = ref(Date.now());
let dateRange = ref('');

let curPageIndex = ref(1)

const formList = ref([
    /*{
        id: 1,
        c_name: "测试",
        c_person: "崔斌斌",
    }*/
])
let formTotal = 0

let formRef = ref(null)
const __info__ = {
    reset() {
        this.c_name = null
        this.c_desc = null
        this.c_progress = null
        this.c_result = null
        this.c_person = null
        this.c_summary = null
        this.c_comment = null
        this.c_finish = 0

        return this
    }
}

let deviceRecordInfo = ref(__info__)

// 1查看 2新建
let recordMode = ref(0)
let showDialogue = ref(false)

// 清理定时器，事件监听器，异步函数
onUnmounted(() => {
    AC_getList.abort();
    AC_add.abort();
    AC_del.abort();
    AC_export.abort();
});

onMounted(() => {
    _getList();
});

function onDateChanged(date) {
    curDate.value = date;

    onMenuClicked(curBJId.value);
}

let beginTimestamp = null;
let endTimestamp = null;

function onDateRangeChanged(range) {
    beginTimestamp = Math.round(range[0]);
    endTimestamp = Math.round(range[1]);
}

function onExportAll() {
    if (beginTimestamp == null || endTimestamp == null) {
        // window.alert('请选择导出时间区间');
        ElMessage({
            showClose: true,
            message: '请选择导出时间区间',
            type: 'warning',
            center: true,
            duration: 2000,
        });

        return
    }

    Singleton.getInstance(SysX).export({
        bgId: curBJId.value,
        beginDate: beginTimestamp,
        endDate: endTimestamp
    }, AC_export.signal, () => {
        loadingExport.value = true;
    }, (r, data) => {
        loadingExport.value = false;

        ExcelService.ExportAOAToExcel1(data.data.rows, data.data.colNames, 'export_all', false);
    });
}

function onMenuClicked(menuIndex) {
    curBJId.value = menuIndex

    formTotal = 0
    curPageIndex.value = 1

    recordMode.value = 0
    showDialogue.value = false

    _getList(curPageIndex.value);
}

function onPageChanged(pageIndex) {
    if (pageIndex !== curPageIndex.value) {
        curPageIndex.value = pageIndex

        _getList(pageIndex);
    }
}

/*import {__IS_MOBILE_ONLY__, __IS_TABLET_ONLY__} from "@/framework/services/GlobalService.js";
console.log("__IS_MOBILE_ONLY__: " + __IS_MOBILE_ONLY__.value)
console.log("__IS_TABLET_ONLY__: " + __IS_TABLET_ONLY__.value)*/

function _getList(pageNum) {
    Singleton.getInstance(SysX).getList({
        bgId: curBJId.value,
        mills: curDate.value,
        pageNum: pageNum,
        pageSize: PAGE_SIZE
    }, AC_getList.signal, () => {
        loadingList.value = true;
    }, (r, data) => {
        loadingList.value = false;
        showDialogue.value = false

        if (r) {
            formList.value = data.data.list
            formTotal = data.data.total
        }
    });
}

function onAddClicked() {
    recordMode.value = 2

    formRef.value?.resetFields()
    deviceRecordInfo.value = __info__.reset()
    showDialogue.value = true
}

function onDeviceClicked(row) {
    if (row) {
        recordMode.value = 1
        showDialogue.value = true
        deviceRecordInfo.value = JSON.parse(JSON.stringify(row));
    }
}

function onDeleteClicked(row) {
    if (row) {
        Singleton.getInstance(SysX).del({
            bgId: row.bg_id,
            id: row.id,
        }, AC_del.signal, () => {
            loadingDel.value = true;
        }, (r, data) => {
            loadingDel.value = false;

            if (r) {
                _getList(curPageIndex.value);
            } else {
            }
        });
    }
}

function onSaveClicked() {
    let v = deviceRecordInfo.value;
    if (v.c_person === null || v.c_name === null) {
        return;
    }

    Singleton.getInstance(SysX).add({
        bgId: curBJId.value,
        info: JSON.stringify(deviceRecordInfo.value),
    }, AC_add.signal, () => {
        loadingAdd.value = true;
    }, (r, data) => {
        loadingAdd.value = false;
        showDialogue.value = false

        if (r) {
            _getList(curPageIndex.value);
        } else {
        }
    });
}

function onArrowChanged(newIndex, oldIndex) {
    console.log('curArrowIndex', newIndex)
}

</script>

<template>
    <div class="page-container">
        <div class="page-title">
            <img src="@/assets/st_logo.png" class="page-logo-content" alt="">
            <span class="page-title-content">{{ "电气检修记录" }}</span>

            <div class="page-menu-content">
                <el-menu mode="horizontal" background-color="#1C4785" text-color="#DCDCDC" active-text-color="#ffffff"
                         :default-active="curBJId"
                         @select="onMenuClicked">
                    <el-menu-item index="3">电气一班</el-menu-item>
                    <el-menu-item index="4">电气二班</el-menu-item>
                </el-menu>
            </div>

            <div style="position: absolute; right: 25px">
                <img class="avatar-img" src="@/assets/avatar.png" alt=""/>
            </div>
        </div>

        <div>
            <div style="width: 230px">
                <!--                <el-carousel height="40px" @change="onArrowChanged" indicator-position="none" arrow="always">
                                    <el-carousel-item>-->
                <el-date-picker style="width: 130px; height: 35px; padding-top: 10px; padding-left: 2px"
                                @change="onDateChanged"
                                :clearable="false"
                                v-model="curDate"
                                type="date"
                                :editable="false"
                                placeholder="选择日期"
                                format="YYYY/MM/DD"
                                value-format="x"
                                size="small"
                />
                <!--                    </el-carousel-item>
                                </el-carousel>-->
            </div>

            <div>
                <el-date-picker style="position: absolute; right: 60px; top: 60px"
                                @change="onDateRangeChanged"
                                :clearable="false"
                                v-model="dateRange"
                                type="daterange"
                                range-separator="至"
                                start-placeholder="导出开始日期"
                                end-placeholder="导出结束日期"
                                format="YYYY/MM/DD"
                                :editable="false"
                                value-format="x"
                                size="small"
                />

                <el-button @click="onExportAll" circle :dark="true" type="warning" style="position: absolute; right: 20px; top: 55px">导出
                </el-button>
            </div>
        </div>

        <div class="page-content">
            <!--中侧-->
            <div v-loading="loadingList"
                 style="width: 100%; padding-left: 0; padding-top: 5px">
                <!--https://element-plus.org/zh-CN/component/table.html#%E7%AD%9B%E9%80%89-->
                <el-button-group style="width: 100%;">
                    <el-button style="font-size: 12px; width: 100%; height: 30px" type="warning"
                               @click="onAddClicked">{{ "新增一条检修记录" }}
                    </el-button>
                </el-button-group>

                <el-table show-overflow-tooltip :data="formList" fit stripe border highlight-current-row max-height="100%">
                    <el-table-column sortable fixed type="index" label="序号" width="75"/>
                    <el-table-column prop="c_finish" label="完成情况" width="90">
                        <template #default="scope1">
                            <el-switch
                                v-model="scope1.row.c_finish"
                                disabled
                                inline-prompt
                                class="ml-2"
                                style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
                                active-text="已完成"
                                inactive-text="未完成"
                            />
                        </template>
                    </el-table-column>
                    <el-table-column sortable prop="c_name" label="位号/名称" width="200"/>
                    <el-table-column sortable prop="c_person" label="作业人员" width="140"/>
                    <el-table-column prop="c_desc" label="故障描述" width="250"/>
                    <el-table-column prop="c_progress" label="维修过程"/>
                    <el-table-column sortable prop="c_result" label="维修结果" width="190"/>
                    <el-table-column prop="c_summary" label="技术小结" width="120"/>
                    <el-table-column prop="c_comment" label="班长批注"/>

                    <el-table-column fixed="left" label="" min-width="90" width="95">
                        <template #default="scope">
                            <el-button-group style="width: 100%;">
                                <el-button link type="primary" size="small" @click.prevent="onDeviceClicked(scope.row)">查看
                                </el-button>
                                <el-button link type="primary" size="small" @click.prevent="onDeleteClicked(scope.row)">删除
                                </el-button>
                            </el-button-group>
                        </template>
                    </el-table-column>
                </el-table>

                <el-pagination
                    size="small"
                    :page-size="PAGE_SIZE"
                    :hide-on-single-page="true"
                    layout="prev, pager, next"
                    :total="formTotal"
                    @current-change="onPageChanged"
                    :current-page="curPageIndex"
                />
            </div>
        </div>

        <el-dialog align-center v-model="showDialogue" title="新增记录" width="650" draggable modal center :close-on-click-modal="false">
            <el-form ref="formRef"
                     :model="deviceRecordInfo"
                     style="margin-left: 0" label-width="110px" label-position="left">
                <el-form-item label="位号/名称" prop="c_name">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_name"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="作业人员" prop="c_person">
                    <el-input clearable v-model="deviceRecordInfo.c_person" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="故障描述" prop="c_desc">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_desc"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="维修过程" prop="c_progress">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_progress"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="维修结果" prop="c_result">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_result" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="技术小结" prop="c_summary">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_summary" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="完成情况" prop="c_finish">
                    <el-switch
                        v-model="deviceRecordInfo.c_finish"
                        inline-prompt
                        class="ml-2"
                        style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
                        active-text="已完成"
                        inactive-text="未完成"
                    />
                </el-form-item>
                <el-form-item label="班长批注">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_comment" autocomplete="off"/>
                </el-form-item>
            </el-form>

            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="onSaveClicked">保存
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<style scoped>
.page-container {
    width: 100%;
    height: 100%;
    background-color: #F5F5F5;
    display: flex;
    flex-direction: column;

    .page-title {
        width: 100%;
        height: 40px;
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

        .page-menu-content {
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

.avatar-img {
    --img-size: 30px;
    --color_border: #c02942;
    --color_inner: #ecd078;
    --border-size: 3px;
    --scale-rate: 1;
    --max-scale-rate: 1.35;
    --bg-option: content-box no-repeat center / calc(100% / var(--scale-rate));
    --outline-offset: calc((1 / var(--scale-rate) - 1) * var(--img-size) / 2 - var(--border-size));

    transform: scale(var(--scale-rate));
    width: var(--img-size);
    height: var(--img-size);
    cursor: pointer;
    transition: 0.5s;

    background: radial-gradient(
        circle closest-side,
        var(--color_inner) calc(99% - var(--border-size)),
        var(--color_border) calc(100% - var(--border-size)) 99%,
        transparent
    ) var(--bg-option);

    outline: var(--border-size) solid var(--color_border);
    border-radius: 0 0 999px 999px;
    outline-offset: var(--outline-offset);
    padding-top: calc(var(--img-size) / 5);
    mask: linear-gradient(#000, #000) no-repeat 50% calc(10px - var(--outline-offset)) / calc(100% / var(--scale-rate) - 3 * var(--border-size)) 50%,
    radial-gradient(
        circle closest-side,
        #000 99%,
        transparent
    ) var(--bg-option);
}

.avatar-img:hover {
    --scale-rate: var(--max-scale-rate);
}

.el-menu {
    border-bottom: solid 1px #1480ec;

    .el-menu-item {
        font-size: 12px;
        font-weight: bolder;
        height: 32px;
        padding: 0 12px;
    }

    .el-submenu__title {
        font-size: 12px;
        font-weight: bolder;
        padding: 0 12px;
        height: 40px;
    }

    .el-sub-menu__title,
    .el-tooltip__trigger,
    .el-tooltip__trigger {
        font-size: 12px;
        font-weight: bolder;
        height: 40px;
    }
}

/* 使用::v-deep穿透scoped样式 */
.el-table .el-table__cell {
    font-size: 10px; /* 设置你想要的文字大小 */
}

/*https://element-plus.org/zh-CN/component/menu.html*/
.el-menu--horizontal {
    --el-menu-horizontal-height: 40px;
}

.el-menu--vertical {
    --el-menu-horizontal-height: 30px;
}

.el-table__body tr.current-row > td {
    background: #BDDBBB !important;
}

.el-pagination {
    justify-content: center;
}

.el-carousel__item h3 {
    color: #475669;
    opacity: 0.75;
    line-height: 150px;
    margin: 0;
    text-align: center;
}

.el-carousel__item:nth-child(2n) {
    background-color: #99a9bf;
}

.el-carousel__item:nth-child(2n + 1) {
    background-color: #d3dce6;
}

</style>