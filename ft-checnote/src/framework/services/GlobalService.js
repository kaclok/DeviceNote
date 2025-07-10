import {computed} from "vue";
import {isMobile, isMobileOnly, isTablet} from 'vue-device-detect';

// 运行时：全局变量
export const __IS_MOBILE_ONLY__ = computed(() => isMobileOnly)
export const __IS_TABLET_ONLY__ = computed(() => isTablet)
export const __IS_MOBILE_OR_TABLET__ = computed(() => isMobile)
export const __IS_SUPPORT_WEBSOCKET__ = computed(() => window.WebSocket)
