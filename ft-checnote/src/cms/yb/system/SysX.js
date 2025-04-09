import {TimeService} from "@/framework/services/TimeService.js";
import {ApiX} from "@/cms/yb/api/ApiX.js";
import {onceAsync} from "@/framework/utils/OnceAsync.js";

class SysX {
    async getList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async add(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.add(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async del(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.del(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async export(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.export(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }
}

export {
    SysX,
};
