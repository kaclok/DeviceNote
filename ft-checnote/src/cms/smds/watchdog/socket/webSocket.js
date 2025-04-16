let ws;
import {branchInfo} from '../store/global.js'
import pinia from '../pinia.js'

const socketInfo = branchInfo(pinia)

export function connectWebSocket() {
    // 建立WebSocket连接
    ws = new WebSocket("ws://10.8.54.244:8080/smds-pre-warning-server-1.0-SNAPSHOT/api/webSocket/hahah");
    ws = new WebSocket("ws://localhost:8091/api/webSocket/029567");
    // ws = new WebSocket("ws://10.10.22.158:8080/api/webSocket/hahah");
    // 当WebSocket连接成功时
    ws.onopen = () => {
        console.log("WebSocket连接已建立");
        ws.send("Hello from the client!"); // 发送测试消息
    };

    // 当收到来自服务端的消息时
    ws.onmessage = (event) => {
        socketInfo.insertSocketInfo(event.data)
        console.log("收到来自服务端的消息:", event.data);
        if (socketInfo.socketInfo.length > 0) {
            console.log("------" + socketInfo.socketInfo[0].message)
        }
    };

    // 当WebSocket连接关闭时
    ws.onclose = () => {
        console.log("WebSocket连接已关闭");
    };

    // 当WebSocket出现错误时
    ws.onerror = (error) => {
        console.error("WebSocket发生错误:", error);
    };
}

// 发送消息到服务端
export function sendMessage(message) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(message);
    } else {
        console.error("WebSocket连接未打开");
    }
}
