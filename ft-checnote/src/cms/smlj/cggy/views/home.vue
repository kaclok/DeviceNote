<template>
    <div class="page-container">
        <div class="page-title">
            <img src="../../../../assets/st_logo.png" class="page-logo-content" alt="">
            <span class="page-title-content">{{ "采购供应系统" }}</span>

            <div class="left-menu">
                <el-menu mode="horizontal" background-color="#1C4785" text-color="#DCDCDC" active-text-color="#ffffff"
                         router
                         :default-active='getDefaultActive()'>
                    <el-menu-item index="/home/bill">结算单</el-menu-item>
                    <el-menu-item index="/home/cgy">采购员</el-menu-item>
                    <el-menu-item index="/home/jhy">计划员</el-menu-item>
                </el-menu>
            </div>

            <div class="right-menu">
            </div>
        </div>

        <div class="page-content">
            <router-view/>
        </div>
    </div>
</template>

<script setup lang="js">
import {onMounted, onUnmounted, ref} from "vue";

const AC_getMsg = new AbortController();
const loadingMsg = ref(false);

// 清理定时器，事件监听器，异步函数
onUnmounted(() => {
    AC_getMsg.abort();
});

onMounted(() => {

});

function getDefaultActive() {
    return '/home/bill'
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