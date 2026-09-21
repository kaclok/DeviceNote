<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {downloadTemplate, parseContractExcel} from "../utils/ExcelX.js"
import {useRouter} from 'vue-router';
import {buildDeptPathMap, buildScopedDeptTree, matchDept, deptDisplay, deptScopeDepts, effectiveScope, scopeText, SCOPE} from "../utils/DeptX.js"
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts"
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const router = useRouter();

// 数据范围（data_scope）：与后端 CCGHT.resolveScopeDepts 同口径（档位编号即包含序）——
// 4 = 全集团（不限制）；3 本公司 / 2 本部门（含下级）逐级收敛。
// 前端只做体验优化（让用户选不到越权部门），真正的拦截在后端 contract/import 的逐行校验。
const {wsCache} = useSessionCache()
const _acc = wsCache.get(ECacheType.ACCOUNT) || {}
// 有效范围：唯一来源是账号行自己的 data_scope（与后端 dataScopeOf 同口径，角色侧已无该字段）
const dataScope = effectiveScope(_acc)
const scopeAll = dataScope === SCOPE.ALL
// 展开起点：账号自己的归属部门 —— 与后端 expandScope 的入参同口径
const myDeptCode = _acc.dept_code || ''

const importing = ref(false)
const result = ref(null)          // {success, fail, failRows:[{row,id,title,reason}]}
const file = ref(null)
// 归属部门：导入的整批合同统一归属该部门（必填，导入前先选定）
// 受限账号只能导到自己范围内，直接预填归属部门省一步；全集团账号留空，必须显式选择
const deptCode = ref(scopeAll ? '' : myDeptCode)
const allDeptOptions = ref([])
const deptPathMap = computed(() => buildDeptPathMap(allDeptOptions.value))

/** 归属部门展示文本：命中字典 → 「公司/部门」；未命中 → 「未知部门(code)」 */
function deptPath(code) {
    return deptDisplay(deptPathMap.value, code)
}

/* ---------------- 左侧常驻组织架构树 ---------------- */
const treeRef = ref()
const treeKeyword = ref('')

/**
 * 本账号「可见部门」的原样三态：null = 不限制（全集团）/ [] = 无可见部门（fail-closed）。
 * 口径与后端 expandScope 一致（4 全量 / 3 本公司子树 / 2 起点子树 / 其余起点）。
 * ⚠️ 不做全量 fallback —— 受限账号不能因为"恰好等于全量"被当成不限。
 */
const visibleDeptCodes = computed(() => {
    const vis = deptScopeDepts(allDeptOptions.value, dataScope, myDeptCode)
    return vis === null ? null : new Set(vis.map(d => d.dept_code))
})

/**
 * 树数据：全量字典按可见集剪枝 + 补回祖先链（否则父节点缺失，每个部门都会变成根节点）。
 * 祖先节点由 buildScopedDeptTree 标成 selectable=false —— 只作层级路径，不可选。
 * 常态收缩：不设 default-expanded-keys，全部折叠（116 个部门一次铺开会把左栏撑成长条）。
 * 搜索无需额外处理 —— Element Plus 的 tree-store.filter 会对每个可见非叶节点调 node.expand()，
 * 自顶向下遍历，命中项的整条祖先路径会自动展开。
 */
const treeData = computed(() => buildScopedDeptTree(allDeptOptions.value, visibleDeptCodes.value))

/** 树搜索：部门名 / 公司·部门全路径 / 部门编码 任一命中（父节点因有命中子节点而保留） */
function filterNode(value, data) {
    return matchDept(data, value, deptPathMap.value)
}

watch(treeKeyword, v => {
    treeRef.value?.filter(String(v || ''))
})

function nodeTitle(data) {
    return data.selectable ? deptPath(data.dept_code) : '该部门不在你的数据范围内，仅作为层级路径展示'
}

/** 点树节点即选定归属部门（本批合同统一归属它）；祖先节点只作层级路径，不可选 */
function onTreeClick(data) {
    if (!data.selectable) {
        ElMessage.warning('该部门不在你的数据范围内，仅作为层级路径展示')
        return
    }
    deptCode.value = data.dept_code
}

/** 清空选择：回到"未选择"（上传区随之禁用，必须重新选一个） */
function clearDept() {
    deptCode.value = ''
    treeRef.value?.setCurrentKey(null)
}

const AC_import = new AbortController()
const AC_dept = new AbortController()

onMounted(() => {
    loadDepts()
})

onUnmounted(() => {
    AC_import.abort()
    AC_dept.abort()
})

// 归属部门字典：登录后已由 SysX 预加载缓存，这里命中缓存即刻返回
function loadDepts() {
    Singleton.getInstance(SysX).getDeptList(null, AC_dept.signal, () => {
    }, (r, data) => {
        if (r) allDeptOptions.value = data.data || []
    })
}

// 拖拽/选择上传
const fileInput = ref()

/**
 * 点点击上传区：未选归属部门时连文件选择框都不打开。
 * 归属部门是必填项，先选后传能避免用户选完文件才被拦（那一趟已白跑一次解析）。
 */
function onUploadClick() {
    if (!deptCode.value) {
        ElMessage.warning('请先选择归属部门，再上传文件')
        return
    }
    fileInput.value.click()
}

function onFileChange(e) {
    const f = e.target.files[0]
    if (!f) return
    handleFile(f)
}

function onDrop(e) {
    if (!deptCode.value) {
        ElMessage.warning('请先选择归属部门，再上传文件')
        return
    }
    const f = e.dataTransfer.files[0]
    if (f) handleFile(f)
}

async function handleFile(f) {
    if (!deptCode.value) {
        ElMessage.warning('请先选择归属部门，再上传文件')
        return
    }
    if (!/\.(xlsx|xls)$/i.test(f.name)) {
        ElMessage.error('仅支持 .xlsx / .xls 文件')
        return
    }
    file.value = f
    importing.value = true
    try {
        const rows = await parseContractExcel(f)
        if (rows.length === 0) {
            ElMessage.warning('文件中没有可导入的数据')
            importing.value = false
            return
        }
        // 整批统一打上所选归属部门（后端逐行强校验：缺部门或部门非法都按行拦截）
        rows.forEach(r => {
            r.dept_code = deptCode.value
        })
        Singleton.getInstance(SysX).importContractExcel(rows, AC_import.signal, () => {
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
                notifyError(data, '导入失败')
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
            <div class="head-desc">下载模板 → 填写数据 → 选择归属部门 → 上传校验 → 查看导入结果（支持 .xlsx / .xls，单次最多 1000 行）</div>
        </div>

        <!-- 模板 -->
        <el-card shadow="never" class="block-card">
            <div class="block-title">① 下载模板</div>
            <div class="block-body">
                <el-button @click="downloadTemplate">⬇️ 下载导入模板</el-button>
                <!--                <el-button @click="downloadTemplate">📄 查看填写说明</el-button>-->
                <div class="tip-text">模板包含全部字段与示例行，带 * 的为必填项；合同编号重复将整行拦截</div>
            </div>
        </el-card>

        <!-- ② 归属部门 + ③ 上传文件：左树右传（与账号管理页同构），左树常态收缩 -->
        <div class="import-body">
            <!-- 左侧：常驻组织架构。点部门即选定本批合同的归属部门（与账号页一样，点一次即生效） -->
            <el-card shadow="never" class="dept-aside">
                <div class="aside-head">
                    <span class="aside-title">② 归属部门（必填）</span>
                    <span v-if="deptCode" class="aside-clear" @click="clearDept">清空</span>
                </div>
                <el-input v-model="treeKeyword" placeholder="搜索部门" clearable size="small" class="aside-search">
                    <template #prefix><span style="color:#94a3b8">🔍</span></template>
                </el-input>
                <div class="tree-box">
                    <el-tree
                        ref="treeRef"
                        :data="treeData"
                        node-key="dept_code"
                        :props="{label: 'dept_name', children: 'children'}"
                        :current-node-key="deptCode || null"
                        :filter-node-method="filterNode"
                        highlight-current
                        :expand-on-click-node="false"
                        @node-click="onTreeClick"
                    >
                        <template #default="{ data }">
                            <span class="tree-node" :class="{'node-plain': !data.selectable}" :title="nodeTitle(data)">
                                {{ data.dept_name }}
                            </span>
                        </template>
                    </el-tree>
                </div>
                <div class="aside-foot">
                    <div class="picked-line">
                        <!-- 选中即"啪"地弹出绿勾、清空即"啪"地缩没（enter/leave 两套 animation，见样式）。
                             勾本身也是清空入口：点它等同点右上角「清空」，点完勾自己"啪"地消失 -->
                        <Transition name="check">
                            <span v-if="deptCode" class="check-pop" role="button" tabindex="0"
                                  title="清除已选部门" aria-label="清除已选部门"
                                  @click="clearDept" @keydown.enter.prevent="clearDept" @keydown.space.prevent="clearDept">
                                <svg viewBox="0 0 24 24" width="11" height="11" aria-hidden="true">
                                    <path d="M4.5 12.6 L9.8 17.8 L19.5 7.2"/>
                                </svg>
                            </span>
                        </Transition>
                        <span class="picked-label">已选：</span>
                        <b v-if="deptCode" :title="deptPath(deptCode)">{{ deptPath(deptCode) }}</b><span v-else class="picked-empty">未选择</span>
                    </div>
                    <div v-if="!scopeAll" class="scope-line"
                         title="你的账号只能把合同导入到数据范围内的部门。需要更大范围请联系管理员调整数据范围。">
                        数据范围：{{ scopeText(dataScope) }}
                    </div>
                </div>
            </el-card>

            <!-- 右侧：上传区。未选定部门时禁用（点击/拖拽都会给出明确提示） -->
            <el-card shadow="never" class="upload-card">
                <div class="block-title">③ 上传文件</div>
                <div class="upload-zone" :class="{dragging: importing, disabled: !deptCode}"
                     @click="onUploadClick"
                     @dragover.prevent="importing = true" @dragleave.prevent="importing = false" @drop.prevent="onDrop">
                    <div class="uic">📂</div>
                    <div class="u-main">将 Excel 文件拖拽到此处，或 <b>点击选择文件</b></div>
                    <div class="u-sub">支持 .xlsx / .xls，单次最多 1000 行；导入前将进行必填、格式、编号唯一性校验</div>
                    <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileChange"/>
                </div>
                <div v-if="!deptCode" class="warn-tip">请先在左侧选择归属部门</div>
                <div v-if="importing" class="importing-tip">
                    <el-icon class="is-loading" style="margin-right:6px"><i class="el-icon-loading"/></el-icon>
                    正在解析并校验...
                </div>
                <div class="tip-text">本批合同将统一归属到左侧选中的部门；未选择部门时无法上传。可在左侧搜索框按部门名称或编码模糊匹配。</div>
            </el-card>
        </div>

        <!-- 结果 -->
        <el-card v-if="result" shadow="never" class="block-card">
            <div class="block-title">④ 导入结果</div>
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

    /* ① / ④ 仍是纵向堆叠的整块卡片 */
    .block-card {
        margin-bottom: 16px;
    }

    /* 区块标题：①②③④ 共用 */
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

    .warn-tip {
        margin-top: 12px;
        font-size: 12px;
        color: #e6a23c;
    }

    .upload-zone {
        /* 右侧卡片内撑满剩余高度，虚线框内容垂直居中 */
        flex: 1;
        display: flex;
        flex-direction: column;
        justify-content: center;
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

            .uic {
                transform: scale(1.1)
            }
        }

        /* 未选归属部门：视觉上提示不可用（点击仍会给出明确提示） */
        &.disabled {
            opacity: .6;

            &:hover {
                border-color: #cbd5e1;
                background: transparent;
                color: #64748b;
            }
        }

        .uic {
            font-size: 42px;
            margin-bottom: 10px;
            transition: transform .2s;
        }

        .u-main {
            font-size: 15px;

            b {
                color: #2563eb
            }
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

    /* ② + ③：左树右传，两栏等高（左侧定宽、右侧吃掉剩余宽度）。
       min-height 保证树与上传区都不会被内容挤扁。 */
    .import-body {
        display: flex;
        align-items: stretch;
        gap: 12px;
        margin-bottom: 16px;
        min-height: 440px;

        .dept-aside {
            width: 260px;
            flex-shrink: 0;
            /* 高度固定：不随右侧上传区内容伸缩；树内容超高时在 .tree-box 内部滚动 */
            height: 440px;
            display: flex;
            flex-direction: column;

            /* 卡片内部纵向 flex：把固定高度内的剩余空间全部让给树区（超高即在树区内滚动） */
            :deep(.el-card__body) {
                flex: 1;
                min-height: 0;
                display: flex;
                flex-direction: column;
                padding: 12px;
            }

            .aside-head {
                display: flex;
                align-items: center;
                justify-content: space-between;

                .aside-title {
                    font-size: 13px;
                    font-weight: 600;
                }

                .aside-clear {
                    font-size: 12px;
                    color: #2563eb;
                    cursor: pointer;
                }
            }

            .aside-search {
                margin-top: 10px;
            }

            .tree-box {
                flex: 1;
                min-height: 0;
                margin-top: 8px;
                overflow: auto;
                border: 1px solid #e2e8f0;
                border-radius: 8px;
                padding: 4px 2px;

                :deep(.el-tree) {
                    font-size: 12px;
                    background: transparent;
                }

                /* 节点内容行是 flex 容器：让文本项 flex:1 + min-width:0 才能真正触发省略号 */
                :deep(.el-tree-node__content) {
                    overflow: hidden;
                }
            }

            .aside-foot {
                margin-top: 10px;
                font-size: 12px;
                color: #64748b;

                .picked-line {
                    display: flex;
                    align-items: flex-start;
                    gap: 4px;

                    .picked-label {
                        flex-shrink: 0;
                        line-height: 1.45;
                    }

                    /* 全路径可能长达两三行：最多折两行，超出才省略；完整路径挂 title，悬浮可看全 */
                    b {
                        flex: 1 1 auto;
                        min-width: 0;
                        display: -webkit-box;
                        -webkit-box-orient: vertical;
                        -webkit-line-clamp: 2;
                        overflow: hidden;
                        word-break: break-all;
                        line-height: 1.45;
                        color: #2563eb;
                    }

                    .picked-empty {
                        color: #e6a23c;
                    }
                }

                .scope-line {
                    margin-top: 4px;
                    color: #94a3b8;
                    cursor: help;
                }
            }

            /* 选中态绿勾：圆形底 + 白色描边勾。
               enter/leave 用两套 animation（而非默认 opacity 过渡）—— "啪"的手感来自
               过冲回弹曲线与勾的描线在同一收尾点结束。 */
            .check-pop {
                display: inline-flex;
                align-items: center;
                justify-content: center;
                flex-shrink: 0;
                margin-top: 1px; /* 顶部对齐首行文字（行高 1.45），视觉上落在文字中心 */
                width: 16px;
                height: 16px;
                border-radius: 50%;
                background: #16a34a;
                cursor: pointer;
                outline: none;
                transition: transform .12s ease-out, background-color .15s;

                /* 可点提示：hover 放大加深、按下回缩。只动 transform / 背景，不改布局尺寸 */
                &:hover {
                    background: #15803d;
                    transform: scale(1.18);
                }

                &:active {
                    transform: scale(.92);
                }

                &:focus-visible {
                    box-shadow: 0 0 0 2px rgba(22, 163, 74, .35);
                }

                svg {
                    display: block;
                    transform: translateX(1px); /* 勾的视觉重心偏左，微调回圆中心 */
                }

                path {
                    fill: none;
                    stroke: #fff;
                    stroke-width: 3;
                    stroke-linecap: round;
                    stroke-linejoin: round;
                    stroke-dasharray: 22; /* 常态 dashoffset=0 → 完整显示；enter 时描出来 */
                }
            }

            .check-enter-active {
                animation: checkPopIn .34s cubic-bezier(.34, 1.56, .64, 1) both;

                path {
                    animation: checkDraw .26s ease-out both;
                }
            }

            .check-leave-active {
                animation: checkPopOut .15s cubic-bezier(.4, 0, 1, 1) both;
            }

            @keyframes checkPopIn {
                0% {
                    transform: scale(0) rotate(-40deg);
                    opacity: 0;
                }
                60% {
                    transform: scale(1.28) rotate(6deg);
                    opacity: 1;
                }
                100% {
                    transform: scale(1) rotate(0);
                    opacity: 1;
                }
            }

            @keyframes checkPopOut {
                0% {
                    transform: scale(1) rotate(0);
                    opacity: 1;
                }
                100% {
                    transform: scale(0) rotate(35deg);
                    opacity: 0;
                }
            }

            @keyframes checkDraw {
                from {
                    stroke-dashoffset: 22;
                }
                to {
                    stroke-dashoffset: 0;
                }
            }
        }

        .upload-card {
            flex: 1;
            min-width: 0;
            display: flex;
            flex-direction: column;

            :deep(.el-card__body) {
                flex: 1;
                min-height: 0;
                display: flex;
                flex-direction: column;
            }
        }
    }

    /* 树节点：窄栏里超长部门名省略，完整路径走 title */
    .tree-node {
        flex: 1;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;

        /* 祖先路径节点：不在数据范围内，不可点，灰显 */
        &.node-plain {
            color: #c0c4cc;
            cursor: not-allowed;
        }
    }
}
</style>
