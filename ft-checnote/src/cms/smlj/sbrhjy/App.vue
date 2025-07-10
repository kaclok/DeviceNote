<template>
    <div class="page-container">
        <div class="page-title">
            <img src="@/assets/st_logo.png" class="page-logo-content" alt="">
            <span class="page-title-content">{{ "设备润滑加油" }}</span>

            <div class="left-menu">
                <el-menu mode="horizontal" background-color="#1C4785" text-color="#DCDCDC" active-text-color="#ffffff" unique-opened
                         :default-active="curzzId"
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
            </div>
        </div>

        <div class="page-content">
            <!--中侧-->
            <div v-loading="loadingList" style="width: 100%; margin-top: 5px; margin-left: 5px" class="flex flex-wrap gap-4 items-center">
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

                <el-input
                    v-model="filterName"
                    style="width: 300px"
                    placeholder="设备名称(可不填)"
                    clearable
                />

                <el-input
                    v-model="filterWeihao"
                    style="position: relative; left: 2px; width: 300px"
                    placeholder="设备位号(可不填)"
                    clearable
                />
                <el-button v-cd="1.3" type="primary" style="position: relative; left: 8px;"
                           @click="onClearClicked">清除
                </el-button>
                <el-button v-cd="1.3" type="primary" style="position: relative; left: 0;"
                           @click="onSearchClicked">搜索
                </el-button>

                <el-main>
                    <el-table :data="tableData" show-overflow-tooltip fit stripe border highlight-current-row>
                        <el-table-column fixed type="index" :index="indexMethod" label="序号" width="65"/>
                        <el-table-column fixed="left" label="" min-width="90" width="65">
                            <template #default="scope">
                                <el-button-group style="width: 100%;">
                                    <el-button link type="primary" size="small" @click.prevent="onJYClicked(scope.row)">
                                        润滑
                                    </el-button>
                                </el-button-group>
                            </template>
                        </el-table-column>
                        <el-table-column prop="name" label="设备名称" width="200"/>
                        <el-table-column prop="weihao" label="设备位号" width="200"/>
                        <el-table-column prop="size" label="设备型号" width="200"/>
                        <el-table-column prop="prev_fix_a_time" label="上次轴伸端时间" width="180">
                            <template #default="scope1">
                                <el-date-picker
                                    format="YYYY-MM-DD HH"
                                    :disabled="true"
                                    type="datetime"
                                    class="item"
                                    v-model="scope1.row.prev_fix_a_time">
                                </el-date-picker>
                            </template>
                        </el-table-column>
                        <el-table-column prop="a_duration" label="下次轴伸端时间" width="180" sortable>
                            <template #default="scope2">
                                <el-badge is-dot :offset="[-220, 5]" :hidden="getExpiredStyle(scope2.row.prev_fix_a_time, scope2.row.a_duration) === 'primary'"
                                          :type="getExpiredStyle(scope2.row.prev_fix_a_time, scope2.row.a_duration)"
                                >
                                    <el-date-picker
                                        format="YYYY-MM-DD HH"
                                        :disabled="true"
                                        type="datetime"
                                        class="item"
                                        :model-value="calcNextTime(scope2.row.prev_fix_a_time, scope2.row.a_duration)">
                                    </el-date-picker>
                                </el-badge>
                            </template>
                        </el-table-column>
                        <el-table-column prop="prev_fix_b_time" label="上次非轴伸端时间" width="180">
                            <template #default="scope3">
                                <el-date-picker
                                    format="YYYY-MM-DD HH"
                                    :disabled="true"
                                    type="datetime"
                                    class="item"
                                    v-model="scope3.row.prev_fix_b_time">
                                </el-date-picker>
                            </template>
                        </el-table-column>
                        <el-table-column prop="b_duration" label="下次轴伸端时间" width="180" sortable>
                            <template #default="scope4">
                                <el-badge is-dot :offset="[-220, 5]" :hidden="getExpiredStyle(scope4.row.prev_fix_b_time, scope4.row.b_duration) === 'primary'"
                                          :type="getExpiredStyle(scope4.row.prev_fix_b_time, scope4.row.b_duration)"
                                >
                                    <el-date-picker
                                        format="YYYY-MM-DD HH"
                                        :disabled="true"
                                        type="datetime"
                                        class="item"
                                        :model-value="calcNextTime(scope4.row.prev_fix_b_time, scope4.row.b_duration)">
                                    </el-date-picker>
                                </el-badge>
                            </template>
                        </el-table-column>
                    </el-table>

                    <el-pagination
                        v-if="tableTotal > PAGE_SIZE"
                        size="small"
                        :page-size="PAGE_SIZE"
                        layout="prev, pager, next"
                        :total="tableTotal"
                        @current-change="onCurPageChanged"
                        :current-page="curPageIndex"
                    />

                    <el-dialog align-center v-model="showDialogue" title="" width="250" draggable modal center
                               :close-on-click-modal="false">
                        <!--                        <el-input
                                                    style="width: 450px"
                                                    placeholder="请输入轴伸端加油者"
                                                    clearable type="textarea" autosize v-model="deviceRecordInfo.c_a_person"
                                                    autocomplete="off"/>-->
                        <el-button @click="onASaveClicked">给轴伸端加油</el-button>

                        <div style="margin-top: 30px">
                            <!--                            <el-input
                                                            style="width: 450px"
                                                            placeholder="请输入非轴伸端加油者"
                                                            clearable type="textarea" autosize v-model="deviceRecordInfo.c_a_person"
                                                            autocomplete="off"/>-->
                            <el-button @click="onBSaveClicked">给非轴伸端加油</el-button>
                        </div>
                    </el-dialog>
                </el-main>
            </div>
        </div>
    </div>
</template>

<script lang="js" setup>
import {onMounted, onUnmounted, ref} from "vue";
import {doGet} from "@/framework/services/net/Request.js"
import {DateTimeUtil} from "@/framework/utils/DateTimeUtil.js";
import {EnumUtil} from "@/framework/utils/EnumUtil.js";

const PAGE_SIZE = 16

const __info__ = {
    reset() {
        this.c_a_person = null
        this.c_b_person = null
        return this
    }
}

const EExpireType = EnumUtil.asEnum({
    Normal: 0,
    WillExpired: 1,
    Expired: 2,
})

let deviceRecordInfo = ref(__info__)

const curzzId = ref("1")       // 菜单默认选中第一项
const curLevelId = ref("1")      // 下拉框默认选中高压电机

const filterName = ref(null)
const filterWeihao = ref(null)

let AC_getList = new AbortController();
let AC_save = new AbortController();
let AC_update = new AbortController();

let loadingList = ref(false);
let loadingSave = ref(false);

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
let showDialogue = ref(false)

function calcNextTime(begin, duration) {
    return (begin + duration * 3600000);
}

onMounted(() => {
    _reqList();
});

onUnmounted(() => {
    AC_getList.abort();
    AC_save.abort();
    AC_update.abort();
})

function indexMethod(index) {
    return (curPageIndex.value - 1) * PAGE_SIZE + index + 1;
}

function onClearClicked() {
    filterName.value = null
    filterWeihao.value = null

    // 请求服务器最新的列表数据
    _reqList();
}

function onSearchClicked() {
    // 请求服务器最新的列表数据
    _reqList();
}

function onJYClicked(row) {
    if (row) {
        showDialogue.value = true
        deviceRecordInfo.value = JSON.parse(JSON.stringify(row));
    }
}

function getExpiredType(begin, duration) {
    let next = begin + duration * 3600000
    let now = DateTimeUtil.nowMSTimestamp()
    /*console.error(next + "|" + now)*/

    if (next < now) { // 过期
        return EExpireType.Expired
    } else if (next - now < 5 * 24 * 3600000) { // x天之内
        return EExpireType.WillExpired
    }
    return EExpireType.Normal
}

function getExpiredStyle(begin, duration) {
    let type = getExpiredType(begin, duration);
    if (type === EExpireType.WillExpired) {
        return "warning"
    } else if (type === EExpireType.Expired) {
        return "danger"
    }
    return "primary"
}

function onASaveClicked() {
    let c = deviceRecordInfo.value;
    /*if (v.c_a_person === null) {
        return;
    }*/

    _save(c.id, 1, c.c_a_person, c.c_b_person);
}

function onBSaveClicked() {
    let c = deviceRecordInfo.value;
    /*if (v.c_b_person === null) {
        return;
    }*/

    _save(c.id, 2, c.c_a_person, c.c_b_person);
}

function _save(device_id, type, a_person, b_person) {
    doGet("x/sbrhjy/save", {
        device_id: device_id,
        c_a_person: a_person,
        c_b_person: b_person,
        type: type,
    }, AC_save.signal, () => {
        loadingSave.value = true;
    }, (r, data) => {
        loadingSave.value = false;

        if (r) {
            _reqList();
        } else {

        }
    })
}

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

function onCurPageChanged(targetIndex) {
    if (targetIndex !== curPageIndex.value) {
        curPageIndex.value = targetIndex

        _reqList();
    }
}

function _reqList() {
    doGet("x/sbrhjy/getList", {
        zzId: curzzId.value,
        levelId: curLevelId.value,
        filterByName: filterName.value,
        filterByWeihao: filterWeihao.value,
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

.el-main {
    padding-top: 5px;
    padding-left: 5px;
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

/*https://element-plus.org/zh-CN/component/menu.html*/
.el-menu--horizontal {
    --el-menu-horizontal-height: 40px;
}

.el-menu--vertical {
    --el-menu-horizontal-height: 30px;
}
</style>