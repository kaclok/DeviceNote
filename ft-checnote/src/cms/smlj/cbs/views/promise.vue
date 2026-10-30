<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ElMessage} from "element-plus";
import {Search} from "@element-plus/icons-vue";
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

/* ============ 列表 + 分页（对齐目标站「安全承诺」列表：日期 / 是否承诺 / 操作） ============ */
const loading = ref(false)
const rows = ref([])
const dateFilter = ref('')
const PAGER = pagerOf('promise')
const pageNo = ref(1)
const pageSize = ref(PAGER.pageSize)
const total = ref(0)

let AC = new AbortController()

function loadRows() {
    AC.abort()
    AC = new AbortController()
    loading.value = true
    const paras = {pageNo: pageNo.value, pageSize: pageSize.value}
    if (dateFilter.value) paras.date = dateFilter.value
    sys.biz('cbs/promise/list', paras, AC.signal, () => {}, (r, data) => {
        loading.value = false
        if (r) {
            rows.value = data.data?.rows || []
            total.value = data.data?.total || 0
        } else {
            notifyError(data, '承诺列表加载失败')
        }
    })
}

watch([pageNo, pageSize], loadRows)

function search() {
    pageNo.value = 1
    loadRows()
}

/* ============ 详情 / 填报 ============ */
const detailVisible = ref(false)
const form = ref(blankForm(''))
const formLoading = ref(false)
const saving = ref(false)
const curDate = ref('')

function openDetail(row) {
    curDate.value = row.promise_date
    form.value = blankForm(row.promise_date)
    detailVisible.value = true
    formLoading.value = true
    sys.biz('cbs/promise/get', {date: row.promise_date}, AC.signal, () => {}, (r, data) => {
        formLoading.value = false
        if (r) {
            // 后端 jackson NON_EMPTY 会吞掉空串/缺失字段：以本地默认值打底再覆盖
            form.value = {...blankForm(row.promise_date), ...(data.data || {}), promise_date: row.promise_date}
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
        if (r) {
            ElMessage.success('已保存')
            detailVisible.value = false
            loadRows()
        } else {
            notifyError(data, '保存失败')
        }
    })
}

onMounted(loadRows)
onUnmounted(() => AC.abort())
</script>

<template>
    <div class="promise-page">
        <div class="page-head">
            <h2>安全承诺</h2>
        </div>

        <div class="card">
            <div class="toolbar">
                <el-date-picker v-model="dateFilter" type="date" value-format="YYYY-MM-DD"
                                placeholder="承诺日期" clearable style="width:170px"/>
                <el-button type="primary" :prefix-icon="Search" @click="search">查询</el-button>
                <el-button @click="dateFilter = ''; search()">重置</el-button>
            </div>

            <el-table :data="rows" v-loading="loading" border stripe>
                <el-table-column type="index" label="序号" width="70" align="center"/>
                <el-table-column prop="promise_date" label="日期" min-width="140" align="center"/>
                <el-table-column label="是否承诺" width="110" align="center">
                    <template #default>
                        <!-- 口径：当日存在承诺记录即为「是」（目标站同口径，与承诺正文是否填写无关） -->
                        <el-tag type="success" effect="light" size="small">是</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                    <template #default="{row}">
                        <el-link type="primary" @click="openDetail(row)">详情承诺</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" description="暂无承诺记录"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="PAGER.pageSizes"/>
        </div>

        <!-- 详情填报弹窗 -->
        <el-dialog v-model="detailVisible" :title="`安全承诺 — ${curDate}`" width="900px" destroy-on-close>
            <div v-loading="formLoading">
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
            <template #footer>
                <el-button @click="detailVisible = false">关闭</el-button>
                <el-button type="primary" :loading="saving" @click="savePromise">保存</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.promise-page {
    .page-head {
        margin: 4px 0 18px;
        text-align: center;

        h2 {
            margin: 0;
            font-size: 22px;
            font-weight: 700;
            color: #1e293b;
            letter-spacing: 2px;
        }
    }

    .card {
        background: #ffffff;
        border-radius: 10px;
        padding: 16px;
        border: 1px solid #e2e8f0;
    }

    .toolbar {
        display: flex;
        align-items: center;
        gap: 12px;
        margin-bottom: 14px;
    }

    .pager {
        margin-top: 14px;
        justify-content: flex-end;
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
