import {ApiX} from "../api/ApiX.js";
import {MockX} from "./MockX.js";

/**
 * 合同台账 - 业务层
 *
 * 设计说明：
 * 1. 优先调用后端 ApiX；
 * 2. 后端不可用（接口未开发 / 网络失败）时，自动兜底到本地 MockX，
 *    保证前端页面可独立演示；
 * 3. 所有方法遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名。
 *
 * 后端就绪后：将每个方法的 `_mock` 兜底去掉即可，或保留兜底作为离线容错。
 */

/* 是否优先使用本地 Mock（演示模式）。后端联调时置为 false 即可全部走 ApiX */
const USE_MOCK = true

class SysX {
    /* ---------------- 认证 ---------------- */
    async login(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            const r = MockX.login(paras.account, paras.password);
            setTimeout(() => onAfter?.(r.data.success, r), 300);
            return;
        }
        ApiX.login(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 合同台账 ---------------- */
    async getContractList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            const r = MockX.getContractList(paras || {});
            onAfter?.(true, r);
            return;
        }
        ApiX.getContractList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            console.warn("[cghtz] 后端接口不可用，兜底 Mock 数据", fail);
            onAfter?.(true, MockX.getContractList(paras || {}));
        });
    }

    async getContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.getContract(paras.no));
            return;
        }
        ApiX.getContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(true, MockX.getContract(paras.no));
        });
    }

    async checkNoExists(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.checkNoExists(paras.no));
            return;
        }
        ApiX.getContract({no: paras.no}, signal).then(succ => {
            onAfter?.(true, {code: __OK__, data: succ.data.data != null});
        }).catch(fail => {
            onAfter?.(true, MockX.checkNoExists(paras.no));
        });
    }

    async createContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            const r = MockX.createContract(paras);
            onAfter?.(true, r);
            return;
        }
        ApiX.createContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async updateContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.updateContract(paras));
            return;
        }
        ApiX.updateContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async deleteContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.deleteContract(paras.no));
            return;
        }
        ApiX.deleteContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async importContractExcel(rows, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            const r = MockX.importContractExcel(rows);
            onAfter?.(true, r);
            return;
        }
        ApiX.importContractExcel(rows, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 统计 ---------------- */
    async getStats(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.getStats());
            return;
        }
        // 后端统计接口：cghtz/contract/stats
        ApiX.getContractList({}, signal).then(succ => {
            onAfter?.(true, MockX.getStats());
        }).catch(fail => {
            onAfter?.(true, MockX.getStats());
        });
    }

    /* ---------------- 账号与权限 ---------------- */
    async getAccountList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.getAccountList());
            return;
        }
        ApiX.getAccountList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(true, MockX.getAccountList());
        });
    }

    async saveAccount(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.saveAccount(paras));
            return;
        }
        ApiX.saveAccount(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async resetPassword(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.resetPassword(paras.account));
            return;
        }
        ApiX.resetPassword(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async toggleAccountStatus(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.toggleAccountStatus(paras.account));
            return;
        }
        ApiX.toggleAccountStatus(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }
}

export {
    SysX,
}
