<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {downloadTemplate, parseContractExcel} from "../utils/ExcelX.js"
import {useRouter} from 'vue-router';

const router = useRouter();

const importing = ref(false)
const result = ref(null)          // {success, fail, failRows:[{row,id,title,reason}]}
const file = ref(null)
const signers = ref([])          // 签订人字典，用于 Excel 中"姓名→account"转码

const AC_signers = new AbortController()

onMounted(() => {
    loadSigners()
})

onUnmounted(() => {
    AC_signers.abort()
})

function loadSigners() {
    Singleton.getInstance(SysX).getSignerList(null, AC_signers.signal, () => {
    }, (r, data) => {
        if (r) signers.value = data.data || []
    })
}

// 拖拽/选择上传
const fileInput = ref()
function onFileChange(e) {
    const f = e.target.files[0]
    if (!f) return
    handleFile(f)
}

function onDrop(e) {
    const f = e.dataTransfer.files[0]
    if (f) handleFile(f)
}

async function handleFile(f) {
    if (!/\.(xlsx|xls)$/i.test(f.name)) {
        ElMessage.error('仅支持 .xlsx / .xls 文件')
        return
    }
    file.value = f
    importing.value = true
    try {
        const rows = await parseContractExcel(f, signers.value)
        if (rows.length === 0) {
            ElMessage.warning('文件中没有可导入的数据')
            importing.value = false
            return
        }
        Singleton.getInstance(SysX).importContractExcel(rows, new AbortController().signal, () => {
        }, (r, data) => {
            importing.value = false
            if (r) {
                result.value = data.data
                if (result.value.fail > 0) {
                    ElMessage.warning(`导入完成：成功 ${result.value.success} 条，失败 ${result.value.fail} 条`)
                } else {
                    ElMessage.success(`导入成功 ${result.value.success} 条合同`)
                }
            } else {
                ElMessage.error(data?.msg || '导入失败')
            }
        })
    } catch (err) {
        importing.value = false
        ElMessage.error('文件解析失败：' + err.message)
    }
}

function reset() {
    result.value = null
    file.value = null
    fileInput.value.value = ''
}

function goLedger() {
    router.push({name: 'home_ledger'})
}
</script>

<template>
    <div class="import-page">
        <div class="page-head">
            <div class="head-title">Excel 批量导入</div>
            <div class="head-desc">下载模板 → 填写数据 → 上传校验 → 查看导入结果（支持 .xlsx / .xls，单次最多 1000 行）</div>
        </div>

        <!-- 模板 -->
        <el-card shadow="never" class="block-card">
            <div class="block-title">① 下载模板</div>
            <div class="block-body">
                <el-button @click="downloadTemplate">⬇️ 下载导入模板</el-button>
                <el-button @click="downloadTemplate">📄 查看填写说明</el-button>
                <div class="tip-text">模板包含全部字段与示例行，带 * 的为必填项；合同编号重复将整行拦截</div>
            </div>
        </el-card>

        <!-- 上传 -->
        <el-card shadow="never" class="block-card">
            <div class="block-title">② 上传文件</div>
            <div class="block-body">
                <div class="upload-zone" :class="{dragging: importing}" @click="fileInput.click()"
                     @dragover.prevent="importing = true" @dragleave.prevent="importing = false" @drop.prevent="onDrop">
                    <div class="uic">📂</div>
                    <div class="u-main">将 Excel 文件拖拽到此处，或 <b>点击选择文件</b></div>
                    <div class="u-sub">支持 .xlsx / .xls，单次最多 1000 行；导入前将进行必填、格式、编号唯一性校验</div>
                    <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileChange"/>
                </div>
                <div v-if="importing" class="importing-tip"><el-icon class="is-loading" style="margin-right:6px"><i class="el-icon-loading"/></el-icon>正在解析并校验...</div>
            </div>
        </el-card>

        <!-- 结果 -->
        <el-card v-if="result" shadow="never" class="block-card">
            <div class="block-title">③ 导入结果</div>
            <el-alert :type="result.fail > 0 ? 'warning' : 'success'" :closable="false" show-icon
                      :title="`成功 ${result.success} 条${result.fail > 0 ? `，失败 ${result.fail} 条（见下方明细）` : '，全部通过'}`"
                      style="margin-bottom:14px"/>
            <el-table v-if="result.failRows && result.failRows.length" :data="result.failRows" border size="small" max-height="320">
                <el-table-column prop="row" label="Excel 行号" width="90" align="center"/>
                <el-table-column prop="id" label="合同编号" width="180"/>
                <el-table-column prop="title" label="合同名称" min-width="120"/>
                <el-table-column prop="reason" label="失败原因" min-width="220">
                    <template #default="{row}"><span style="color:#dc2626">{{ row.reason }}</span></template>
                </el-table-column>
            </el-table>
            <div class="result-actions">
                <el-button @click="reset">重新导入</el-button>
                <el-button type="primary" @click="goLedger">前往台账查看 →</el-button>
            </div>
        </el-card>
    </div>
</template>

<style lang="scss" scoped>
.import-page {
    .page-head {
        margin-bottom: 16px;

        .head-title {
            font-size: 18px;
            font-weight: 700;
        }

        .head-desc {
            font-size: 13px;
            color: #94a3b8;
            margin-top: 4px;
        }
    }

    .block-card {
        margin-bottom: 16px;

        .block-title {
            font-size: 15px;
            font-weight: 600;
            margin-bottom: 14px;
            padding-left: 10px;
            border-left: 4px solid #2563eb;
        }

        .tip-text {
            font-size: 12px;
            color: #94a3b8;
            margin-top: 10px;
        }

        .upload-zone {
            border: 2px dashed #cbd5e1;
            border-radius: 12px;
            padding: 40px 20px;
            text-align: center;
            color: #64748b;
            cursor: pointer;
            transition: all .2s;

            &:hover, &.dragging {
                border-color: #2563eb;
                background: #eff6ff;
                color: #2563eb;

                .uic {transform: scale(1.1)}
            }

            .uic {
                font-size: 42px;
                margin-bottom: 10px;
                transition: transform .2s;
            }

            .u-main {
                font-size: 15px;

                b {color: #2563eb}
            }

            .u-sub {
                font-size: 12px;
                color: #94a3b8;
                margin-top: 8px;
            }
        }

        .importing-tip {
            margin-top: 12px;
            font-size: 13px;
            color: #2563eb;
        }

        .result-actions {
            display: flex;
            justify-content: flex-end;
            gap: 10px;
            margin-top: 16px;
        }
    }
}
</style>
