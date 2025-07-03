<script setup>
import {SysX} from "@/cms/smlj/cggy/system/SysX.js"
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

            router.push({name: 'homeRouter'})
        }
        else {
            console.log("<UNK>");
        }
    });
}

</script>

<template>
    <div class="login-box">
        <img src="@/assets/jthx-all.png" class="login-image" alt="">

        <div class="login-content">
            <div class="login-view">
                <div style="margin-top: 24px; display: flex; padding: 0 48px;">
                    <el-divider>
                        <span style="font-size: 20px; font-weight: bolder;">{{ '登 录' }}</span>
                    </el-divider>
                </div>

                <div style="margin-left: 32px; margin-top: 24px; margin-right: 32px;">
                    <el-form :model="loginForm" :rules="loginRules" class="login-form">
                        <el-form-item prop="account">
                            <el-input v-model="loginForm.account" placeholder="请输入账号">

                            </el-input>
                        </el-form-item>

                        <el-form-item prop="pwd">
                            <el-input v-model="loginForm.pwd" placeholder="请输入密码" show-password>

                            </el-input>
                        </el-form-item>
                    </el-form>
                    <el-button type="primary" style="width: 100%; margin-bottom: 24px;" @click="loginAction" :loading="loading">{{ loading ? "登 录 中..." : "登 录" }}</el-button>
                </div>
            </div>
        </div>
    </div>
</template>

<style lang="scss" scoped>
.login-box {
    width: 100%;
    height: 100%;
    position: relative;
    background-size: cover;
    background-repeat: no-repeat;
    background-position: left;
    overflow-x: auto;

    .login-image {
        margin-top: 36px;
        float: left;
        object-fit: contain;

        @media screen and (min-width: 993px) {
            width: 600px;
            margin-left: 40px;
        }

        @media screen and (max-width: 992px) {
            width: 100%;
        }
    }

    .login-content {
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        position: fixed;

        .login-view {
            height: 420px;
            margin-top: 180px;
            float: right;
            background-color: #ffffff;
            background-size: 100% 150px;
            background-repeat: no-repeat;
            background-position: bottom;
            border-radius: 6px;
            box-shadow: 0 10px 20px rgba(0, 0, 0, 0.4);

            @media screen and (min-width: 993px) {
                margin-right: 25%;
                width: 420px;
            }

            @media screen and (max-width: 992px) {
                margin-right: 24px;
                width: calc(100% - 48px);
            }

            .el-input__inner {
                height: 48px;
                padding-left: 42px;
                font-size: 16px;
            }

            .el-form-item__error {
                font-size: 12px;
            }

            .el-input__prefix {
                width: 42px;
                height: 48px;
                display: flex;
                justify-content: center;
                align-items: center;
            }

            .el-button {
                height: 42px;
                font-size: 16px;
            }

            .el-form-item {
                margin-bottom: 24px;
            }
        }
    }

    .footer-container {
        position: absolute;
        z-index: 0;
        bottom: 0;
        width: 100%;
        padding: 0px 8px 12px 8px;

        .copyright-style {
            font-size: 10px;
            color: #f5f5f5;
            display: flex;
            justify-content: center;
            align-items: center;
            text-align: center;
        }
    }

    span {
        white-space: pre-wrap;
        word-wrap: break-word;
        word-break: break-all;
        line-height: 1.6;
        overflow: hidden;
    }
}
</style>