import {ApiX} from "../api/ApiX.js";

/**
 * 合同台账 - 业务层
 *
 * 设计说明：
 * 1. signer/perm 字典在登录后预加载并缓存，后续直接读缓存，不再请求后端；
 * 2. 登出时调用 clearDictCache() 清空缓存；
 * 3. 所有方法遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名。
 */

// 字典缓存
let _signerCache = null  // 签订人列表
let _permCache = null     // 权限定义列表

/** 登录成功后预加载字典缓存 */
export function preloadDictCache(signal, onAfter) {
    let done = 0
    const total = 2
    const check = () => {
        done++
        if (done >= total) onAfter?.()
    }
    ApiX.getSignerList(null, signal).then(succ => {
        _signerCache = succ.data
    }).catch(() => {}).finally(check)
    ApiX.getPermDefs(null, signal).then(succ => {
        _permCache = succ.data
    }).catch(() => {}).finally(check)
}

/** 登出时清空缓存 */
export function clearDictCache() {
    _signerCache = null
    _permCache = null
}

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

    /* ---------------- 签订人字典（缓存） ---------------- */
    async getSignerList(paras, signal, onBefore, onAfter) {
        if (_signerCache) {
            onAfter?.(true, _signerCache)
            return
        }
        onBefore?.();
        ApiX.getSignerList(paras, signal).then(succ => {
            _signerCache = succ.data
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
        if (_permCache) {
            onAfter?.(true, _permCache)
            return
        }
        onBefore?.();
        ApiX.getPermDefs(paras, signal).then(succ => {
            _permCache = succ.data
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }
}

export {
    SysX,
}
