<script setup lang="js">
import {useRoute, useRouter} from 'vue-router';
import {clearAccount, ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";
import {ElMessage, ElMessageBox} from "element-plus";
import {ApiLogin} from "@/cms/smlj/cghtz/api/ApiLogin.js";
import {clearDictCache} from "@/cms/smlj/cghtz/system/SysX.js";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const router = useRouter();
const route = useRoute();
const {wsCache} = useSessionCache()

// 空值兜底：未登录或缓存被清时避免 .includes / .username 报错
const acc = wsCache.get(ECacheType.ACCOUNT);
const account = ref(acc)
const perms = ref(acc.role.perms)

let loadingLogout = ref(false)

let AC_logoutList = new AbortController();
const AC_pwd = new AbortController()

onUnmounted(() => {
    AC_logoutList.abort();
    AC_pwd.abort();
});

// 权限判断
function hasPerm(code) {
    return perms.value.includes(code)
}

// 侧边菜单（按权限过滤）
const menus = computed(() => {
    const list = [
        {path: '/home/ledger', title: '合同台账', icon: '📋', perm: 'contract:view'},
        // 菜单顺序 = 实际链路顺序：部门得先有模板才导得进去（导入页会拦），
        // 所以「部门合同模板」排在「批量导入」前面，别让用户先撞一次失败再回头找配置页。
        {path: '/home/deptTpl', title: '合同模板', icon: '🔗', perm: 'perm:assign'},
        {path: '/home/import', title: '批量导入', icon: '📥', perm: 'contract:import'},
        {path: '/home/users', title: '账号权限', icon: '👥', perm: 'perm:assign'},
    ]
    return list.filter(m => hasPerm(m.perm))
})

// 当前激活菜单：响应式绑定 route.path，路由变化时菜单高亮自动跟随
const activeMenu = computed(() => route.path)

function logout() {
    ElMessageBox.confirm('确定退出登录吗？', '提示', {type: 'warning'}).then(() => {
        trueLogout()
    }).catch(() => {
    })
}

function trueLogout() {
    ApiLogin.logout({account: account.value.account}, AC_logoutList.signal, () => {
        loadingLogout.value = true;
    }, (r, data) => {
        loadingLogout.value = false;
        if (r && data.data) {
            clearDictCache()
            clearAccount()
            router.push({name: 'login'})
        } else {
            notifyError(data, '登出失败')
        }
    });
}

// 右上角下拉：按 command 分发（修改密码 / 退出登录）
function onUserCommand(cmd) {
    if (cmd === 'changePwd') openChangePwd()
    else if (cmd === 'logout') logout()
}

/* ---------------- 修改密码（用户自助） ---------------- */
// 管理员新建账号时写入的是初始密码，用户可在此凭原密码自行修改。
// 是否"仍是初始密码"由后端在登录 / account/me 响应里给一个 initPwd 布尔值。
// 原来读的是 account.pwd —— 但 pwd 上有 @JsonIgnore，响应体里根本不会出现该字段，
// 所以这条提醒其实从来没生效过；token 里也不再放用户信息，改由后端算好布尔值下发。
const isInitPwd = computed(() => account.value?.initPwd === true)

const pwdDialogVisible = ref(false)
const pwdFormRef = ref()
const pwdSaving = ref(false)
const pwdForm = ref({oldPwd: '', newPwd: '', confirmPwd: ''})

const pwdRules = {
    oldPwd: [{required: true, message: '请输入原密码', trigger: 'blur'}],
    newPwd: [
        {required: true, message: '请输入新密码', trigger: 'blur'},
        {min: 6, max: 20, message: '新密码长度需为 6~20 位', trigger: 'blur'},
        {
            validator: (rule, value, callback) => {
                if (value && value === pwdForm.value.oldPwd) callback(new Error('新密码不能与原密码相同'))
                else callback()
            }, trigger: 'blur'
        },
    ],
    confirmPwd: [
        {required: true, message: '请再次输入新密码', trigger: 'blur'},
        {
            validator: (rule, value, callback) => {
                if (value !== pwdForm.value.newPwd) callback(new Error('两次输入的新密码不一致'))
                else callback()
            }, trigger: 'blur'
        },
    ],
}

function openChangePwd() {
    pwdForm.value = {oldPwd: '', newPwd: '', confirmPwd: ''}
    pwdDialogVisible.value = true
}

function submitChangePwd() {
    pwdFormRef.value.validate(valid => {
        if (!valid) return
        pwdSaving.value = true
        // account 不传：后端从请求头 at(JWT) 解析当前登录账号
        ApiLogin.changePwd({oldPwd: pwdForm.value.oldPwd, newPwd: pwdForm.value.newPwd}, AC_pwd.signal, () => {
        }, (r, data) => {
            pwdSaving.value = false
            if (r) {
                pwdDialogVisible.value = false
                ElMessage.success('密码修改成功，请使用新密码重新登录')
                // 密码已变更：清掉登录态与字典缓存，回到登录页重新认证
                clearDictCache()
                clearAccount()
                router.push({name: 'login'})
            } else {
                notifyError(data, '密码修改失败')
            }
        })
    })
}
</script>

<template>
    <div class="page-container">
        <!-- 顶栏 -->
        <div class="page-title">
            <div class="logo">📑</div>
            <span class="page-title-content">合同台账管理系统</span>

            <div class="page-title-sub">陕西金泰化学神木氯碱</div>

            <div class="right-menu">
                <el-dropdown @command="onUserCommand">
                    <div class="user-info">
                        <el-avatar :size="30" style="background:#6366f1;font-size:13px">
                            {{ (account.username || account.account || '?').slice(0, 1) }}
                        </el-avatar>
                        <span class="user-name">{{ account.account }}</span>
                        <el-tag v-if="isInitPwd" size="small" type="warning" effect="light" class="pwd-warn"
                                @click.stop="openChangePwd">初始密码
                        </el-tag>
                    </div>
                    <template #dropdown>
                        <el-dropdown-menu>
                            <el-dropdown-item command="changePwd">修改密码</el-dropdown-item>
                            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                        </el-dropdown-menu>
                    </template>
                </el-dropdown>
            </div>
        </div>

        <!-- 主体：侧边菜单 + 内容 -->
        <div class="page-body">
            <div class="sidebar">
                <el-menu :default-active="activeMenu" router background-color="#0f172a"
                         text-color="#94a3b8" active-text-color="#ffffff" :unique-opened="true">
                    <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
                        <span class="menu-icon">{{ m.icon }}</span>
                        <span>{{ m.title }}</span>
                    </el-menu-item>
                </el-menu>
                <div class="sidebar-footer">v0.1.0</div>
            </div>

            <div class="page-content">
                <router-view/>
            </div>
        </div>

        <!-- 修改密码（用户自助） -->
        <el-dialog v-model="pwdDialogVisible" title="修改密码" width="440px" destroy-on-close>
            <el-alert v-if="isInitPwd" type="warning" :closable="false" show-icon
                      title="当前使用的是管理员设置的初始密码，建议尽快修改"
                      style="margin-bottom:14px"/>
            <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="80px">
                <el-form-item label="账号">
                    <el-input :model-value="account.account" disabled/>
                </el-form-item>
                <el-form-item label="原密码" prop="oldPwd">
                    <el-input v-model="pwdForm.oldPwd" type="password" show-password
                              placeholder="请输入原密码" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="新密码" prop="newPwd">
                    <el-input v-model="pwdForm.newPwd" type="password" show-password
                              placeholder="6~20 位" autocomplete="off"/>
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPwd">
                    <el-input v-model="pwdForm.confirmPwd" type="password" show-password
                              placeholder="请再次输入新密码" autocomplete="off"/>
                </el-form-item>
            </el-form>
            <div class="pwd-tip">修改成功后需使用新密码重新登录</div>
            <template #footer>
                <el-button @click="pwdDialogVisible = false">取消</el-button>
                <el-button type="primary" :loading="pwdSaving" @click="submitChangePwd">确定修改</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<style lang="scss" scoped>
.page-container {
    width: 100%;
    height: 100%;
    box-sizing: border-box;
    background-color: #f1f5f9;
    display: flex;
    flex-direction: column;
    overflow: hidden;

    .page-title {
        box-sizing: border-box;
        height: 56px;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        background-color: #ffffff;
        border-bottom: 1px solid #e2e8f0;
        padding: 0 20px;

        .logo {
            font-size: 22px;
            margin-right: 10px;
        }

        .page-title-content {
            color: #0f172a;
            font-size: 17px;
            font-weight: 700;
        }

        .page-title-sub {
            margin-left: 16px;
            padding-left: 16px;
            border-left: 1px solid #e2e8f0;
            color: #94a3b8;
            font-size: 12px;
        }

        .right-menu {
            margin-left: auto;
            display: flex;
            align-items: center;
            gap: 18px;

            .user-info {
                display: flex;
                align-items: center;
                gap: 8px;
                cursor: pointer;
                padding: 4px 8px;
                border-radius: 8px;
                transition: background .2s;

                &:hover {
                    background: #f8fafc;
                }

                .user-name {
                    font-size: 14px;
                    font-weight: 500;
                }

                .pwd-warn {
                    cursor: pointer;
                    font-size: 11px;
                }

                .user-role {
                    font-size: 11px;
                    color: #2563eb;
                    background: #eff6ff;
                    padding: 1px 8px;
                    border-radius: 999px;
                }
            }
        }
    }

    .page-body {
        flex: 1;
        display: flex;
        overflow: hidden;
        box-sizing: border-box;
        min-height: 0;

        .sidebar {
            /* 宽度只需容纳最长菜单项（当前 4 个汉字，菜单名换来换去，这里不写死）。
               空间是从菜单内距里省出来的，不是靠把侧栏撑宽：
               .el-menu-item 外距收到 4px、内距收到 10px（见下），选中态的蓝色渐变底因此能完整包住文字。
               ⚠️ 菜单名一改长就要回来重算宽度 —— 跑 .workbuddy/_verify_menu_geometry.py，
               它直接从本文件解析宽度与菜单名，再用无头 Chrome + 真实 element-plus CSS 实量，不会和源码脱节。 */
            width: 125px;
            flex-shrink: 0;
            box-sizing: border-box;
            background-color: #0f172a;
            display: flex;
            flex-direction: column;

            :deep(.el-menu) {
                border-right: none;
                flex: 1;

                .el-menu-item {
                    height: 46px;
                    /* 外距 8px -> 4px、内距 20px（element-plus 默认）-> 10px：
                       菜单到侧栏左右两侧的留白各收窄 14px，选中态的渐变底相应铺宽 */
                    margin: 2px 4px;
                    padding: 0 10px;
                    border-radius: 8px;
                    /* 菜单名不折行：折行会撑破 46px 行高，也会让选中底形状变形 */
                    white-space: nowrap;

                    &.is-active {
                        background: linear-gradient(90deg, #2563eb, #3b82f6) !important;
                    }

                    .menu-icon {
                        margin-right: 10px;
                        font-size: 15px;
                    }
                }
            }

            .sidebar-footer {
                padding: 14px;
                font-size: 11px;
                color: #475569;
                text-align: center;
            }
        }

        .page-content {
            flex: 1;
            box-sizing: border-box;
            overflow-y: auto;
            overflow-x: hidden;
            padding: 20px 24px 40px;
        }
    }

    .pwd-tip {
        font-size: 12px;
        color: #94a3b8;
        padding-left: 80px;
    }
}
</style>
