<template>
    <div class="container">
        <h2> AI: </h2>
        <div class="grid-container">
            <div
                v-for="item in filterItems(aiGridItems)"
                :key="item.name"
                class="grid-item"
                @click="item.url ? onClickedImg(item.url) : undefined"
            >
                <img :src="item.image" :alt="item.name" class="icon">
                <div class="label">{{ item.name }}</div>
            </div>
        </div>

        <h2 style="position: relative; top: 10px"> App: </h2>
        <div class="grid-container">
            <div
                v-for="item in filterItems(appGridItems)"
                :key="item.name"
                class="grid-item"
                @click="item.url ? onClickedImg(item.url) : undefined"
            >
                <img :src="item.image" :alt="item.name" class="icon">
                <div style="white-space: pre-line; text-align: center" class="label">{{ item.name }}</div>
            </div>
        </div>
    </div>
</template>

<script setup>
import logo_jt from '@/assets/image/logo/jt.png';
import logo_kimi from '@/assets/image/logo/kimi.svg';
import logo_deepseek from '@/assets/image/logo/deepseek.png';
import logo_tyqw from '@/assets/image/logo/tyqw.png';
import logo_wxyy from '@/assets/image/logo/wxyy.png';
import logo_chatgpt from '@/assets/image/logo/chatgpt.png';
import logo_gemini from '@/assets/image/logo/gemini.svg';
import logo_claude from '@/assets/image/logo/claude.png';
import logo_grok from '@/assets/image/logo/grok.jpg';
import logo_lx from '@/assets/image/logo/wps-lx.jpg';
import logo_db from '@/assets/image/logo/db.png';
import logo_txyb from '@/assets/image/logo/txyb.jpg';

import logo_book from '@/assets/image/answer_marking.png';

import {computed, ref} from 'vue';
import {useTitle} from '@vueuse/core'
import {WsService} from "@/framework/services/net/webSocket/WsService.js";
import {Ws} from "@/framework/services/net/webSocket/Ws.js";
/*
import axios from "axios";
import {SessionStorageService} from "@/framework/services/SessionStorageService.js";

let account = SessionStorageService.getStore("Account");
console.log("account: " + account);
if (account === null) {
    // window.location.href
    let params = new URLSearchParams(window.location.search);
    let authCode = params.get('code');

    onGotAuthCode(authCode, onGotToken);
}

function onGotAuthCode(authCode, onSuccess) {
    axios.post("http://10.8.54.110:8790/auth/token", {}, {
        params: {
            code: authCode,
            grant_type: "authorization_code",
            client_id: "aiEntry",
            redirect_uri: "http://10.8.54.127:5175"
        }
    }).then((response) => {
        console.table(response.data);
        if (response.data.status === 0) {
            onSuccess(response);
        }
    })
}

function onGotToken(r) {
    let headers = {
        'Content-Type': 'application/json',
        "Authorization": `${r.data.token_type} ${r.data.access_token}`
    }

    axios.post("http://10.8.54.110:8790/auth/userinfo/v2", {}, {headers: headers}).then((response) => {
        account = response.data.data.account;

        sessionStorage.setItem("Account", account);
        console.log("onGotToken: " + account);
    })
}*/

let params = new URLSearchParams(window.location.search);
let group = params.get('group');
group = group === null ? "1" : group.toString();

const title = useTitle()
title.value = '入口'

const filterItems = computed(() => {
    return (items) => {
        // 使用 param 进行计算
        return items.filter((item) => {
            return item.enabled;
        });
    };
});

// You can add reactive state and methods here if needed
const aiGridItems = ref([
    {
        name: "金泰Ai助手",
        url: "http://10.8.13.54:8080",
        image: logo_jt,
        enabled: true,
    },
    {
        name: "豆包",
        url: "https://www.doubao.com/chat/",
        image: logo_db,
        enabled: true,
    },
    {
        name: "Deepseek",
        url: "https://chat.deepseek.com",
        image: logo_deepseek,
        enabled: true,
    },
    {
        name: "Kimi",
        url: "https://kimi.moonshot.cn",
        image: logo_kimi,
        enabled: true,
    },
    {
        name: "Wps",
        url: "https://lingxi.wps.cn/",
        image: logo_lx,
        enabled: true,
    },
    {
        name: "腾讯元宝",
        url: "https://yuanbao.tencent.com/chat",
        image: logo_txyb,
        enabled: true,
    },
    {
        name: "通义千问",
        url: "https://tongyi.aliyun.com",
        image: logo_tyqw,
        enabled: true,
    },
    {
        name: "文心一言",
        url: "https://yiyan.baidu.com",
        image: logo_wxyy,
        enabled: true,
    },
    {
        name: "chatgpt",
        url: "https://chatgpt.com",
        image: logo_chatgpt,
        enabled: true,
    },
    {
        name: "gemini",
        url: "https://gemini.google.com/app?hl=zh-cn",
        image: logo_gemini,
        enabled: true,
    },
    {
        name: "claude",
        url: "https://claude.ai/new",
        image: logo_claude,
        enabled: true,
    },
    {
        name: "grok",
        url: "https://grok.com",
        image: logo_grok,
        enabled: true,
    }
])

const appGridItems = ref([
    /*{
        name: "生产记事",
        url: "http://10.8.54.26/chr/index/index",
        image: logo_book,
        enabled: false,
    },
    {
        name: "转化器系统",
        url: "http://10.8.54.26/cms/index/index",
        image: logo_book,
        enabled: false,
    },*/
    {
        name: "员工培训平台",
        url: "http://117.36.227.42:8082?group=" + group,
        image: logo_book,
        enabled: true,
    },
    {
        name: "日报",
        url: "http://10.8.54.110:8790/auth/authorize?response_type=code&scope=openid&client_id=dailypaper&redirect_uri=http://10.8.54.127:5175",
        image: logo_book,
        enabled: true,
    },
    {
        name: "作业票统计",
        url: "http://10.8.54.161:8085/#/jobTicket",
        image: logo_book,
        enabled: true,
    },
    {
        name: "设备检修记录\n(手机用)",
        url: "http://117.36.227.42:4175?group=" + group, // http://10.8.54.24:4177/pages/sb/index.html",
        image: logo_book,
        enabled: true,
    },
    {
        name: "仪表检修记录",
        url: "http://117.36.227.42:4177/pages/yb/index.html?group=" + group,
        image: logo_book,
        enabled: true,
    },
    {
        name: "电气检修记录",
        url: "http://117.36.227.42:4177/pages/dq/index.html?group=" + group,
        image: logo_book,
        enabled: true,
    },
    {
        name: "物资库存记录",
        url: "http://117.36.227.42:4177/pages/wz/index.html?group=" + group,
        image: logo_book,
        enabled: true,
    },
    {
        name: "异常工况预警",
        url: "http://117.36.227.42:4177/pages/smds/watchdog/index.html?group=" + group,
        image: logo_book,
        enabled: true,
    },
])

// Example of a method to handle clicks on grid items
function onClickedImg(url) {
    if (url) {
        window.open(url, '_blank');
    }
}

/*let ws1 = new WsService("ws://localhost:7092/ws")
ws.connect()*/

/*let ws2 = new Ws("ws://localhost:7092/ws?account=029567")
ws2.connect()*/

</script>

<style scoped>
.container {
    /*background: url("@/assets/image/infinity-6223003.jpg") no-repeat center;*/

    max-width: 1400px;
    margin: 0 auto;
    padding: 20px;
    background-color: white;
    border-radius: 20px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.grid-container {
    display: grid;
    grid-template-columns: repeat(8, 1fr);
    gap: 10px;
}

.grid-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px 0;
    border-right: 1px solid #eee;
    border-bottom: 1px solid #eee;
    cursor: pointer;
    transition: background-color 0.2s;
}

.grid-item:hover {
    background-color: #f5f5f5;
}

.icon {
    width: 36px;
    height: 36px;
    margin-bottom: 10px;
}

.add-icon {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    color: #999;
    margin-bottom: 10px;
}

.label {
    font-size: 14px;
    color: #666;
}

/* Media query for mobile devices */
@media (max-width: 600px) {
    .grid-container {
        grid-template-columns: repeat(2, 1fr);
    }
}
</style>