<script setup lang="js">
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";
import {useRouter} from 'vue-router';
import {ElMessage} from "element-plus";
import {ApiLogin} from "@/cms/smlj/cbs/api/ApiLogin.js";
import {preloadDictCache} from "@/cms/smlj/cbs/system/SysX.js";
import {notifyError} from "@/framework/services/net/NwCodeMap.js"

const {wsCache} = useSessionCache()

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

let AC_login = new AbortController();

onUnmounted(() => {
    AC_login.abort();
});

function loginAction() {
    // cbs 登录是真校验口令的（/cbs/login，BCrypt 比对），响应只带展示字段不含角色；
    // 权限快照由路由守卫进首页前的 ensureMe()（/cbs/me）补全，这里不必等
    ApiLogin.login({account: loginForm.value.account, password: loginForm.value.pwd}, AC_login.signal, () => {
        loading.value = true;
    }, (r, data) => {
        loading.value = false;
        if (r && data.data) {
            wsCache.set(ECacheType.ACCOUNT, data.data);
            ElMessage.success(`欢迎回来，${data.data.username || data.data.account}`)
            // 预加载角色/部门字典缓存，不传 login 页的 signal，避免页面卸载 abort 掉请求
            preloadDictCache()
            const goto = router.currentRoute.value.query?.goto
            if (goto) {
                router.replace(goto)
            } else {
                router.push({name: 'home'})
            }
        } else {
            notifyError(data, '登录失败')
        }
    });
}
</script>

<template>
    <div class="login-page">
        <div class="login-card">
            <div class="login-header">
                <div class="logo">🏗️</div>
                <div class="title">智慧安全生产管控平台</div>
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
    background: linear-gradient(135deg, #0f172a 0%, #134e4a 55%, #0d9488 100%);

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
                background: linear-gradient(135deg, #0d9488, #2dd4bf);
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 30px;
                box-shadow: 0 8px 20px rgba(13, 148, 136, 0.35);
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
    }
}
</style>
