import {TimeService} from "@/framework/services/TimeService.js";
import {ApiX} from "@/cms/daily_paper/api/ApiX.js";
import {onceAsync} from "@/framework/utils/OnceAsync.js";

class SysX {
    async getGYListByZZ(paras, signal, onBefore, onAfter) {
        onBefore?.();
        let rlt = await ApiX.getGYListByZZ(paras, signal).catch(fail => {
            onAfter?.(false, fail);
        });

        onAfter?.(true, rlt.data);
    }

    async getDeviceList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        let rlt = await ApiX.getDeviceList(paras, signal).catch(fail => {
            onAfter?.(false, fail);
        });

        onAfter?.(true, rlt.data);
    }

    async getRecordList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        let rlt = await ApiX.getRecordList(paras, signal).catch(fail => {
            onAfter?.(false, fail);
        });
        onAfter?.(true, rlt.data);
    }

    async getRecord(paras, signal, onBefore, onAfter) {
        onBefore?.();
        let rlt = await ApiX.getRecord(paras, signal).catch(fail => {
            onAfter?.(false, fail);
        });
        onAfter?.(true, rlt.data);
    }

    async report(paras, signal, onBefore, onAfter) {
        onBefore?.();
        let rlt = await ApiX.report(paras, signal).catch(fail => {
            onAfter?.(false, fail);
        });

        onAfter?.(true);
    }
}

export {
    SysX,
};
