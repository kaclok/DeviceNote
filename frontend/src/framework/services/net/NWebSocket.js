export class NWebSocket {
    constructor() {
        this.socketMap = new WeakMap();
        this.messageCallbacks = {};
        this.closeCallbacks = {};
        this.errorCallbacks = {};
        this.baseUrl = 'ws://10.8.54.113:10112/xboot/ws/bigScreen';
    }

    connect(url, identifier) {
        const socket = new WebSocket(`${this.baseUrl}${url}`);
        socket.onopen = this.onOpen;
        socket.onclose = this.onClose.bind(this, identifier);
        socket.onmessage = this.onMessage.bind(this, identifier);
        socket.onerror = this.onError.bind(this, identifier);

        this.socketMap.set(identifier, socket);
    }

    close(identifier) {
        const socket = this.socketMap.get(identifier);
        if (socket) {
            socket.close();
            this.socketMap.delete(identifier);
        }
    }

    onOpen(event) {
        console.log('WebSocket 连接成功', event)
    }

    onClose(identifier, event) {
        if (this.closeCallbacks[identifier]) {
            this.closeCallbacks[identifier]();
        }
        console.log('WebSocket 连接关闭:', event);
    }

    onCloseReceived(identifier, callback) {
        this.closeCallbacks[identifier] = callback;
    }

    onError(identifier, event) {
        if (this.errorCallbacks[identifier]) {
            this.errorCallbacks[identifier]();
        }
        console.error('WebSocket 连接错误:', event);
    }

    onErrorReceived(identifier, callback) {
        this.errorCallbacks[identifier] = callback;
    }

    onMessage(identifier, event) {
        const message = JSON.parse(event.data)
        // 调用相应标识符接口的消息回调函数
        const cbs = this.messageCallbacks[identifier];
        if (cbs) {
            if (cbs[message.name]) {
                cbs[message.name].forEach((e, index) => {
                    // this.messageCallbacks[identifier][message.name][index](message.data)
                    e(message.data);
                })
            } else {
                // console.log(event, '未找到对应通道')
            }
        } else {
            // console.log(event, '未找到对应链接')
        }
    }

    onMessageReceived(identifier, channel, callback) {
        if (!this.messageCallbacks[identifier]) {
            this.messageCallbacks[identifier] = {};
        }
        if (!this.messageCallbacks[identifier][channel]) {
            this.messageCallbacks[identifier][channel] = [callback];
        } else {
            this.messageCallbacks[identifier][channel].push(callback);
        }
    }

    send(message, identifier) {
        const socket = this.socketMap.get(identifier);
        if (socket.readyState === WebSocket.OPEN) {
            socket.send(message)
        }
    }
}
