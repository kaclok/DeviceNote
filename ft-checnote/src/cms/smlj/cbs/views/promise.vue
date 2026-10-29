<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {ElMessage} from "element-plus";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/* 表单字段分区（口径同原系统安全承诺页；数字项默认 0，状态项默认 false） */
const NUM_GROUPS = [
    {title: '生产装置', items: [
        {key: 'prod_sets', label: '运行套数'},
        {key: 'stop_sets', label: '停产套数'},
    ]},
    {title: '隐患数量（按等级）', items: [
        {key: 'hazard_lv1', label: '一级隐患'},
        {key: 'hazard_lv2', label: '二级隐患'},
        {key: 'hazard_lv3', label: '三级隐患'},
        {key: 'hazard_lv4', label: '四级隐患'},
    ]},
    {title: '装置动态', items: [
        {key: 'start_sets', label: '开车装置数'},
        {key: 'stop_dev_sets', label: '停车装置数'},
        {key: 'trial_sets', label: '试生产装置数'},
        {key: 'supervision_process', label: '重点监管危险工艺'},
        {key: 'repair_sets', label: '检维修套数'},
    ]},
    {title: '特殊作业（今日数量）', items: [
        {key: 'fire_special', label: '特级动火'},
        {key: 'fire_lv1', label: '一级动火'},
        {key: 'fire_lv2', label: '二级动火'},
        {key: 'confined', label: '受限空间作业'},
        {key: 'blind', label: '盲板抽堵作业'},
        {key: 'work_height', label: '高处作业'},
        {key: 'lifting', label: '吊装作业'},
        {key: 'temp_power', label: '临时用电作业'},
        {key: 'digging', label: '动土作业'},
        {key: 'road_close', label: '断路作业'},
        {key: 'repair_work', label: '检维修作业'},
    ]},
]
const BOOL_ITEMS = [
    {key: 'has_contractor_work', label: '今日是否有承包商作业'},
    {key: 'in_trial', label: '是否处于试生产期'},
    {key: 'in_startstop', label: '是否处于开停车状态'},
    {key: 'in_pilot', label: '是否处于中(扩)试'},
    {key: 'has_major_hazard', label: '是否存在重大隐患'},
]

function blankForm(date) {
    const f = {promise_date: date, company_promise: ''}
    NUM_GROUPS.forEach(g => g.items.forEach(it => {
        f[it.key] = 0
    }))
    BOOL_ITEMS.forEach(it => {
        f[it.key] = false
    })
    return f
}

const promiseDate = ref(new Date().toISOString().slice(0, 10))
const form = ref(blankForm(promiseDate.value))
const loading = ref(false)
const saving = ref(false)

let AC = new AbortController()

function loadPromise() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    sys.biz('cbs/promise/get', {date: promiseDate.value}, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            // 后端 jackson NON_EMPTY 会吞掉空串/缺失字段：以本地默认值打底再覆盖
            const base = blankForm(promiseDate.value)
            form.value = {...base, ...(data.data || {}), promise_date: promiseDate.value}
        } else {
            notifyError(data, '安全承诺加载失败')
        }
    })
}

function savePromise() {
    saving.value = true
    const f = {...form.value}
    if (!f.company_promise) f.company_promise = null
    sys.biz('cbs/promise/save', f, AC.signal, () => {}, (r, data) => {
        saving.value = false
        if (r) ElMessage.success('已保存')
        else notifyError(data, '保存失败')
    })
}

onMounted(loadPromise)
onUnmounted(() => AC.abort())
</script>

<template>
    <div class="promise-page">
        <div class="card" v-loading="loading">
            <div class="toolbar">
                <el-date-picker v-model="promiseDate" type="date" value-format="YYYY-MM-DD"
                                :clearable="false" style="width:160px" @change="loadPromise"/>
                <span class="tip">按日填报，一天一条；切换日期会载入当日已有数据</span>
                <el-button type="primary" class="save-btn" :loading="saving" @click="savePromise">保存</el-button>
            </div>

            <template v-for="g in NUM_GROUPS" :key="g.title">
                <el-divider content-position="left">{{ g.title }}</el-divider>
                <div class="num-grid">
                    <div v-for="it in g.items" :key="it.key" class="num-item">
                        <span class="num-label">{{ it.label }}</span>
                        <el-input-number v-model="form[it.key]" :min="0" :precision="0" :controls="false"
                                         class="num-input"/>
                        <span class="num-unit">个</span>
                    </div>
                </div>
            </template>

            <el-divider content-position="left">其他状态</el-divider>
            <div class="bool-grid">
                <div v-for="it in BOOL_ITEMS" :key="it.key" class="bool-item">
                    <span>{{ it.label }}</span>
                    <el-switch v-model="form[it.key]"/>
                </div>
            </div>

            <el-divider content-position="left">企业安全承诺</el-divider>
            <el-input v-model="form.company_promise" type="textarea" :rows="5"
                      placeholder="填写当日企业安全承诺内容"/>
        </div>
    </div>
</template>

<style lang="scss" scoped>
.promise-page {
    max-width: 860px;

    .card {
        background: #ffffff;
        border-radius: 10px;
        padding: 16px 24px 24px;
        border: 1px solid #e2e8f0;
    }

    .toolbar {
        display: flex;
        align-items: center;
        gap: 12px;

        .tip {
            font-size: 12px;
            color: #94a3b8;
        }

        .save-btn {
            margin-left: auto;
        }
    }

    .num-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 10px 24px;

        .num-item {
            display: flex;
            align-items: center;
            gap: 8px;

            .num-label {
                flex: 1;
                font-size: 13px;
                color: #475569;
            }

            .num-input {
                width: 90px;
            }

            .num-unit {
                font-size: 12px;
                color: #94a3b8;
            }
        }
    }

    .bool-grid {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 12px 24px;

        .bool-item {
            display: flex;
            align-items: center;
            justify-content: space-between;
            font-size: 13px;
            color: #475569;
            padding: 6px 12px;
            background: #f8fafc;
            border-radius: 8px;
        }
    }
}
</style>
