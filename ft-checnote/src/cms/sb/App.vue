<script setup>

import {SysX} from "@/cms/sb/system/SysX.js";
import {Singleton} from "@/framework/services/Singleton.js";
import {computed, ref} from "vue";
import {ElMessage} from "element-plus";
import {Search, Plus} from "@element-plus/icons-vue";
import dayjs from "dayjs";

// 时间显示：后端返回毫秒时间戳，统一格式化；已是字符串则原样展示
function formatTime(ts) {
    if (ts === null || ts === undefined || ts === "") return ""
    if (typeof ts === "number" || /^\d{10,}$/.test(String(ts))) {
        return dayjs(Number(ts)).format("YYYY-MM-DD HH:mm")
    }
    return String(ts)
}

// 移动端一页展示条数，避免列表过长
const DEVICE_PAGE_SIZE = 8
const DEVICE_RECORD_PAGE_SIZE = 5

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

// 装置列表（原横向菜单拍平，便于移动端横滑选择）
const zzList = [
    {id: "1", name: "公辅装置"},
    {id: "2", name: "热动力站装置"},
    {id: "3", name: "烧碱装置"},
    {id: "4-1", name: "聚合"},
    {id: "4-2", name: "乙炔"},
    {id: "4-3", name: "VCM"},
    {id: "4-4", name: "固液分离"},
];

const curZZId = ref(-1);
let curGYId = ref(-1);
let curDeviceId = ref(-1);
let curDeviceName = ref("");
const zzScrollRef = ref(null)

// 顶部装置栏：桌面端滚轮转横向滚动
function onZzWheel(e) {
    const el = zzScrollRef.value
    if (!el) return
    el.scrollLeft += e.deltaY || e.deltaX
}

let curDevicePageIndex = ref(1)
let curDeviceRecordPageIndex = ref(1)

const dj = ref(false)

const gyList = ref([])
const filteredGYList = computed(() => {
    return gyList.value.filter((item) => {
        return item.d_j === dj.value;
    });
})

const deviceList = ref([])
const deviceTotal = ref(0)
const deviceTotalPages = computed(() => Math.max(1, Math.ceil(deviceTotal.value / DEVICE_PAGE_SIZE)))
const devicePanelRef = ref(null)

// 设备关键字过滤（前端本地过滤，减少翻页查找成本）
const deviceKeyword = ref("")
const filteredDeviceList = computed(() => {
    const kw = deviceKeyword.value.trim();
    if (!kw) return deviceList.value;
    return deviceList.value.filter((d) =>
        (d.name || "").includes(kw) ||
        (d.pos_idx || "").includes(kw) ||
        (d.install_location || "").includes(kw)
    );
})

const deviceRecordList = ref([])
const deviceRecordTotal = ref(0)
const deviceRecordTotalPages = computed(() => Math.max(1, Math.ceil(deviceRecordTotal.value / DEVICE_RECORD_PAGE_SIZE)))

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
        this.c_fix_data = null
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
// 检修记录侧滑抽屉：选中设备后从右侧飞出盖住当前页
let showRecordDrawer = ref(false)

const deviceRecordRules = {
    c_person: [{required: true, message: "请填写检修人", trigger: "blur"}],
}

// 清理定时器，事件监听器，异步函数
onUnmounted(() => {
    AC_getGYList.abort();
    AC_getDeviceList.abort();
    AC_getDeviceRecordList.abort();
    AC_getDeviceRecord.abort();
    AC_report.abort();
});

function onMenuClicked(zzId) {
    if (curZZId.value === zzId) return
    curZZId.value = zzId

    curGYId.value = -1;
    curDeviceId.value = -1;
    curDeviceName.value = ""

    deviceTotal.value = 0
    deviceRecordTotal.value = 0

    curDevicePageIndex.value = 1
    curDeviceRecordPageIndex.value = 1

    deviceRecordMode.value = 0
    showDialogue.value = false
    showRecordDrawer.value = false

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

function onGYMenuClicked(gyId) {
    if (curGYId.value === gyId) return
    curGYId.value = gyId

    curDeviceId.value = -1;
    curDeviceName.value = ""
    showRecordDrawer.value = false

    deviceTotal.value = 0
    curDevicePageIndex.value = 1

    curDeviceRecordPageIndex.value = 1;
    deviceRecordTotal.value = 0

    deviceRecordMode.value = 0
    showDialogue.value = false

    _getDeviceList(null, curDevicePageIndex.value, false);
}

function onDeviceClicked(device) {
    if (!device) return
    curDeviceId.value = device.id;
    curDeviceName.value = device.name || ""

    curDeviceRecordPageIndex.value = 1;
    deviceRecordTotal.value = 0

    deviceRecordMode.value = 0
    showDialogue.value = false

    // 从右侧飞出记录页盖住当前页
    showRecordDrawer.value = true

    _getDeviceRecordList(null, curDeviceRecordPageIndex.value, false);
}

function onDevicePageChanged(delta) {
    const target = curDevicePageIndex.value + delta;
    if (target < 1 || target > deviceTotalPages.value) return

    _getDeviceList(null, target, false);
    scrollToPanel(devicePanelRef.value);
}

function _getDeviceList(posIdx, pageNum, append) {
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
            if (append) {
                deviceList.value = deviceList.value.concat(data.data.list)
                curDevicePageIndex.value = pageNum
            } else {
                deviceList.value = data.data.list
                curDevicePageIndex.value = pageNum || 1
            }

            // 换页/换单元时清空已选设备与记录
            curDeviceId.value = -1
            curDeviceName.value = ""
            showRecordDrawer.value = false
            deviceRecordTotal.value = 0
            deviceRecordList.value = []

            deviceTotal.value = data.data.total
        }
    });
}

function onDeviceRecordPageChanged(delta) {
    const target = curDeviceRecordPageIndex.value + delta;
    if (target < 1 || target > deviceRecordTotalPages.value) return

    _getDeviceRecordList(null, target, false);
}

function _getDeviceRecordList(person, pageNum, append) {
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
            if (append) {
                deviceRecordList.value = deviceRecordList.value.concat(data.data.list)
                curDeviceRecordPageIndex.value = pageNum
            } else {
                deviceRecordList.value = data.data.list
                curDeviceRecordPageIndex.value = pageNum || 1
            }

            deviceRecordTotal.value = data.data.total
        }
    });
}

// 翻页后把设备面板滚回可视区，避免手机上翻页后看不到列表
function scrollToPanel(el) {
    if (!el) return
    el.scrollIntoView({behavior: "smooth", block: "start"});
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

function onDialogueClosed() {
    showDialogue.value = false
}

function onSaveClicked() {
    deviceRecordFormRef.value?.validate((valid) => {
        if (!valid) return
        _doSaveReport()
    });
}

function _doSaveReport() {
    Singleton.getInstance(SysX).report({
        gyId: curGYId.value,
        deviceId: curDeviceId.value,
        info: JSON.stringify(deviceRecordInfo.value),
    }, AC_report.signal, () => {
        loadingReport.value = true;
    }, (r, data) => {
        loadingReport.value = false;

        if (r) {
            showDialogue.value = false
            ElMessage.success("保存成功");

            // 留在当前页刷新，避免新增后跳页丢失位置
            _getDeviceRecordList(null, curDeviceRecordPageIndex.value, false);
        } else {
            ElMessage.error("保存失败，请稍后重试");
        }
    });
}

</script>

<template>
    <div class="page-container">
        <!--顶部：标题 + 装置选择-->
        <header class="app-header">
            <div class="header-title">
                <img src="@/assets/st_logo.png" class="header-logo" alt="">
                <span class="header-text">设备检修记录</span>
            </div>
            <div ref="zzScrollRef" class="zz-scroll" @wheel.prevent="onZzWheel">
                <div v-for="zz in zzList" :key="zz.id"
                     class="zz-chip" :class="{active: curZZId === zz.id}"
                     @click="onMenuClicked(zz.id)">
                    {{ zz.name }}
                </div>
            </div>
        </header>

        <main class="page-body">
            <!--空状态：未选装置-->
            <div v-if="curZZId === -1" class="empty-state">
                <div class="empty-icon">🏭</div>
                <div class="empty-text">请先在顶部选择一个装置</div>
            </div>

            <template v-else>
                <!--工艺单元选择-->
                <section class="panel" v-loading="loadingGYList">
                    <div class="seg-switch">
                        <div class="seg-item" :class="{active: !dj}" @click="dj = false">静设备</div>
                        <div class="seg-item" :class="{active: dj}" @click="dj = true">动设备</div>
                    </div>

                    <!--工艺单元：自动换行铺开，条目再多也不需要滑动-->
                    <div v-if="filteredGYList.length > 0" class="gy-list">
                        <div v-for="gy in filteredGYList" :key="gy.id"
                             class="gy-chip" :class="{active: curGYId === gy.id}"
                             @click="onGYMenuClicked(gy.id)">
                            {{ gy.name }}
                        </div>
                    </div>
                    <div v-else class="empty-tip">该装置下暂无{{ dj ? "动" : "静" }}设备单元</div>
                </section>

                <!--设备列表-->
                <section v-if="curGYId !== -1" ref="devicePanelRef" class="panel" v-loading="loadingDeviceList">
                    <div class="panel-header">
                        <div class="panel-title">设备列表</div>
                        <div class="panel-count">共 {{ deviceTotal }} 台</div>
                    </div>

                    <el-input v-model="deviceKeyword" class="device-search"
                              placeholder="搜索本页名称 / 位号 / 位置" clearable :prefix-icon="Search"/>

                    <template v-if="filteredDeviceList.length > 0">
                        <div v-for="d in filteredDeviceList" :key="d.id"
                             class="device-card" :class="{active: curDeviceId === d.id}"
                             @click="onDeviceClicked(d)">
                            <div class="device-card-top">
                                <span class="device-name">{{ d.name }}</span>
                                <span class="device-tags">
                                    <span v-if="d.pos_idx" class="device-tag">{{ d.pos_idx }}</span>
                                    <span class="device-tag id">ID: {{ d.id }}</span>
                                </span>
                            </div>
                            <div class="device-card-bottom">
                                <span v-if="d.install_location" class="device-loc">📍 {{ d.install_location }}</span>
                            </div>
                        </div>
                    </template>
                    <div v-else-if="!loadingDeviceList" class="empty-tip">
                        {{ deviceKeyword ? "本页无匹配设备，可翻页或清空搜索" : "暂无设备数据" }}
                    </div>

                    <!--分页-->
                    <div v-if="deviceTotalPages > 1" class="pager">
                        <div class="pager-btn" :class="{disabled: curDevicePageIndex <= 1}"
                             @click="onDevicePageChanged(-1)">上一页
                        </div>
                        <div class="pager-info">
                            <span class="pager-cur">{{ curDevicePageIndex }}</span>
                            <span class="pager-sep">/</span>
                            <span class="pager-total">{{ deviceTotalPages }}</span>
                        </div>
                        <div class="pager-btn" :class="{disabled: curDevicePageIndex >= deviceTotalPages}"
                             @click="onDevicePageChanged(1)">下一页
                        </div>
                    </div>
                </section>
            </template>
        </main>

        <!--检修记录：选中设备后从右侧飞出，盖住当前页（竖屏形式）-->
        <el-drawer v-model="showRecordDrawer" direction="rtl" size="88%" modal
                   class="record-drawer" :close-on-click-modal="true">
            <template #title>
                <div class="drawer-title">
                    <span class="drawer-name">{{ curDeviceName }}</span>
                    <span class="drawer-count">{{ deviceRecordTotal }} 条记录</span>
                </div>
            </template>

            <div class="drawer-content" v-loading="loadingDeviceRecordList">
                <template v-if="deviceRecordList.length > 0">
                    <div v-for="r in deviceRecordList" :key="r.id"
                         class="record-card" @click="onDeviceRecordClicked(r)">
                        <div class="record-card-top">
                            <span class="record-person">
                                <span class="record-avatar">{{ (r.c_person || "?").slice(0, 1) }}</span>
                                {{ r.c_person }}
                            </span>
                            <span class="record-time">{{ formatTime(r.record_time || r.c_trouble_time) }}</span>
                        </div>
                        <div v-if="r.c_trouble_xx" class="record-desc">{{ r.c_trouble_xx }}</div>
                        <div class="record-link">查看详情 →</div>
                    </div>
                </template>
                <div v-else-if="!loadingDeviceRecordList" class="empty-tip">
                    暂无检修记录，点击右下角 + 新增
                </div>

                <!--分页：吸底常驻，滚动到任何位置都能翻页-->
                <div class="pager-sticky">
                    <div v-if="deviceRecordTotalPages > 1" class="pager">
                        <div class="pager-btn" :class="{disabled: curDeviceRecordPageIndex <= 1}"
                             @click="onDeviceRecordPageChanged(-1)">上一页
                        </div>
                        <div class="pager-info">
                            <span class="pager-cur">{{ curDeviceRecordPageIndex }}</span>
                            <span class="pager-sep">/</span>
                            <span class="pager-total">{{ deviceRecordTotalPages }}</span>
                        </div>
                        <div class="pager-btn" :class="{disabled: curDeviceRecordPageIndex >= deviceRecordTotalPages}"
                             @click="onDeviceRecordPageChanged(1)">下一页
                        </div>
                    </div>
                </div>
            </div>
        </el-drawer>

        <!--新增记录悬浮按钮-->
        <transition name="fab-fade">
            <div v-if="curDeviceId !== -1 && !showDialogue" class="fab" @click="onAddDeviceRecordClicked">
                <el-icon :size="26"><Plus/></el-icon>
            </div>
        </transition>

        <!--检修记录 弹窗（新增/查看均可编辑并保存）-->
        <el-dialog align-center v-model="showDialogue" class="record-dialog"
                   :title="deviceRecordMode === 2 ? '新增检修记录' : '检修记录详情'"
                   width="92%" modal :close-on-click-modal="false">
            <el-form ref="deviceRecordFormRef"
                     :model="deviceRecordInfo"
                     :rules="deviceRecordRules"
                     label-width="88px" label-position="left">
                <el-form-item label="检修人" prop="c_person">
                    <el-input clearable v-model="deviceRecordInfo.c_person" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="检修时间" prop="c_trouble_time">
                    <el-date-picker clearable
                                    type="datetime" class="item" style="width: 100%"
                                    v-model="deviceRecordInfo.c_trouble_time"/>
                </el-form-item>
                <el-form-item label="故障现象" prop="c_trouble_xx">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_trouble_xx"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="故障原因" prop="c_trouble_yy">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_trouble_yy"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="检修内容" prop="c_fix_xm">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_fix_xm"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="更换备件型号" prop="c_bjxh">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_bjxh"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="检测数据" prop="c_fix_data">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_fix_data"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="维修结论" prop="c_fix_jl">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_fix_jl"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="维修耗时" prop="c_fix_hs">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_fix_hs"
                              autocomplete="off"/>
                </el-form-item>
                <el-form-item label="备注">
                    <el-input clearable type="textarea" autosize v-model="deviceRecordInfo.c_comment"
                              autocomplete="off"/>
                </el-form-item>
            </el-form>

            <template #footer>
                <div class="dialog-footer">
                    <el-button @click="onDialogueClosed">取消</el-button>
                    <el-button type="primary" :loading="loadingReport"
                               @click="onSaveClicked">保存
                    </el-button>
                </div>
            </template>
        </el-dialog>
    </div>
</template>

<style lang='scss' scoped>
$brand: #1C4785;
$brand-light: #2E6BC0;
$bg: #F2F5FA;

.page-container {
    width: 100%;
    max-width: 720px;
    margin: 0 auto;
    height: 100vh;
    height: 100dvh;
    background-color: $bg;
    display: flex;
    flex-direction: column;
    overflow: hidden;
}

/* ===== 顶部 ===== */
.app-header {
    flex-shrink: 0;
    background: linear-gradient(135deg, $brand 0%, $brand-light 100%);
    padding-top: env(safe-area-inset-top);
    padding-bottom: 10px;
    box-shadow: 0 2px 8px rgba(28, 71, 133, 0.25);
    z-index: 10;

    .header-title {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px 14px 6px;

        .header-logo {
            width: 26px;
            height: 26px;
            object-fit: contain;
            border-radius: 6px;
            background: rgba(255, 255, 255, 0.92);
            padding: 2px;
        }

        .header-text {
            color: #ffffff;
            font-size: 16px;
            font-weight: 600;
            letter-spacing: 1px;
        }
    }

    .zz-scroll {
        display: flex;
        gap: 8px;
        overflow-x: auto;
        padding: 4px 14px 4px;
        -webkit-overflow-scrolling: touch;
        scrollbar-width: thin;
        scrollbar-color: rgba(255, 255, 255, 0.45) transparent;

        &::-webkit-scrollbar {
            height: 3px;
        }

        &::-webkit-scrollbar-thumb {
            background: rgba(255, 255, 255, 0.45);
            border-radius: 3px;
        }

        &::-webkit-scrollbar-track {
            background: transparent;
        }

        .zz-chip {
            flex-shrink: 0;
            padding: 6px 12px;
            border-radius: 15px;
            font-size: 12px;
            line-height: 1.2;
            color: rgba(255, 255, 255, 0.85);
            background: rgba(255, 255, 255, 0.14);
            border: 1px solid transparent;
            transition: all 0.2s;
            cursor: pointer;
            touch-action: manipulation;
            user-select: none;

            &.active {
                background: #ffffff;
                color: $brand;
                font-weight: 600;
                border-color: #ffffff;
            }
        }
    }
}

/* ===== 主体 ===== */
.page-body {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
    overscroll-behavior: contain;
    padding: 10px 10px calc(84px + env(safe-area-inset-bottom));
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.panel {
    background: #ffffff;
    border-radius: 12px;
    padding: 10px;
    box-shadow: 0 1px 4px rgba(30, 60, 110, 0.06);
}

.panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;

    .panel-title {
        font-size: 13px;
        font-weight: 600;
        color: #1f2d3d;

        .panel-sub {
            font-size: 11px;
            font-weight: 400;
            color: #8492a6;
        }
    }

    .panel-count {
        font-size: 11px;
        color: #8492a6;
    }
}

/* 动/静设备 分段切换 */
.seg-switch {
    display: flex;
    background: #EEF2F8;
    border-radius: 10px;
    padding: 3px;
    margin-bottom: 10px;

    .seg-item {
        flex: 1;
        text-align: center;
        padding: 8px 0;
        font-size: 13px;
        color: #5a6a85;
        border-radius: 8px;
        transition: all 0.2s;
        cursor: pointer;
        touch-action: manipulation;
        user-select: none;

        &.active {
            background: #ffffff;
            color: $brand;
            font-weight: 600;
            box-shadow: 0 1px 3px rgba(30, 60, 110, 0.12);
        }
    }
}

/* 工艺单元：换行布局，全部可见无需滑动 */
.gy-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .gy-chip {
        padding: 7px 12px;
        border-radius: 7px;
        font-size: 12px;
        line-height: 1.2;
        color: #4a5a75;
        background: #F0F4FA;
        border: 1px solid #E2E9F3;
        transition: all 0.2s;
        cursor: pointer;
        touch-action: manipulation;
        user-select: none;
        white-space: nowrap;
        max-width: 100%;
        overflow: hidden;
        text-overflow: ellipsis;

        &.active {
            background: $brand;
            color: #ffffff;
            border-color: $brand;
            font-weight: 600;
        }
    }
}

/* 设备搜索 */
.device-search {
    margin-bottom: 10px;

    :deep(.el-input__wrapper) {
        border-radius: 8px;
        background: #F5F8FC;
        box-shadow: none;
    }
}

/* 设备卡片 */
.device-card {
    border: 1px solid #E8EEF6;
    border-radius: 10px;
    padding: 10px 12px;
    margin-bottom: 10px;
    transition: all 0.15s;
    cursor: pointer;
    touch-action: manipulation;
    -webkit-tap-highlight-color: transparent;

    &:active {
        transform: scale(0.99);
    }

    &.active {
        border-color: $brand;
        background: #F4F8FF;
    }

    .device-card-top {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 8px;

        .device-name {
            font-size: 13px;
            font-weight: 600;
            color: #1f2d3d;
            line-height: 1.4;
        }

        .device-tags {
            display: flex;
            align-items: center;
            gap: 4px;
            flex-shrink: 0;
        }

        .device-tag {
            flex-shrink: 0;
            font-size: 11px;
            color: $brand;
            background: #EDF3FC;
            border-radius: 4px;
            padding: 1px 6px;
            font-family: monospace;

            &.id {
                color: #5a6a85;
                background: #F0F4FA;
            }
        }
    }

    .device-card-bottom {
        margin-top: 3px;

        .device-loc {
            font-size: 11px;
            color: #8492a6;
        }
    }
}

/* 记录卡片 */
.record-card {
    border: 1px solid #E8EEF6;
    border-radius: 10px;
    padding: 10px 12px;
    margin-bottom: 10px;
    cursor: pointer;
    transition: all 0.15s;
    touch-action: manipulation;
    -webkit-tap-highlight-color: transparent;

    &:active {
        transform: scale(0.99);
    }

    .record-card-top {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: 8px;

        .record-person {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 13px;
            font-weight: 600;
            color: #1f2d3d;
            line-height: 1.4;
            min-width: 0;
            word-break: break-all;

            .record-avatar {
                flex-shrink: 0;
                width: 22px;
                height: 22px;
                border-radius: 50%;
                background: linear-gradient(135deg, $brand, $brand-light);
                color: #ffffff;
                font-size: 11px;
                display: flex;
                align-items: center;
                justify-content: center;
            }
        }

        .record-time {
            flex-shrink: 0;
            font-size: 11px;
            color: #8492a6;
            padding-top: 2px;
            font-variant-numeric: tabular-nums;
        }
    }

    .record-desc {
        margin-top: 6px;
        font-size: 12px;
        line-height: 1.5;
        color: #5a6a85;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
    }

    .record-link {
        margin-top: 6px;
        font-size: 11px;
        color: $brand-light;
    }
}

/* 抽屉标题（#title 插槽内容在本组件作用域内） */
.drawer-title {
    display: flex;
    flex-direction: column;
    gap: 2px;

    .drawer-name {
        font-size: 14px;
        font-weight: 600;
        color: #1f2d3d;
    }

    .drawer-count {
        font-size: 11px;
        color: #8492a6;
    }
}

/* 分页 */
.pager {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-top: 12px;
    padding-top: 10px;
    border-top: 1px dashed #E8EEF6;

    .pager-btn {
        flex: 0 0 auto;
        padding: 8px 14px;
        font-size: 12px;
        color: $brand;
        background: #F0F4FA;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.15s;
        user-select: none;
        touch-action: manipulation;

        &:active {
            transform: scale(0.96);
        }

        &.disabled {
            color: #b6c1d1;
            background: #F6F8FC;
            pointer-events: none;
        }
    }

    .pager-info {
        flex: 1;
        text-align: center;
        font-size: 12px;
        color: #8492a6;

        .pager-cur {
            color: $brand;
            font-weight: 600;
            font-size: 13px;
        }

        .pager-sep {
            margin: 0 4px;
            color: #c6d0e0;
        }
    }
}

/* 抽屉内分页吸底：滚动到哪里都能翻页 */
.pager-sticky {
    position: sticky;
    bottom: calc(78px + env(safe-area-inset-bottom)); /* 悬浮在 FAB 上方 */
    margin: 10px -10px 0;
    padding: 8px 10px;
    background: rgba(242, 245, 250, 0.96);
    border-top: 1px dashed #E8EEF6;
}

/* 空状态 */
.empty-state {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 10px;

    .empty-icon {
        font-size: 52px;
    }

    .empty-text {
        font-size: 13px;
        color: #8492a6;
    }
}

.empty-tip {
    padding: 12px 0;
    text-align: center;
    font-size: 12px;
    color: #a0aec0;
}

/* 悬浮新增按钮 */
.fab {
    position: fixed;
    right: calc(50% - 360px + 20px);
    bottom: calc(28px + env(safe-area-inset-bottom));
    width: 54px;
    height: 54px;
    border-radius: 50%;
    background: linear-gradient(135deg, $brand 0%, $brand-light 100%);
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 6px 16px rgba(28, 71, 133, 0.4);
    cursor: pointer;
    z-index: 3000; /* 高于记录抽屉遮罩，抽屉打开时也可点 + 新增 */

    @media (max-width: 720px) {
        right: 20px;
    }

    &:active {
        transform: scale(0.92);
    }
}

.fab-fade-enter-active,
.fab-fade-leave-active {
    transition: all 0.25s;
}

.fab-fade-enter-from,
.fab-fade-leave-to {
    opacity: 0;
    transform: translateY(20px) scale(0.8);
}

/* 弹窗底部按钮 */
.dialog-footer {
    display: flex;
    justify-content: center;
    gap: 12px;

    .el-button {
        min-width: 100px;
        height: 38px;
        font-size: 13px;
    }
}
</style>

<!-- 移动端全局基线：本页是独立入口（src/cms/sb/main.js 未引全局 reset），此处补齐 -->
<style lang="scss">
html,
body,
#app {
    width: 100%;
    height: 100%;
    margin: 0;
    padding: 0;
}

body {
    overflow: hidden; /* 由页面内部面板负责滚动，避免双滚动条 */
    -webkit-text-size-adjust: 100%;
    -webkit-tap-highlight-color: transparent;
    overscroll-behavior: none;
    background: #F2F5FA;
    font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Arial, sans-serif;
}

/* 输入框字号与页面文本统一（viewport 已禁用缩放，无需再锁 16px 防 iOS 聚焦放大） */
@media (max-width: 768px) {
    .el-input__inner,
    .el-textarea__inner {
        font-size: 13px;
    }
}

/* 弹窗：与页面整体风格统一（同圆角/同浅底输入框/品牌色按钮），内容区内部滚动 */
.record-dialog {
    border-radius: 14px;
    overflow: hidden;
    max-width: 560px;

    .el-dialog__header {
        padding: 14px 16px 12px;
        margin-right: 0;
        text-align: center;
        border-bottom: 1px solid #F0F4FA;

        .el-dialog__title {
            font-size: 14px;
            font-weight: 600;
            color: #1f2d3d;
        }
    }

    .el-dialog__body {
        max-height: 62vh;
        overflow-y: auto;
        -webkit-overflow-scrolling: touch;
        overscroll-behavior: contain;
        padding: 14px 14px 8px;
        box-sizing: border-box;
        background: #F7FAFD;

        &::-webkit-scrollbar {
            width: 0;
        }
    }

    .el-dialog__footer {
        padding: 10px 16px calc(12px + env(safe-area-inset-bottom));
        border-top: 1px solid #F0F4FA;
    }

    .el-form-item {
        margin-bottom: 12px;
    }

    .el-form-item__label {
        font-size: 12px;
        color: #8492a6;
        line-height: 34px;
        padding-right: 8px;
    }

    .el-form-item__error {
        font-size: 11px;
        padding-top: 2px;
    }

    /* 输入框统一浅底、圆角、品牌色聚焦 */
    .el-textarea__inner,
    .el-input__wrapper {
        border-radius: 8px;
    }

    .el-textarea__inner {
        line-height: 1.5;
        padding: 8px 10px;
        font-size: 13px;
        background: #ffffff;
        box-shadow: 0 0 0 1px #E2E9F3 inset;

        &:focus {
            box-shadow: 0 0 0 1px #1C4785 inset;
        }
    }

    .el-input__wrapper {
        background: #ffffff;
        box-shadow: 0 0 0 1px #E2E9F3 inset;
        padding: 3px 10px;

        .el-input__inner {
            font-size: 13px;
            height: 30px;
            line-height: 30px;
        }

        &.is-focus {
            background: #ffffff;
            box-shadow: 0 0 0 1px #1C4785 inset;
        }
    }

    /* 底部按钮与整体配色一致 */
    .el-button--primary {
        background: #1C4785;
        border-color: #1C4785;

        &:hover,
        &:focus {
            background: #2E6BC0;
            border-color: #2E6BC0;
        }
    }

    .el-button {
        border-radius: 8px;
    }
}

/* 检修记录抽屉：从右侧飞出盖住当前页 */
.record-drawer {
    .el-drawer__header {
        margin-bottom: 0;
        padding: 14px 16px 10px;
        border-bottom: 1px solid #E8EEF6;
    }

    .el-drawer__body {
        padding: 10px 10px calc(140px + env(safe-area-inset-bottom));
        overflow-y: auto;
        -webkit-overflow-scrolling: touch;
        overscroll-behavior: contain;
        background: #F2F5FA;
    }
}

/* 小屏上时间选择面板不超出视口 */
@media (max-width: 480px) {
    .el-picker-panel.el-date-picker {
        max-width: 96vw;
        box-sizing: border-box;
    }

    .record-dialog {
        width: 92vw !important;
        margin: 0 auto;
    }
}
</style>
