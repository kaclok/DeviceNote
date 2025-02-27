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
let AC_export = new AbortController();

let loadingList = ref(false);
let loadingAdd = ref(false);
let loadingExport = ref(false)

let curBJId = ref("1");
let curDate = ref(Date.now());
let dateRange = ref('');

let curPageIndex = ref(1)

const formList = ref([])
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

function onSaveClicked() {
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

</script>

<template>
    <div class="page-container">
        <div class="page-title">
            <img src="@/assets/st_logo.png" class="page-logo-content" alt="">
            <span class="page-title-content">{{ "仪表检修记录" }}</span>

            <div class="page-menu-content">
                <el-menu mode="horizontal" background-color="#1C4785" text-color="#DCDCDC" active-text-color="#ffffff"
                         :default-active="curBJId"
                         @select="onMenuClicked">
                    <el-menu-item index="1">仪表一班</el-menu-item>
                    <el-menu-item index="2">仪表二班</el-menu-item>
                </el-menu>
            </div>
        </div>

        <div>
            <el-date-picker style="width: 120px; height: 40px; padding-top: 10px"
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

            <el-date-picker style="position: absolute; right: 60px; top: 50px"
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

            <el-button @click="onExportAll" circle :dark="true" type="warning" style="position: absolute; right: 20px; top: 45px">导出
            </el-button>
        </div>

        <div class="page-content">
            <!--中侧-->
            <div v-loading="loadingList"
                 style="width: 100%; padding-left: 0; padding-top: 10px">
                <!--https://element-plus.org/zh-CN/component/table.html#%E7%AD%9B%E9%80%89-->
                <el-button-group style="width: 100%;">
                    <el-button style="font-size: 12px; width: 100%; height: 30px" type="warning"
                               @click="onAddClicked">{{ "新增一条检修记录" }}
                    </el-button>
                </el-button-group>

                <el-table show-overflow-tooltip :data="formList" fit stripe border highlight-current-row max-height="100%">
                    <el-table-column sortable fixed type="index" label="序号" width="75"/>
                    <el-table-column sortable prop="c_name" label="名称" width="200"/>
                    <el-table-column sortable prop="c_person" label="作业人员" width="140"/>
                    <el-table-column prop="c_desc" label="故障描述" width="250"/>
                    <el-table-column prop="c_progress" label="维修过程"/>
                    <el-table-column sortable prop="c_result" label="维修结果" width="190"/>
                    <el-table-column prop="c_summary" label="技术小结" width="120"/>
                    <el-table-column prop="c_comment" label="备注" width="120"/>

                    <el-table-column fixed="left" label="" min-width="45" width="70">
                        <template #default="scope">
                            <el-button link type="primary" size="small" @click.prevent="onDeviceClicked(scope.row)">查看
                            </el-button>
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
                <el-form-item label="备注">
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

<style lang='scss' scoped>
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
::v-deep .el-table .el-table__cell {
    font-size: 10px; /* 设置你想要的文字大小 */
}

/*https://element-plus.org/zh-CN/component/menu.html*/
.el-menu--horizontal {
    --el-menu-horizontal-height: 40px;
}

.el-menu--vertical {
    --el-menu-horizontal-height: 30px;
}

::v-deep .el-table__body tr.current-row > td {
    background: #BDDBBB !important;
}

.el-pagination {
    justify-content: center;
}

</style>