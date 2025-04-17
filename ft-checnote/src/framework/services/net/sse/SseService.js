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
            let json = JSON.parse(event.data);
            this.msgQueue.push(json);
        };

        this.eventSource.onopen = (event) => {
            console.log('SSE 连接成功:');
        };

        this.eventSource.onerror = (error) => {
            console.log('SSE 连接错误:', error);
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
