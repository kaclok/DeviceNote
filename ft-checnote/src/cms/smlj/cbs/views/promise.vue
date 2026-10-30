<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import gd from "../data/gd.json"
import {pagerOf} from "../system/Pager.js"
import {ElMessage} from "element-plus";
import {Search} from "@element-plus/icons-vue";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const sys = Singleton.getInstance(SysX)

/*
 * 填报字段口径照目标站「安全承诺」弹窗：一张扁平表单，每行 3 项（span=8），
 * 顺序与标签、单位都与目标站一致，不做分组。数字项默认 0，是否项默认「否」。
 */
const NUM_FIELDS = [
    {key: 'total_sets', label: '生产装置套数', unit: '套'},
    {key: 'prod_sets', label: '运行套数', unit: '套'},
    {key: 'stop_sets', label: '停产套数', unit: '套'},
    {key: 'hazard_lv1', label: '一级重大危险源', unit: '处'},
    {key: 'hazard_lv2', label: '二级重大危险源', unit: '处'},
    {key: 'hazard_lv3', label: '三级重大危险源', unit: '处'},
    {key: 'hazard_lv4', label: '四级重大危险源', unit: '处'},
    {key: 'start_sets', label: '开车装置数', unit: '处'},
    {key: 'stop_dev_sets', label: '停车装置数', unit: '处'},
    {key: 'trial_sets', label: '试生产装置数', unit: '处'},
    {key: 'supervision_process', label: '重点监管危险工艺', unit: '种'},
    {key: 'repair_sets', label: '检维修套数', unit: '套'},
    {key: 'fire_special', label: '特级动火作业', unit: '处'},
    {key: 'fire_lv1', label: '一级动火作业', unit: '处'},
    {key: 'fire_lv2', label: '二级动火作业', unit: '处'},
    {key: 'confined', label: '进入受限空间作业', unit: '处'},
    {key: 'blind', label: '盲板抽堵作业', unit: '处'},
    {key: 'work_height', label: '高处作业', unit: '处'},
    {key: 'lifting', label: '吊装作业', unit: '处'},
    {key: 'temp_power', label: '临时用电作业', unit: '处'},
    {key: 'digging', label: '动土作业', unit: '处'},
    {key: 'road_close', label: '断路作业', unit: '处'},
    {key: 'repair_work', label: '检维修作业', unit: '处'},
]

const BOOL_FIELDS = [
    {key: 'has_contractor_work', label: '是否有承包商作业'},
    {key: 'in_trial', label: '是否处于试生产期'},
    {key: 'in_startstop', label: '是否处于开停车状态'},
    {key: 'in_pilot', label: '是否开展中(扩)试'},
    {key: 'has_major_hazard', label: '有无重大隐患'},
]

function blankForm(date) {
    const f = {promise_date: date, company_promise: ''}
    NUM_FIELDS.forEach(it => {
        f[it.key] = 0
    })
    BOOL_FIELDS.forEach(it => {
        f[it.key] = false
    })
    return f
}

/* ============ 列表 + 分页（日期 / 是否承诺 / 操作） ============ */
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

/* ============ 承诺填报 ============ */
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
    NUM_FIELDS.forEach(it => {
        f[it.key] = Number(f[it.key] || 0)
    })
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
                        <el-link type="primary" @click="openDetail(row)">承诺</el-link>
                    </template>
                </el-table-column>
            </el-table>
            <el-empty v-if="!loading && rows.length === 0" description="暂无承诺记录"/>

            <el-pagination class="pager" background layout="total, sizes, prev, pager, next, jumper"
                           :total="total" v-model:current-page="pageNo" v-model:page-size="pageSize"
                           :page-sizes="PAGER.pageSizes"/>
        </div>

        <!-- 承诺填报（照目标站：一张扁平表单，每行 3 项，不切块） -->
        <el-dialog v-model="detailVisible" :title="`安全承诺填报${curDate ? '（' + curDate + '）' : ''}`"
                   width="1200px" top="6vh" destroy-on-close>
            <div v-loading="formLoading">
                <el-form :model="form" label-width="150px" label-position="right" class="promise-form">
                    <el-row :gutter="20">
                        <el-col v-for="f in NUM_FIELDS" :key="f.key" :span="8">
                            <el-form-item :label="f.label">
                                <el-input v-model="form[f.key]" type="number" min="0" :placeholder="`请输入${f.label}`">
                                    <template #append>{{ f.unit }}</template>
                                </el-input>
                            </el-form-item>
                        </el-col>
                        <el-col v-for="f in BOOL_FIELDS" :key="f.key" :span="8">
                            <el-form-item :label="f.label">
                                <el-radio-group v-model="form[f.key]">
                                    <el-radio :value="true">是</el-radio>
                                    <el-radio :value="false">否</el-radio>
                                </el-radio-group>
                            </el-form-item>
                        </el-col>
                        <el-col :span="24">
                            <el-form-item label="公司承诺">
                                <el-input v-model="form.company_promise" type="textarea" :rows="5"
                                          placeholder="请输入公司承诺"/>
                            </el-form-item>
                        </el-col>
                    </el-row>
                </el-form>
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

    .promise-form {
        padding: 4px 8px 0;

        :deep(.el-form-item) {
            margin-bottom: 16px;
        }

        :deep(.el-input-group__append) {
            padding: 0 12px;
            color: #64748b;
        }
    }
}
</style>
