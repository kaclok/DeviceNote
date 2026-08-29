<script setup>
import gd from "./data/gd.json";
import {SysX} from "./system/SysX.js";
import {Singleton} from "@/framework/services/Singleton.js";
import {ref} from "vue";
import {ElMessage} from "element-plus";

/* ---------------- 固定配置(gd.json) ---------------- */
// 车道列表
const lans = gd.lans
// 道闸操作: 0开启道闸 1常开锁定 2解锁恢复
const lanStatus = gd.lanStatus

/* ---------------- 页面状态 ---------------- */
const curLanId = ref(lans[0]?.id)
const curStatus = ref(lanStatus[0]?.id)
const loading = ref(false)
let AC = null

/* ---------------- 交互 ---------------- */
function onSubmit() {
    AC?.abort();
    AC = new AbortController();

    Singleton.getInstance(SysX).openDoor({
        laneId: curLanId.value,
        status: curStatus.value,
    }, AC.signal, () => {
        loading.value = true;
    }, (r, data) => {
        loading.value = false;

        if (r) {
            // 后端返回 Result.success(boolean), data.data 为操作结果
            if (data.data === true) {
                ElMessage({
                    showClose: true,
                    message: '操作成功',
                    type: 'success',
                    center: true,
                    duration: 2000,
                });
            } else {
                ElMessage({
                    showClose: true,
                    message: '操作失败，请检查后端',
                    type: 'error',
                    center: true,
                    duration: 2000,
                });
            }
        } else {
            // 后端真实错误直接暴露, 不兜底
            ElMessage({
                showClose: true,
                message: data?.data?.message || '请求失败',
                type: 'error',
                center: true,
                duration: 2000,
            });
        }
    });
}
</script>

<template>
    <div class="page-container">
        <div class="page-title">
            <span class="page-title-content">车道闸控制</span>
        </div>

        <div class="page-content">
            <el-card class="op-card" shadow="never">
                <el-form label-width="70px" label-position="left">
                    <el-form-item label="车道">
                        <el-select v-model="curLanId" placeholder="请选择车道" style="width: 100%">
                            <el-option v-for="l in lans" :key="l.id" :label="l.desc" :value="l.id"/>
                        </el-select>
                    </el-form-item>

                    <el-form-item label="操作">
                        <el-select v-model="curStatus" placeholder="请选择操作" style="width: 100%">
                            <el-option v-for="s in lanStatus" :key="s.id" :label="s.status" :value="s.id"/>
                        </el-select>
                    </el-form-item>

                    <el-form-item>
                        <el-button type="primary" :loading="loading" style="width: 100%" @click="onSubmit">确定
                        </el-button>
                    </el-form-item>
                </el-form>
            </el-card>
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
}

.page-title {
    width: 100%;
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #1C4785;
}

.page-title-content {
    color: #ffffff;
    font-size: 18px;
    font-weight: bolder;
    text-align: center;
}

.page-content {
    flex: 1;
    width: 100%;
    display: flex;
    align-items: flex-start;
    justify-content: center;
    padding: 24px 16px;
    box-sizing: border-box;
}

.op-card {
    width: 100%;
    max-width: 420px;
}
</style>
