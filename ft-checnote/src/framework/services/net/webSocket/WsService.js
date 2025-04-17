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

        if (this.ws !== null && (this.ws.readyState === WebSocket.CONNECTING || this.ws.readyState === WebSocket.OPEN)) {
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

            /*let json = JSON.parse(msgEvent.data);
            this.msgQueue.push(json);*/
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
        }
    }

    send(msg) {
        if (this.ws !== null && this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(msg);
        }
    }
}
