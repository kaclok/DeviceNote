export class WsService {
    constructor(url) {
        this.url = url;
        this.ws = null;
        this.msgQueue = [];
    }

    connect() {
        if (!window.WebSocket) {
            console.log('当前不支持websocket');
            return;
        }

        if (this.ws !== null && (this.ws.readyState <= WebSocket.OPEN)) {
            return;
        }

        this.ws = new WebSocket(this.url);

        this.ws.onopen = (event) => {
            console.log('ws 连接成功:', event);
        };

        this.ws.onclose = (event) => {
            console.log('ws 连接断开', event);

            this.ws.close();
            this.ws = null;
        };

        this.ws.onmessage = (msgEvent) => {
            console.log("ws 收到服务器消息: " + msgEvent.data);

            this.msgQueue.push(msgEvent.data);
        };

        this.ws.onerror = (event) => {
            console.log('ws 连接错误:', event);
            this.ws.close();
        };
    }

    close() {
        if (this.ws !== null) {
            this.ws.close();
            this.ws = null;

            this.msgQueue = []
        }
    }

    send(msg) {
        if (this.isConnected()) {
            this.ws.send(msg);
        } else {
            console.error('ws 状态错误');
        }
    }

    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN;
    }
}
