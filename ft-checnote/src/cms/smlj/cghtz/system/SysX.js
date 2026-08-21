import {ApiX} from "../api/ApiX.js";

/**
 * 合同台账 - 业务层
 *
 * 设计说明：
 * 1. USE_MOCK = true：全部走本地 MockX，用于前端独立演示；
 * 2. USE_MOCK = false：全部走后端 ApiX，请求失败如实回调失败，
 *    不做 Mock 兜底（避免后端出错时前端仍展示假数据，掩盖真实问题）；
 * 3. 所有方法遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名。
 */
class SysX {
    /* ---------------- 合同台账 ---------------- */
    async getContractList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getContractList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async getContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async checkNoExists(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getContract({id: paras.id}, signal).then(succ => {
            onAfter?.(true, {code: __OK__, data: succ.data.data != null});
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async createContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.createContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async updateContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.updateContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async deleteContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.deleteContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async importContractExcel(rows, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.importContractExcel(rows, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 签订人字典 ---------------- */
    async getSignerList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getSignerList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 账号与权限 ---------------- */
    async getAccountList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getAccountList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async saveAccount(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.saveAccount(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async resetPassword(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.resetPassword(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async toggleAccountStatus(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.toggleAccountStatus(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 角色与权限字典 ---------------- */
    async getRoleList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getRoleList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async getPermDefs(paras, signal, onBefore, onAfter) {
        onBefore?.();
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
