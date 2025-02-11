// 自定义指令
import vListenResize from "@/framework/directives/vListenResize.js";
import vCancelWhenUnmount from "@/framework/directives/cancel-when-unmount/vCancelWhenUnmount.js";
import vHasPermission from "@/framework/directives/vHasPermission.js";
import vHasRole from "@/framework/directives/vHasRole.js";
import vCd from "@/framework/directives/vCd.js";
import vDraggableFloat from "@/framework/directives/vDraggableFloat.ts";
import vThrottle from "@/framework/directives/vThrottle.js";
import vDebounce from "@/framework/directives/vDebounce.js";
import VLongPress from "@/framework/directives/vLongPress.js";

const list = [
    {
        name: "listenResize",
        directive: vListenResize.directive,
    },
    {
        name: "cancelWhenUnmount",
        directive: vCancelWhenUnmount.directive,
    },
    {
        name: "cd",
        directive: vCd.directive,
    },
    {
        name: "hasPermission",
        directive: vHasPermission.directive,
    },
    {
        name: "hasRole",
        directive: vHasRole.directive,
    },
    {
        name: "draggableFloat",
        directive: vDraggableFloat.directive,
    },
    {
        name: "throttle",
        directive: vThrottle.directive,
    },
    {
        name: "debounce",
        directive: vDebounce.directive,
    },
    {
        name: "longPress",
        directive: VLongPress.directive,
    },
]

function RegisterDirective(app) {
    for (const one of list) {
        app.directive(one.name, one.directive);
    }
}

export {
    list,
    RegisterDirective,
}