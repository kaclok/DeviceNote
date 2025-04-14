<script setup lang="js">

import {ref, onUnmounted, onMounted} from "vue";
import {doGet, doPost} from "@/framework/services/net/Request.js"

const PAGE_SIZE = 19

let AC_getList = new AbortController();
let AC_cover = new AbortController();

let loadingList = ref(false);
let loadingCover = ref(false);

let curPageIndex = ref(1)

const formList = ref([
    /* {
         id: 1,
         c_name: "扫帚",
         c_number: 3,
         c_unit: "套",
         c_declarer: "数字化中心",
         c_model: "SHK-ZLB-10.5/3150-40-16%",
         c_ht: "SMLJ-CG-ZJGC-ZK-2024-04-044"
     },*/
])
let formTotal = 0

const filterName = ref(null)
const filterModel = ref(null)
const filterDeclarer = ref(null)
const filterHt = ref(null)

let params = new URLSearchParams(window.location.search);
let managerCode = params.get('is-manager');
let groupCode = params.get('group');
groupCode = groupCode === null ? "1" : groupCode.toString();
let curGroupIndex = ref(groupCode);

// 清理定时器，事件监听器，异步函数
onUnmounted(() => {
    AC_getList.abort();
    AC_cover.abort();
});

onMounted(() => {
    onMenuClicked(curGroupIndex.value);
});


function indexMethod(index) {
    return (curPageIndex.value - 1) * PAGE_SIZE + index + 1;
}

function onPageChanged(pageIndex) {
    if (pageIndex !== curPageIndex.value) {
        curPageIndex.value = pageIndex

        _reqList()
    }
}

function onClearClicked() {
    filterName.value = null
    filterModel.value = null
    filterDeclarer.value = null
    filterHt.value = null

    onSearchClicked()
}

function onSearchClicked() {
    _reqList()
}

const fileList = ref(null)

function onFileChange() {
    const fs = fileList.value.files
    _reqCover(fs)
    // 解决第二次选择同样的文件时不调用的问题
    fileList.value.value = null
}

function onCoverClicked() {
    fileList.value.click();
}

function onMenuClicked(menuIndex) {
    curGroupIndex.value = menuIndex

    formTotal = 0
    curPageIndex.value = 1

    _reqList();
}

function _reqList() {
    doGet("x/wz/getList", {
        group: curGroupIndex.value,
        filterByName: filterName.value,
        filterByModel: filterModel.value,
        filterByDeclarer: filterDeclarer.value,
        filterByHt: filterHt.value,
        pageNum: curPageIndex.value,
        pageSize: PAGE_SIZE
    }, AC_getList.signal, () => {
        loadingList.value = true;
    }, (r, data) => {
        loadingList.value = false;

        if (r) {
            formList.value = data.data.list
            formTotal = data.data.total
        } else {
        }
    })
}

function _reqCover(fs) {
    const formData = new FormData()
    formData.append("group", curGroupIndex.value)
    for (let f of fs) {
        formData.append('files', f)
    }

    doPost("x/wz/cover", formData, AC_cover, () => {
        loadingCover.value = true;
    }, (r, data) => {
        loadingCover.value = false;

        let prompt = "更新成功"
        if (!r) {
            if (data.message) {
                prompt = "更新失败:" + data.message
            } else {
                prompt = "更新失败"
            }
        } else {
            _reqList()
        }

        window.alert(prompt)
    })
}

</script>

<template>
    <div class="page-container">
        <div class="page-title">
            <img src="@/assets/st_logo.png" class="page-logo-content" alt="">
            <span class="page-title-content">{{ "物资库存记录" }}</span>

            <div class="page-menu-content">
                <el-menu mode="horizontal" background-color="#1C4785" text-color="#DCDCDC" active-text-color="#ffffff"
                         :default-active="curGroupIndex"
                         @select="onMenuClicked">
                    <el-menu-item index="1">神木氯碱</el-menu-item>
                    <el-menu-item index="2">神木电石</el-menu-item>
                    <el-menu-item index="3">米脂氯碱</el-menu-item>
                </el-menu>
            </div>

            <div v-loading="loadingCover" style="position: absolute; right: 25px" v-if="managerCode !== null">
                <form>
                    <!--
                        实现阿里云的文件上传 https://www.bilibili.com/video/BV1G84y1f7k5?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
                        https://www.bilibili.com/video/BV1rV4y1e7Vt?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
                        1、文件、文件夹多选
                        2、递归遍历文件夹的文件
                        3、获取进度
                        4、onfinish
                        5、取消上传
                    -->
                    <input type="file" multiple ref="fileList" accept=".xlsx, .xls" @change="onFileChange" style="display: none;"/>
                    <el-button v-cd="1.3" type="success" @click="onCoverClicked">覆盖库存</el-button>
                </form>
            </div>
        </div>

        <div class="page-content">
            <!--中侧-->
            <div v-loading="loadingList"
                 style="width: 100%; padding-left: 0; padding-top: 5px">
                <!--https://element-plus.org/zh-CN/component/table.html#%E7%AD%9B%E9%80%89-->
                <div>
                    <el-input
                        v-model="filterName"
                        style="width: 300px"
                        placeholder="名字(可不填)"
                        clearable
                    />

                    <el-input
                        v-model="filterModel"
                        style="position: relative; left: 2px; width: 300px"
                        placeholder="规格型号(可不填)"
                        clearable
                    />
                    <el-input
                        v-model="filterDeclarer"
                        style="position: relative; left: 4px; width: 300px"
                        placeholder="申报单位/人(可不填)"
                        clearable
                    />

                    <el-input
                        v-model="filterHt"
                        style="position: relative; left: 6px; width: 300px"
                        placeholder="采购合同(可不填)"
                        clearable
                    />

                    <el-button v-cd="1.3" type="primary" style="position: relative; left: 8px;" @click="onClearClicked">清除</el-button>
                    <el-button v-cd="1.3" type="primary" style="position: relative; left: 0;" @click="onSearchClicked">搜索</el-button>
                </div>

                <div style="position: relative; top: 3px">
                    <el-table show-overflow-tooltip :data="formList" fit stripe border highlight-current-row max-height="100%">
                        <el-table-column type="index" :index="indexMethod" label="序号" width="65"></el-table-column>
                        <el-table-column sortable prop="c_name" label="名称" width="300"/>
                        <el-table-column prop="c_number" label="数量" width="150"/>
                        <el-table-column prop="c_unit" label="单位" width="100"/>
                        <el-table-column prop="c_model" label="型号" width="360"/>
                        <el-table-column sortable prop="c_declarer" label="申报单位/人" width="300"/>
                        <el-table-column sortable prop="c_ht" label="甲方采购合同"/>
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
        </div>
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