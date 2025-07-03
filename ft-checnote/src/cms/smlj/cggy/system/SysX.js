import {TimeService} from "@/framework/services/TimeService.js";
import {ApiX} from "@/cms/sb/api/ApiX.js";
import {onceAsync} from "@/framework/utils/OnceAsync.js";

class SysX {
    async getGYListByZZ(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getGYListByZZ(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async getDeviceList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getDeviceList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async getRecordList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getRecordList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async getRecord(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getRecord(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async report(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.report(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async login(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.login(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }
}

export {
    SysX,
};
