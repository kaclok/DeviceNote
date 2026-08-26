<script setup lang="js">
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";
import {useRouter} from 'vue-router';
import {ElMessage} from "element-plus";
import {ApiLogin} from "@/cms/smlj/cghtz/api/ApiLogin.js";
import {preloadDictCache} from "@/cms/smlj/cghtz/system/SysX.js";

const {wsCache} = useSessionCache()

// 获取路由实例
const router = useRouter();

let loginForm = ref({
    account: '',
    pwd: '',
})
let loginRules = {
    account: [{required: true, trigger: 'blur', message: '请输入登录账号'}],
    pwd: [{required: true, trigger: 'blur', message: '请输入登录密码'}],
}

let loading = ref(false)

let AC_loginList = new AbortController();

onUnmounted(() => {
    AC_loginList.abort();
});

function loginAction() {
    ApiLogin.login({account: loginForm.value.account, pwd: loginForm.value.pwd}, AC_loginList.signal, () => {
        loading.value = true;
    }, (r, data) => {
        loading.value = false;
        if (r && data.data) {
            // 保存登录信息与权限（wsCache 会自动序列化，直接存对象/数组）
            const acc = data.data.user;
            wsCache.set(ECacheType.ACCOUNT, acc);

            const roles = Array.isArray(acc.role) ? acc.role : [acc.role]
            const allPerms = roles.flatMap(role => role.perms);
            wsCache.set(ECacheType.ALL_PERMS, [...new Set(allPerms)]);

            ElMessage.success(`欢迎回来，${acc.account}`)
            // 预加载签订人/权限字典缓存，不传 login 页的 signal，避免页面卸载 abort 掉请求
            preloadDictCache()
            router.push({name: 'home'})
        } else {
            ElMessage.error(data?.data?.message || data?.msg || '登录失败')
        }
    });
}

// 快捷填充演示账号
function fillAccount(account, pwd) {
    loginForm.value.account = account
    loginForm.value.pwd = pwd
}
</script>

<template>
    <div class="login-page">
        <div class="login-card">
            <div class="login-header">
                <div class="logo">📑</div>
                <div class="title">合同台账管理系统</div>
                <div class="subtitle">陕西金泰化学神木氯碱有限公司</div>
            </div>

            <el-form :model="loginForm" :rules="loginRules" class="login-form" @keyup.enter="loginAction">
                <el-form-item prop="account">
                    <el-input v-model="loginForm.account" placeholder="请输入账号" size="large" clearable>
                        <template #prefix><span style="color:#94a3b8">👤</span></template>
                    </el-input>
                </el-form-item>
                <el-form-item prop="pwd">
                    <el-input v-model="loginForm.pwd" placeholder="请输入密码" show-password size="large" clearable>
                        <template #prefix><span style="color:#94a3b8">🔒</span></template>
                    </el-input>
                </el-form-item>
                <el-button type="primary" size="large" style="width:100%;height:44px;font-size:16px;font-weight:600"
                           @click="loginAction" :loading="loading">
                    {{ loading ? "登 录 中..." : "登 录" }}
                </el-button>
            </el-form>
        </div>
    </div>
</template>

<style lang="scss" scoped>
.login-page {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 55%, #2563eb 100%);

    .login-card {
        width: 420px;
        background: #ffffff;
        border-radius: 16px;
        padding: 36px 34px 24px;
        box-shadow: 0 24px 64px rgba(0, 0, 0, 0.35);

        .login-header {
            text-align: center;
            margin-bottom: 28px;

            .logo {
                width: 60px;
                height: 60px;
                margin: 0 auto 12px;
                border-radius: 15px;
                background: linear-gradient(135deg, #2563eb, #60a5fa);
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 30px;
                box-shadow: 0 8px 20px rgba(37, 99, 235, 0.35);
            }

            .title {
                font-size: 20px;
                font-weight: 700;
                letter-spacing: 1px;
                color: #0f172a;
            }

            .subtitle {
                font-size: 12px;
                color: #94a3b8;
                margin-top: 6px;
            }
        }

        .demo-accounts {
            margin-top: 22px;
            padding-top: 16px;
            border-top: 1px dashed #e2e8f0;

            .demo-title {
                font-size: 12px;
                color: #94a3b8;
                margin-bottom: 10px;
            }

            .demo-btns {
                display: flex;
                gap: 8px;
                flex-wrap: wrap;

                .demo-tag {
                    cursor: pointer;
                    transition: transform .15s;

                    &:hover {
                        transform: translateY(-1px);
                    }
                }
            }

            .demo-hint {
                font-size: 11px;
                color: #cbd5e1;
                margin-top: 10px;
            }
        }
    }
}
</style>
