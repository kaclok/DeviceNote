export class SseService {
    constructor(url) {
        this.url = url;
        this.eventSource = null;
        this.msgQueue = [];
    }

    connect() {
        if (this.eventSource) {
            return;
        }

        // 连接 Spring Boot 的 SSE 端点
        this.eventSource = new EventSource(this.url);

        this.eventSource.onmessage = (event) => {
            console.log(event.data);

            this.msgQueue.push(event.data);
        };

        this.eventSource.onerror = (error) => {
            console.error('SSE 连接错误:', error);
            this.close();
        };
    }

    close() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
    }
}
