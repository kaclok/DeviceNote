import {ApiX} from "../api/ApiX.js";
import {MockX} from "./MockX.js";

/**
 * 合同台账 - 业务层
 *
 * 设计说明：
 * 1. USE_MOCK = true：全部走本地 MockX，用于前端独立演示；
 * 2. USE_MOCK = false：全部走后端 ApiX，请求失败如实回调失败，
 *    不做 Mock 兜底（避免后端出错时前端仍展示假数据，掩盖真实问题）；
 * 3. 所有方法遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名。
 */

/* 是否优先使用本地 Mock（演示模式）。后端联调时置为 false 即可全部走 ApiX */
const USE_MOCK = false

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

    async logout(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.logout(paras, signal).then(succ => {
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
            onAfter?.(false, fail);
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
            onAfter?.(false, fail);
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
            onAfter?.(false, fail);
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

    /* ---------------- 签订人字典 ---------------- */
    async getSignerList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.getSignerList());
            return;
        }
        ApiX.getSignerList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
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
            onAfter?.(false, fail);
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

    /* ---------------- 角色与权限字典 ---------------- */
    async getRoleList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.getRoleList());
            return;
        }
        ApiX.getRoleList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async getPermDefs(paras, signal, onBefore, onAfter) {
        onBefore?.();
        if (USE_MOCK) {
            onAfter?.(true, MockX.getPermDefs());
            return;
        }
        ApiX.getPermDefs(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }
}

export {
    SysX,
}
