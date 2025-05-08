import {onMounted, onUnmounted} from "vue"

// 3.05 DHi:/ t@E.Ul 04/20 标签页间通信 # JavaScript # 前端开发工程师 # 编程 # 程序员 # web前端  https://v.douyin.com/jvXXurH2fes/ 复制此链接，打开Dou音搜索，直接观看视频！
function useBroadcastChannel(channelName, onMessage) {
    const channel = new BroadcastChannel(channelName);

    onMounted(() => {
        channel.addEventListener('message', onMessage);
    })

    onUnmounted(() => {
        channel.removeEventListener('message', onMessage);
    })

    return {channel}
}

export {
    useBroadcastChannel,
}