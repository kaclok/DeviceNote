import {onMounted, onUnmounted} from "vue"
import {SseService} from "@/framework/services/SseService.js";

function useSse(url) {
    const sseInstance = new SseService(url);

    onMounted(() => {
        console.error("onMounted");
        sseInstance.connect();
    })

    onUnmounted(() => {
        console.error("onUnmounted");
        sseInstance.close();
    })

    return {sseInstance}
}

export {
    useSse,
}