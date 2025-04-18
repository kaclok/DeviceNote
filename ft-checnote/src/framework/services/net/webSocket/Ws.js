import {useWebSocket} from '@vueuse/core'

export class Ws {
    constructor(url) {
        this.url = url

        this.status = "CLOSED"
        this.data = null
        this.ws = null
    }

    connect() {
        const {status, data, send, open, close, ws} = useWebSocket(this.url, {
            autoReconnect: {
                retries: 3,
                delay: 1000,
                onFailed() {
                    /*console.error('Failed to connect WebSocket after 3 retries')*/
                    window.alert('Failed to connect WebSocket after 3 retries')
                },
            },
            heartbeat: {
                message: 'ping',
                interval: 5000,
                pongTimeout: 4000,
            },
            onConnected: this.onConnected,
            onDisconnected: this.onDisconnected,
            onMessage: this.onMessage,
            onError: this.onError,
        })

        this.status = status
        this.data = data

        this.open = open
        this.close = close
        this.send = send

        this.ws = ws
    }

    onConnected(ws) {
        console.log("前端 ws 连接成功")
    }

    onDisconnected(ws, event) {
        console.log("前端 ws 断开")
    }

    onError(ws, event) {
        console.log("前端 ws 异常")
        this.close()
    }

    onMessage(ws, event) {
        console.log(event.data)
    }
}