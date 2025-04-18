// 1. 自定义封装 WebSocket（推荐 Pinia + 断线重连）
// (1) tools-vue3（推荐）
// (2) vue-use-websocket（基于 VueUse）
// (3) socket.io-client（适用于兼容性要求高的场景）

export class HeartBeatService {
    /*constructor(intervalSeconds, maxRetries) {
        this.intervalSeconds = intervalSeconds;
        this.maxRetries = maxRetries;

        this._hasRetryCount = 0;
        this._timer = null;
    }

    begin() {
        this._timer = setInterval(() => {

        }, this.intervalSeconds * 1000)
    }

    end() {
        if(this._timer) {
            clearInterval(this._timer);
        }
    }*/
}
