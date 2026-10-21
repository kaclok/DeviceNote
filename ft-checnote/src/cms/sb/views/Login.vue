<script setup>
import {SysX} from "@/cms/sb/system/SysX.js"
import {TokenService} from '@/framework/services/TokenService.js'
import {Singleton} from "@/framework/services/Singleton.js";
import {LocalStorageService} from '@/framework/services/LocalStorageService.js'
import {useRouter} from 'vue-router';

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
    Singleton.getInstance(SysX).login({account: loginForm.value.account, password: loginForm.value.pwd}, AC_loginList.signal, () => {
        loading.value = true;
    }, (r, data) => {
        loading.value = false;
        if (r) {
            TokenService.setAT(data.data.token.right)
            TokenService.setATIssueAt(data.data.token.left)
            TokenService.setATExpireAt(data.data.token.middle)
            LocalStorageService.setStore("account", data.data.account.account)
            LocalStorageService.setStore("accountAuth", data.data.account.auth)

            router.push({name: 'recordRouter'})
        }
        else {
            console.log("<UNK>");
        }
    });
}

</script>

<template>
    <div class="login-box">
        <div class="login-content">
            <div class="login-view">
                <div class="login-header">
                    <img src="@/assets/st_logo.png" class="login-logo" alt="">
                    <span class="login-title">设备检修记录</span>
                </div>

                <div class="login-form-wrap">
                    <el-form :model="loginForm" :rules="loginRules" class="login-form" @submit.prevent="loginAction">
                        <el-form-item prop="account">
                            <el-input v-model="loginForm.account" placeholder="请输入账号" size="large"/>
                        </el-form-item>

                        <el-form-item prop="pwd">
                            <el-input v-model="loginForm.pwd" placeholder="请输入密码" show-password size="large"
                                      @keyup.enter="loginAction"/>
                        </el-form-item>
                    </el-form>
                    <el-button type="primary" class="login-btn" size="large" @click="loginAction" :loading="loading">
                        {{ loading ? "登 录 中..." : "登 录" }}
                    </el-button>
                </div>
            </div>
        </div>
    </div>
</template>

<style lang="scss" scoped>
$brand: #1C4785;
$brand-light: #2E6BC0;

.login-box {
    width: 100%;
    height: 100vh;
    height: 100dvh;
    position: relative;
    background: linear-gradient(160deg, $brand 0%, $brand-light 55%, #6FA3DC 100%);
    overflow: hidden auto;

    .login-content {
        width: 100%;
        min-height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 24px;
        box-sizing: border-box;

        .login-view {
            width: 100%;
            max-width: 400px;
            background: #ffffff;
            border-radius: 16px;
            padding: 28px 24px 32px;
            box-shadow: 0 16px 40px rgba(0, 20, 60, 0.35);
        }
    }

    .login-header {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 10px;
        margin-bottom: 28px;

        .login-logo {
            width: 56px;
            height: 56px;
            object-fit: contain;
            border-radius: 12px;
            background: #F0F4FA;
            padding: 6px;
            box-sizing: border-box;
        }

        .login-title {
            font-size: 20px;
            font-weight: 600;
            color: #1f2d3d;
            letter-spacing: 2px;
        }
    }

    .login-form-wrap {
        :deep(.el-input__wrapper) {
            border-radius: 10px;
            background: #F5F8FC;
            box-shadow: none;
            padding: 4px 14px;
        }

        .el-form-item {
            margin-bottom: 20px;
        }
    }

    .login-btn {
        width: 100%;
        height: 46px;
        font-size: 16px;
        letter-spacing: 4px;
        border-radius: 10px;
        background: linear-gradient(135deg, $brand 0%, $brand-light 100%);
        border: none;
        margin-top: 4px;
    }
}
</style>
