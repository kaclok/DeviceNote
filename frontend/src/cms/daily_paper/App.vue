<script setup>

import {SysX} from "@/cms/daily_paper/system/SysX.js";
import {Singleton} from "@/framework/services/Singleton.js";
import {ref, computed} from "vue";

const DEVICE_PAGE_SIZE = 18
const DEVICE_RECORD_PAGE_SIZE = 13
const PAPER_COUNT = 8

let AC_getGYList = new AbortController();
let AC_getDeviceList = new AbortController();
let AC_getDeviceRecordList = new AbortController();
let AC_getDeviceRecord = new AbortController();
let AC_report = new AbortController();

let loadingGYList = ref(false);
let loadingDeviceList = ref(false);
let loadingDeviceRecordList = ref(false);
let loadingDeviceRecord = ref(false);
let loadingReport = ref(false);

let curZZId = ref(-1);
let curGYId = ref(-1);
let curDeviceId = ref(-1);

let curDevicePageIndex = ref(1)
let curDeviceRecordPageIndex = ref(1)

const gyList = ref([])

const deviceList = ref([])
let deviceTotal = 0

const deviceRecordList = ref([])
let deviceRecordTotal = 0

const deviceRecordFormRef = ref(null)
const __info__ = {
    reset() {
        this.id = null
        this.gy_id = null
        this.device_id = null
        this.record_time = null
        this.c_person = null
        this.c_trouble_time = null
        this.c_trouble_xx = null
        this.c_trouble_yy = null
        this.c_fix_xm = null
        this.c_bjxh = null
        this.c_fix_jl = null
        this.c_fix_hs = null
        this.c_comment = null

        return this
    }
}

let deviceRecordInfo = ref(__info__)
// 1查看 2新建
let deviceRecordMode = ref(0)
let showDialogue = ref(false)
const deviceRecordFormRule = ref({
    c_person: [
        {required: true, trigger: 'blur'},
    ],
    c_trouble_time: [
        {
            required: true,
            type: 'datetime',
        },
    ],
    c_trouble_xx: [
        {
            required: true,
            trigger: 'blur'
        },
    ],
    c_trouble_yy: [
        {
            required: true,
            trigger: 'blur'
        },
    ],
    c_fix_xm: [
        {
            required: true,
            trigger: 'blur'
        },
    ],
    c_bjxh: [
        {
            required: true,
            trigger: 'blur'
        },
    ],
    c_fix_jl: [
        {
            required: true,
            trigger: 'blur'
        },
    ],
    c_fix_hs: [
        {
            required: true,
            trigger: 'blur'
        },
    ],
})

// 清理定时器，事件监听器，异步函数
onUnmounted(() => {
    AC_getGYList.abort();
    AC_getDeviceList.abort();
    AC_getDeviceRecordList.abort();
    AC_getDeviceRecord.abort();
    AC_report.abort();
});

function onMenuClicked(menuIndex) {
    curZZId.value = menuIndex

    curGYId.value = -1;
    curDeviceId.value = -1;

    deviceTotal = 0
    deviceRecordTotal = 0

    curDevicePageIndex.value = 1
    curDeviceRecordPageIndex.value = 1

    deviceRecordMode.value = 0
    showDialogue.value = false

    Singleton.getInstance(SysX).getGYListByZZ({zzId: curZZId.value}, AC_getGYList.signal, () => {
        loadingGYList.value = true;
    }, (r, data) => {
        loadingGYList.value = false;

        if (r) {
            gyList.value = data.data;
            deviceList.value = []
        }
    });
}

function onGYMenuClicked(menuIndex) {
    curGYId.value = menuIndex

    curDeviceId.value = -1;

    deviceTotal = 0
    curDevicePageIndex.value = 1

    curDeviceRecordPageIndex.value = 1;
    deviceRecordTotal = 0

    deviceRecordMode.value = 0
    showDialogue.value = false

    _getDeviceList(null, curDevicePageIndex.value);
}

function onDeviceClicked(row) {
    if (row) {
        curDeviceId.value = row.id;

        curDeviceRecordPageIndex.value = 1;
        deviceRecordTotal = 0

        deviceRecordMode.value = 0
        showDialogue.value = false

        _getDeviceRecordList(null, curDeviceRecordPageIndex.value);
    }
}

function onCurDevicePageChanged(curPageIndex) {
    if (curPageIndex !== curDevicePageIndex.value) {
        curDevicePageIndex.value = curPageIndex

        _getDeviceList(null, curPageIndex);
    }
}

function _getDeviceList(posIdx, pageNum) {
    Singleton.getInstance(SysX).getDeviceList({
        gyId: curGYId.value,
        posIdx: posIdx,
        pageNum: pageNum,
        pageSize: DEVICE_PAGE_SIZE
    }, AC_getDeviceList.signal, () => {
        loadingDeviceList.value = true;
    }, (r, data) => {
        loadingDeviceList.value = false;
        if (r) {
            deviceList.value = data.data.list
            deviceTotal = data.data.total
            deviceRecordList.value = []
        }
    });
}

function onCurDeviceRecordPageChanged(curPageIndex) {
    if (curPageIndex !== curDeviceRecordPageIndex.value) {
        curDeviceRecordPageIndex.value = curPageIndex

        _getDeviceRecordList(null, curPageIndex);
    }
}

function _getDeviceRecordList(person, pageNum) {
    Singleton.getInstance(SysX).getRecordList({
        deviceId: curDeviceId.value,
        person: person,
        pageNum: pageNum,
        pageSize: DEVICE_RECORD_PAGE_SIZE
    }, AC_getDeviceRecordList.signal, () => {
        loadingDeviceRecordList.value = true;
    }, (r, data) => {
        loadingDeviceRecordList.value = false;
        if (r) {
            deviceRecordList.value = data.data.list
            deviceRecordTotal = data.data.total
        }
    });
}

function onDeviceRecordClicked(row) {
    if (row) {
        deviceRecordMode.value = 1
        _getDeviceRecord(row)
    }
}

function _getDeviceRecord(row) {
    Singleton.getInstance(SysX).getRecord({
        recordId: row.id
    }, AC_getDeviceRecord.signal, () => {
        loadingDeviceRecord.value = true;
    }, (r, data) => {
        loadingDeviceRecord.value = false;
        if (r) {
            showDialogue.value = true
            if (data.data != null && data.data.length > 0) {
                deviceRecordInfo.value = data.data[0];
            }
        }
    });
}

function onAddDeviceRecordClicked() {
    deviceRecordMode.value = 2

    deviceRecordFormRef.value?.resetFields();
    deviceRecordInfo.value = __info__.reset()
    showDialogue.value = true
}

function onSaveClicked() {
    Singleton.getInstance(SysX).report({
        gyId: curGYId.value,
        deviceId: curDeviceId.value,
        info: JSON.stringify(deviceRecordInfo.value),
    }, AC_report.signal, () => {
        loadingReport.value = true;
    }, (r, data) => {
        loadingReport.value = false;
        showDialogue.value = false

        if (r) {
            _getDeviceRecordList(null, curDeviceRecordPageIndex.value);
        } else {
        }
    });
}

</script>

<template>
    <div class="page-container">
        <div class="page-title">
            <img src="@/assets/st_logo.png" class="page-logo-content" alt="">
            <span class="page-title-content">{{ "设备检修记录" }}</span>

            <div class="page-menu-content">
                <el-menu mode="horizontal" background-color="#1C4785" text-color="#DCDCDC" active-text-color="#ffffff"
                         @select="onMenuClicked">
                    <el-menu-item index="1">公辅装置</el-menu-item>
                    <el-menu-item index="2">热动力站装置</el-menu-item>
                    <el-menu-item index="3">烧碱装置</el-menu-item>
                    <el-sub-menu index="4">
                        <template #title>聚氯乙烯</template>
                        <el-menu-item index="4-1">聚合</el-menu-item>
                        <el-menu-item index="4-2">乙炔</el-menu-item>
                        <el-menu-item index="4-3">VCM</el-menu-item>
                        <el-menu-item index="4-4">固液分离</el-menu-item>
                    </el-sub-menu>
                </el-menu>
            </div>
        </div>

        <div class="page-content">
            <span v-if="curZZId === -1"
                  style="font-size: 24px; width: 350px; position: absolute; left: 42%; top: 50%; overflow-x: hidden; text-align: center">请先点击选择标题栏的->装置</span>

            <!--左侧-->
            <div v-loading="loadingGYList" v-if="curZZId !== -1" style=" width: 180px;  height: 100%; padding-left: 0;
                background-color: #1C4785;  overflow-y: auto;">

                <el-menu style="margin-left: -15px; height: 100%;"
                         mode="vertical" background-color="#1C4785" text-color="#DCDCDC" active-text-color="#ffffff"
                         @select="onGYMenuClicked">
                    <el-menu-item v-for="(gy, idx) in gyList"
                                  :index="gy.id">{{ !gy.d_j ? idx + 1 + ("*静*" + gy.name) : idx + 1 + ("*动*" + gy.name) }}
                    </el-menu-item>
                </el-menu>
            </div>

            <!--中侧-->
            <div v-loading="loadingDeviceList" v-if="curGYId !== -1"
                 style="width: calc((100% - 180px) / 2); padding-left: 0;">
                <!--https://element-plus.org/zh-CN/component/table.html#%E7%AD%9B%E9%80%89-->
                <el-table show-overflow-tooltip :data="deviceList" fit stripe border highlight-current-row @current-change="onDeviceClicked"
                          max-height="calc(100% - 40px)" height="calc(100% - 40px)">
                    <el-table-column sortable prop="idx" label="序号" width="80"/>
                    <el-table-column sortable prop="id" label="id" width="80"/>
                    <el-table-column sortable prop="name" label="名称"/>
                    <el-table-column sortable prop="pos_idx" label="位号" width="190"/>
                    <el-table-column sortable prop="install_location" label="安装位置" width="150"/>
                </el-table>

                <el-pagination
                    size="small"
                    :hide-on-single-page="true"
                    :page-size="DEVICE_PAGE_SIZE"
                    layout="prev, pager, next"
                    :total="deviceTotal"
                    @current-change="onCurDevicePageChanged"
                    :current-page="curDevicePageIndex"
                />
            </div>

            <!--右侧-->
            <div v-loading="loadingDeviceRecordList" v-if="curDeviceId !== -1"
                 style="padding-left: 0; width: calc((100% - 180px) / 2);">
                <el-button-group style="width: 100%;">
                    <el-button style="font-size: 15px; width: 100%; height: 40px" type="warning"
                               @click="onAddDeviceRecordClicked">新增检修记录
                    </el-button>
                </el-button-group>

                <el-table v-if="deviceRecordTotal > 0" show-overflow-tooltip :data="deviceRecordList" fit stripe border highlight-current-row
                          style="margin-top: 10px" max-height="calc(100% - 80px)" height="calc(100% - 80px)">
                    <el-table-column sortable prop="id" label="id" width="70"/>
                    <el-table-column sortable prop="c_person" label="检修人" width="120"/>
                    <el-table-column sortable prop="record_time" label="故障时间" width="200">
                        <template #default="scope1">
                            <el-date-picker
                                :disabled="true"
                                type="datetime"
                                class="item"
                                v-model="scope1.row.c_trouble_time">
                            </el-date-picker>
                        </template>
                    </el-table-column>
                    <el-table-column prop="c_trouble_xx" label="故障现象"/>
                    <el-table-column fixed="left" label="操作" min-width="55">
                        <template #default="scope">
                            <el-button link type="primary" size="small" @click.prevent="onDeviceRecordClicked(scope.row)">查看
                            </el-button>
                        </template>
                    </el-table-column>
                </el-table>

                <el-pagination v-if="deviceRecordTotal > 0"
                               size="small"
                               :hide-on-single-page="true"
                               :page-size="DEVICE_RECORD_PAGE_SIZE"
                               layout="prev, pager, next"
                               :total="deviceRecordTotal"
                               @current-change="onCurDeviceRecordPageChanged"
                               :current-page="curDeviceRecordPageIndex"
                />
            </div>
        </div>
        <el-dialog align-center v-model="showDialogue" title="检修记录" width="600" draggable modal center :close-on-click-modal="false">
            <el-form :rules="deviceRecordFormRule"
                     ref="deviceRecordFormRef"
                     :model="deviceRecordInfo"
                     style="margin-left: 0" label-width="110px" label-position="left">
                <el-form-item label="检修人" prop="c_person">
                    <el-input clearable :disabled="deviceRecordMode === 1" v-model="deviceRecordInfo.c_person" autocomplete="off"/>
                </el-form-item>
                <el-form-item v-if="deviceRecordMode === 1" label="检修时间" prop="record_time">
                    <el-date-picker
                        :disabled="deviceRecordMode === 1" clearable
                        type="datetime" class="item" v-model="deviceRecordInfo.record_time">
                    </el-date-picker>
                </el-form-item>
                <el-form-item label="故障时间" prop="c_trouble_time">
                    <el-date-picker clearable
                                    :disabled="deviceRecordMode === 1"
                                    type="datetime" class="item" v-model="deviceRecordInfo.c_trouble_time">
                    </el-date-picker>
                </el-form-item>
                <el-form-item label="故障现象" prop="c_trouble_xx">
                    <el-input clearable type="textarea" :disabled="deviceRecordMode === 1" v-model="deviceRecordInfo.c_trouble_xx"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="故障原因" prop="c_trouble_yy">
                    <el-input clearable type="textarea" :disabled="deviceRecordMode === 1" v-model="deviceRecordInfo.c_trouble_yy"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="维修项目" prop="c_fix_xm">
                    <el-input clearable type="textarea" :disabled="deviceRecordMode === 1" v-model="deviceRecordInfo.c_fix_xm" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="更换备件型号" prop="c_bjxh">
                    <el-input clearable type="textarea" :disabled="deviceRecordMode === 1" v-model="deviceRecordInfo.c_bjxh" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="维修结论" prop="c_fix_jl">
                    <el-input clearable type="textarea" :disabled="deviceRecordMode === 1" v-model="deviceRecordInfo.c_fix_jl" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="维修耗时" prop="c_fix_hs">
                    <el-input clearable type="textarea" :disabled="deviceRecordMode === 1" v-model="deviceRecordInfo.c_fix_hs" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input clearable type="textarea" :disabled="deviceRecordMode === 1" v-model="deviceRecordInfo.c_comment" autocomplete="off"/>
                </el-form-item>
            </el-form>

            <template #footer>
                <div class="dialog-footer">
                    <el-button v-if="deviceRecordMode === 2" @click="onSaveClicked">保存
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<style lang='scss'>
.page-container {
    width: 100%;
    height: 100%;
    background-color: #F5F5F5;
    display: flex;
    flex-direction: column;

    .page-title {
        width: 100%;
        height: 60px;
        display: flex;
        align-items: center;
        background-color: #1C4785;

        .page-logo-content {
            position: relative;
            left: 20px;

            width: 42px;
            height: 42px;
            object-fit: contain;
        }

        .page-title-content {
            min-width: 250px;
            color: #ffffff;
            font-size: 24px;
            font-weight: bolder;
            text-align: center;
        }

        .page-menu-content {
            position: relative;
            left: -60px;

            width: 850px;
            height: 100%;
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
        font-size: 16px;
        font-weight: bolder;
        padding: 0 12px;
    }

    .el-submenu__title {
        font-size: 16px;
        font-weight: bolder;
        padding: 0 12px;
    }

    .el-sub-menu__title,
    .el-tooltip__trigger,
    .el-tooltip__trigger {
        font-size: 16px;
        font-weight: bolder;
    }
}

.el-pagination {
    justify-content: center;
}

</style>