<script setup lang="js">
import {SysX} from "../system/SysX.js"
import {Singleton} from "@/framework/services/Singleton.js";
import {parseContractExcel} from "../utils/ExcelX.js"
import {useRouter} from 'vue-router';
import {buildDeptPathMap, buildScopedDeptTree, matchDept, deptDisplay, deptScopeDepts, effectiveScope, scopeText, SCOPE} from "../utils/DeptX.js"
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts"
import {notifyError} from "@/framework/services/net/NwCodeMap.js"
import {ElMessage, ElMessageBox} from "element-plus"

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
/**
 * 有没有配置类权限（perm:assign）：决定"没有模板"那个弹窗给不给「前往配置」的出口。
 * 导入岗只持 contract:import，点了「前往配置」也会被路由守卫弹回来 ——
 * 与其让用户撞一次空门，不如直接告诉他"找管理员配"。
 */
const canAssign = Array.isArray(_acc.role?.perms) && _acc.role.perms.includes('perm:assign')

const importing = ref(false)
const result = ref(null)          // {success, fail, failRows:[{row,id,title,reason}]}
const file = ref(null)

/* ---------------- ① 归属部门（本页第一步） ----------------
 * 流程反过来了：先定部门 → 部门用的合同模板由后端沿组织树算出来 → 有模板才允许传文件。
 * 为什么把"选模板"从这一页整个拿掉：模板是部门级的系统配置（一个部门只能有一套），
 * 让导入的人自己挑模板，等于给他留着绕开配置页改口径的口子。现在模板由部门决定，
 * 本页只如实呈现"这个部门用的是哪套、写进哪张表"。
 * 模板的预览与下载都搬到「部门合同模板」页了 —— 配之前先看一眼，顺序也更自然。
 * 刻意不预填归属部门：预填会在进页面时就弹出"该部门没配模板"，白白惊用户一下。 */
const deptCode = ref('')
const allDeptOptions = ref([])
const deptPathMap = computed(() => buildDeptPathMap(allDeptOptions.value))

/** 归属部门展示文本：命中字典 → 「公司/部门」；未命中 → 「未知部门(code)」 */
function deptPath(code) {
    return deptDisplay(deptPathMap.value, code)
}

/**
 * 已选路径按层级拆行：部门在第几层就占几行，一行一层（窄侧栏里固定折 2 行会把第 3 层吃掉）。
 * 高度不再写死 —— 行数即层数，由 .picked-seg 的 block 自然撑开。
 */
const deptPathSegments = computed(() => {
    if (!deptCode.value) return []
    return deptPath(deptCode.value).split('/').filter(Boolean)
})

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
 * 默认全展开（模板上的 default-expand-all），超出栏高时由 .tree-box 滚动。
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

/* ---------------- 部门 → 合同模版（后端算，前端只呈现） ----------------
 * 绑定关系打在部门上、**不向下继承**（一个部门用哪套模板就是它自己配的那套），
 * 所以"这个部门到底用哪套模板"只有后端能给准话 —— 未配置就是没有模板可用。
 * ⚠️ 这一层只负责"别让用户白跑一趟解析"；真正的拦截在后端 contract/import 的逐行校验 ——
 *    前端可以被绕过（改包、直连），后端不能。 */
const effBind = ref(null)          // 该部门绑定的模板；null = 该部门未配置
const effLoading = ref(false)
const effLoadOk = ref(true)        // 核对请求是否成功：失败时不给结论，只提示稍后重试
const AC_eff = new AbortController()

/**
 * 该部门配置的模板（后端查绑定表，与「部门合同模板」页同口径）。
 * 后端把 tb_name / importable 一并下发：本页因此不必再拉一次模板列表，
 * 也少了一份可能过期的副本 —— 页面显示的"写进哪张表"就是后端认定的那张。
 */
const curTpl = computed(() => {
    const b = effBind.value
    if (!b || b.tpl_id == null) return null
    return {
        id: b.tpl_id,
        name: b.tpl_name || ('#' + b.tpl_id),
        tb_name: b.tb_name || '',
        // 判据写成 "!== false"：后端没下发该字段（例如命中旧缓存）时不拦，由后端 fail-closed 兜底
        importable: b.importable !== false,
    }
})

/** 该部门用的模板是否可用；不可用时给出具体原因（'' = 可用） */
const tplBlockMsg = computed(() => {
    const t = curTpl.value
    if (!t || t.importable) return ''
    return t.tb_name
        ? `模版「${t.name}」对应的物理表 ${t.tb_name} 不存在或结构未就绪，暂时无法导入`
        : `模版「${t.name}」已失效（模板记录不存在），请联系管理员`
})

/** 拉取某部门实际生效的模版；顺带给"没配置"的情况弹一次提示 */
function loadEffective(code) {
    if (!code) {
        effBind.value = null
        effLoadOk.value = true
        return
    }
    effLoading.value = true
    Singleton.getInstance(SysX).getDeptTplEffective({dept_code: code}, AC_eff.signal, () => {
    }, (r, data) => {
        // 快速连点两个部门时，先发的请求可能后到 —— 回来时部门已经换了，就别再改界面了
        if (code !== deptCode.value) return
        effLoading.value = false
        // 核对失败时按"未绑定"处理会误导用户去建立绑定，所以这里只把结论置空，
        // 由 effLoadOk 让 bindState 退回 idle（不给结论）
        effBind.value = r ? (data.data || null) : null
        effLoadOk.value = !!r
        if (r && !effBind.value) promptConfigure(code)
    })
}

/** 部门变更即重新核对；清空时直接归零，不发无意义请求 */
watch(deptCode, code => {
    effBind.value = null
    effLoadOk.value = true
    loadEffective(code)
}, {immediate: true})

/**
 * 绑定状态，四态：
 *   idle     还没选定部门（没什么可核对的）；或核对请求失败（不给结论）
 *   checking 正在核对
 *   unbound  该部门没有配置模板 → 无法导入，必须先去配置
 *   ready    该部门认下了某套模板 → 放行（这套模板本身能不能导另由 tplBlockMsg 判）
 */
const bindState = computed(() => {
    if (!deptCode.value) return 'idle'
    if (effLoading.value) return 'checking'
    if (!effLoadOk.value) return 'idle'
    if (!curTpl.value) return 'unbound'
    return 'ready'
})

/**
 * 「该部门没有合同模板」的弹窗：不做静默处理 —— 用户点了个部门却什么都不发生，
 * 只会让他反复点、或者干脆去猜为什么不给传文件。
 * 有配置权限的人给一个直达出口；没有的人只能被告知去找管理员（见 canAssign）。
 */
function promptConfigure(code) {
    const head = `部门「${deptPath(code)}」还没有配置合同模板，因此无法导入数据。`
    if (!canAssign) {
        // 没有 perm:assign 的人点了「前往配置」只会被路由守卫弹回来，索性不给这个按钮
        ElMessageBox.alert(
            head + '请联系管理员到「部门合同模板」页为该部门指定模板。',
            '该部门没有合同模板',
            {type: 'warning', confirmButtonText: '知道了'}
        ).catch(() => {
        })
        return
    }
    ElMessageBox.confirm(
        head + '请先到「部门合同模板」页为它指定模板。',
        '该部门没有合同模板',
        {type: 'warning', confirmButtonText: '前往配置模板', cancelButtonText: '知道了'}
    ).then(() => goDeptTpl()).catch(() => {
    })
}

function goDeptTpl() {
    router.push({name: 'home_deptTpl'})
}

const AC_import = new AbortController()
const AC_dept = new AbortController()

onMounted(() => {
    loadDepts()
})

onUnmounted(() => {
    AC_import.abort()
    AC_dept.abort()
    AC_eff.abort()
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
 * 上传前置闸门：归属部门 → 核对中 → 核对失败 → 该部门没配模板 → 模板本身不可用。
 * 顺序即步骤顺序（先说最早缺的那个），避免用户补完一个又撞下一个。
 */
function uploadGate() {
    if (!deptCode.value) {
        ElMessage.warning('请先在左侧选择归属部门，再上传文件')
        return false
    }
    if (bindState.value === 'checking') {
        ElMessage.warning('正在核对该部门的合同模板，请稍候再试')
        return false
    }
    if (bindState.value === 'idle' && !effLoadOk.value) {
        ElMessage.warning('部门合同模板核对失败，请稍后重试或联系管理员')
        return false
    }
    if (bindState.value === 'unbound') {
        ElMessage.error(`部门「${deptPath(deptCode.value)}」没有配置合同模板，无法导入数据；` +
            `请先到「部门合同模板」页为该部门指定模板`)
        return false
    }
    if (tplBlockMsg.value) {
        ElMessage.error(tplBlockMsg.value)
        return false
    }
    return true
}

/**
 * 点击上传区：闸门不过就连文件选择框都不打开 ——
 * 先过闸再选文件，能避免用户选完文件才被拦（那一趟解析已经白跑）。
 */
function onUploadClick() {
    if (!uploadGate()) return
    fileInput.value.click()
}

function onFileChange(e) {
    const f = e.target.files[0]
    if (!f) return
    handleFile(f)
}

function onDrop(e) {
    // 先把文件取出来：下面可能提前 return，而 DataTransfer 在事件回调返回后会被清空
    const f = e.dataTransfer.files[0]
    if (!uploadGate()) return
    if (f) handleFile(f)
}

async function handleFile(f) {
    if (!uploadGate()) return
    if (!/\.(xlsx|xls)$/i.test(f.name)) {
        ElMessage.error('仅支持 .xlsx / .xls 文件')
        return
    }
    file.value = f
    importing.value = true
    try {
        // 列按"该部门生效模板指向的物理表"解析：不同模板列不同，解析口径必须跟着模板走
        const rows = await parseContractExcel(f, curTpl.value.tb_name)
        if (rows.length === 0) {
            ElMessage.warning('文件中没有可导入的数据')
            importing.value = false
            return
        }
        // 整批统一打上所选归属部门（后端逐行强校验：缺部门、部门非法、或超出数据范围都按行拦截）。
        rows.forEach(r => {
            r.dept_code = deptCode.value
        })
        // tpl_id 随 params 一起传：它声明"这批数据属于哪套模版"，值来自该部门的生效绑定。
        // 前端选的模版不是权威 —— 后端会拿它跟部门绑定比对，对不上就逐行拦下，
        // 所以这里即使传错，也换不来一次"落错表"的成功导入。
        Singleton.getInstance(SysX).importContractExcel(rows, {tpl_id: curTpl.value.id}, AC_import.signal, () => {
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
            <div class="head-desc">
                选择归属部门（该部门须已配置合同模板）→ 上传文件 → 查看导入结果；模板的预览与下载在「部门合同模板」页
                （支持 .xlsx / .xls，单次最多 1000 行）
            </div>
        </div>

        <!-- ①② 归属部门 + 上传文件：左树右传（与账号管理页同构），左树默认全展开 -->
        <div class="import-body">
            <!-- 左侧：常驻组织架构。点部门即选定本批合同的归属部门（与账号页一样，点一次即生效） -->
            <el-card shadow="never" class="dept-aside">
                <div class="aside-head">
                    <span class="aside-title">① 归属部门（必填）</span>
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
                        default-expand-all
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
                        <!-- 一层一行：部门所处层级 = 展示行数 = 该行高度（不再固定折两行截断） -->
                        <b v-if="deptCode" :title="deptPath(deptCode)"><span v-for="(seg, i) in deptPathSegments" :key="i" class="picked-seg">{{ seg }}<i v-if="i < deptPathSegments.length - 1" class="picked-slash">/</i></span></b><span v-else class="picked-empty">未选择</span>
                    </div>
                    <!-- 选定部门后，这里给出"这个部门用的哪套模板"的结论：绿=配置到位可导、红=没配置不能导 -->
                    <div v-if="deptCode" class="bind-line" :class="bindState">
                        <template v-if="bindState === 'checking'">正在核对该部门的合同模板…</template>
                        <template v-else-if="bindState === 'unbound'">
                            该部门没有配置合同模板 → 无法导入
                            <span v-if="canAssign" class="bind-link" @click="goDeptTpl">前往配置 →</span>
                        </template>
                        <template v-else-if="bindState === 'ready'">
                            该部门模板校验通过（本部门已配置）
                        </template>
                    </div>
                    <div v-if="!scopeAll" class="scope-line"
                         title="你的账号只能把合同导入到数据范围内的部门。需要更大范围请联系管理员调整数据范围。">
                        数据范围：{{ scopeText(dataScope) }}
                    </div>
                </div>
            </el-card>

            <!-- 右侧：上传区。未选定部门、或该部门没有模板时禁用（点击/拖拽都会给出明确提示） -->
            <el-card shadow="never" class="upload-card">
                <div class="block-title">② 上传文件</div>
                <div class="upload-zone"
                     :class="{dragging: importing, disabled: !deptCode || bindState !== 'ready' || !!tplBlockMsg}"
                     @click="onUploadClick"
                     @dragover.prevent="importing = true" @dragleave.prevent="importing = false" @drop.prevent="onDrop">
                    <div class="uic">📂</div>
                    <div class="u-main">将 Excel 文件拖拽到此处，或 <b>点击选择文件</b></div>
                    <div class="u-sub">支持 .xlsx / .xls，单次最多 1000 行；导入前将进行必填、格式、编号唯一性校验</div>
                    <input ref="fileInput" type="file" accept=".xlsx,.xls" style="display:none" @change="onFileChange"/>
                </div>
                <div v-if="!deptCode" class="warn-tip">请先在左侧选择归属部门</div>
                <div v-else-if="bindState === 'checking'" class="warn-tip">正在核对该部门的合同模板…</div>
                <div v-else-if="bindState === 'idle' && !effLoadOk" class="warn-tip danger">
                    ⛔ 部门合同模板核对失败，请稍后重试或联系管理员
                </div>
                <div v-else-if="bindState === 'unbound'" class="warn-tip danger">
                    ⛔ 该部门没有配置合同模板，无法导入
                    <span v-if="canAssign" class="bind-link" @click="goDeptTpl">前往「部门合同模板」配置 →</span>
                    <span v-else>请联系管理员到「部门合同模板」页为该部门指定模板</span>
                </div>
                <div v-else-if="tplBlockMsg" class="warn-tip danger">⛔ {{ tplBlockMsg }}</div>
                <div v-else-if="bindState === 'ready'" class="warn-tip ok">
                    ✅ 模板校验通过，本次将写入 {{ curTpl.tb_name }}
                </div>
                <div v-if="importing" class="importing-tip">
                    <el-icon class="is-loading" style="margin-right:6px"><i class="el-icon-loading"/></el-icon>
                    正在解析并校验...
                </div>
                <div class="tip-text">本批合同将统一归属到左侧选中的部门。合同模板由该部门决定（在「部门合同模板」页配置），本页不能改；模板的预览与下载也在那一页。可在左侧搜索框按部门名称或编码模糊匹配。</div>
            </el-card>
        </div>

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

    /* ③ 仍是纵向堆叠的整块卡片 */
    .block-card {
        margin-bottom: 16px;
    }

    /* 区块标题：②③ 共用 */
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

        /* 结论沿用同一套语义色：绿=配置到位可放行、红=已阻断 */
        &.ok {
            color: #16a34a;
        }

        &.danger {
            color: #dc2626;
            font-weight: 600;
        }
    }

    /* 被阻断时的一键出口：不让用户对着红条干瞪眼 */
    .bind-link {
        margin-left: 6px;
        color: #2563eb;
        cursor: pointer;
        font-weight: 400;
        text-decoration: underline;
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

        /* 未选归属部门 / 部门没配模板：视觉上提示不可用（点击仍会给出明确提示） */
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

    /* ① + ②：左树右传，两栏等高（左侧定宽、右侧吃掉剩余宽度）。
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

                    /* 一行一层：部门在第几层就展示几行，高度随层级自适应（不写死行数）。
                       单层名字过长只在本行内省略，完整路径另挂 title 悬浮可看全。 */
                    b {
                        flex: 1 1 auto;
                        min-width: 0;
                        display: block;
                        line-height: 1.45;
                        color: #2563eb;

                        .picked-seg {
                            display: block;
                            overflow: hidden;
                            white-space: nowrap;
                            text-overflow: ellipsis;
                        }

                        .picked-slash {
                            margin-left: 1px;
                            font-style: normal;
                            color: #94a3b8;
                        }
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

                /* 该部门用的哪套模板（紧贴部门信息，就地回答"能不能导"） */
                .bind-line {
                    margin-top: 6px;
                    line-height: 1.5;

                    &.checking {
                        color: #94a3b8;
                    }

                    &.unbound {
                        color: #dc2626;
                    }

                    &.ready {
                        color: #16a34a;
                    }
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
