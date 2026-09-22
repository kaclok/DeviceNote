import {ApiX} from "../api/ApiX.js";
import {ApiLogin} from "../api/ApiLogin.js";
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useSessionCache()

/**
 * 从 axios catch 的 fail 对象中提取 {code, msg, data} 业务体。
 * - 非OK业务码：AxiosInst 做 Promise.reject(response)，fail = response 对象，fail.data = {code,msg,data}
 * - HTTP错误：fail = AxiosError，fail.response.data = {code,msg,data}
 * - 其他：原样返回
 */
function _failBody(fail) {
    return fail
}

/**
 * 合同台账 - 业务层
 *
 * 设计说明：
 * 1. role/perm/dept 字典在登录后预加载并缓存，后续直接读缓存，不再请求后端；
 * 2. 登出时调用 clearDictCache() 清空缓存；
 * 3. 所有方法遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名。
 */

// 字典缓存
let _roleCache = null    // 角色列表
let _permCache = null    // 权限定义列表
let _deptCache = null    // 组织架构（部门字典，来自 train.t_org）
let _deptLoading = null  // 组织架构请求去重：登录预加载与首屏页面可能同时触发
let _tplCache = null     // 合同模版列表（模板是低频变更的系统配置，仅「部门合同模板」页用到，
                         // 因此不进登录预加载，只在首次访问该页时拉一次并缓存）

/**
 * 组织架构只拉一次：命中缓存直接返回，否则复用同一个在途请求。
 * 刻意不透传调用方的 signal —— 这是全局共享的字典，不应因某个页面卸载而被 abort。
 * 失败时重置 _deptLoading，保证后续能重试（否则一次失败会永久返回空）。
 */
function loadDeptOnce() {
    if (_deptCache) return Promise.resolve(_deptCache)
    if (!_deptLoading) {
        _deptLoading = ApiX.getDeptList(null, null)
            .then(succ => {
                _deptCache = succ?.data?.data || []
                return _deptCache
            })
            .catch(err => {
                _deptLoading = null
                throw err
            })
    }
    return _deptLoading
}

/** 登录成功后预加载字典缓存 */
export function preloadDictCache(signal, onAfter) {
    let done = 0
    const total = 3
    const check = () => {
        done++
        if (done >= total) onAfter?.()
    }
    ApiX.getRoleList(null, signal).then(succ => {
        _roleCache = succ.data
    }).catch(() => {}).finally(check)
    ApiX.getPermDefs(null, signal).then(succ => {
        _permCache = succ.data
    }).catch(() => {}).finally(check)
    // 组织架构：合同/账号的"归属部门"都要靠它把 dept_code 回显成"公司/部门"名，
    // 登录后立即拉全量并缓存，避免每个页面各自请求一次
    loadDeptOnce().catch(() => {}).finally(check)
}

/** 登出时清空缓存 */
export function clearDictCache() {
    _roleCache = null
    _permCache = null
    _deptCache = null
    _deptLoading = null
    _tplCache = null
}

/* ---------------- 当前用户快照 ---------------- */
// MPA 每个页面是独立文档，模块状态不跨页共享，所以下面几个变量实际是「每页一次」的粒度。
let _meAt = 0            // 上次成功刷新的时间戳
let _meLoading = null    // 在途请求去重
const ME_TTL = 60_000    // 60s 内不重复请求

/**
 * 确保本地 ACCOUNT 缓存是「当前用户最新的」。
 *
 * 为什么需要它：token 里只放 account，服务端每个请求都会按 account 现查实时用户；
 * 但本地 ACCOUNT 是上一次会话的快照 —— admin 改过我的姓名/角色/权限后，
 * 菜单、按钮、「本人」档的签订人预填都会继续按旧值渲染。
 *
 * 失败不 reject：调用方是路由守卫，刷新失败（例如 RT 已过期）由 axios 拦截器统一处理登出，
 * 这里静默保留旧缓存即可，不能因为一次刷新失败把导航打断。
 *
 * @param force 忽略 TTL 强制刷新
 * @returns Promise<boolean> 是否刷新成功
 */
export function ensureMe(force = false) {
    if (!force && _meAt && Date.now() - _meAt < ME_TTL) return Promise.resolve(true)
    if (_meLoading) return _meLoading
    _meLoading = ApiLogin.me(null)
        .then(succ => {
            const acc = succ?.data?.data
            if (!acc) return false
            wsCache.set(ECacheType.ACCOUNT, acc)
            // ALL_PERMS 与登录时同口径：合并去重后的权限码数组，v-hasPermission 等指令读它
            const roles = Array.isArray(acc.role) ? acc.role : [acc.role]
            wsCache.set(ECacheType.ALL_PERMS, [...new Set(roles.flatMap(r => r?.perms || []))])
            _meAt = Date.now()
            return true
        })
        .catch(() => false)
        .finally(() => {
            _meLoading = null
        })
    return _meLoading
}

class SysX {
    /* ---------------- 合同台账 ---------------- */
    async getContractList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getContractList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async getContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async checkNoExists(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getContract({id: paras.id}, signal).then(succ => {
            onAfter?.(true, {code: __OK__, data: succ.data.data != null});
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async createContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.createContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async updateContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.updateContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async deleteContract(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.deleteContract(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    // params = {tpl_id}：模版是整批数据的身份，后端据此比对部门绑定后才允许落库
    async importContractExcel(rows, params, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.importContractExcel(rows, params, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    /* ---------------- 账号与权限 ---------------- */
    async getAccountList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getAccountList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async saveAccount(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.saveAccount(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async resetPassword(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.resetPassword(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async toggleAccountStatus(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.toggleAccountStatus(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    /* ---------------- 角色与权限字典 ---------------- */
    async getRoleList(paras, signal, onBefore, onAfter) {
        if (_roleCache) {
            onAfter?.(true, _roleCache)
            return
        }
        onBefore?.();
        ApiX.getRoleList(paras, signal).then(succ => {
            _roleCache = succ.data
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
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
            onAfter?.(false, _failBody(fail));
        });
    }

    /* ---------------- 组织架构（部门字典，只读） ---------------- */
    // 登录成功后由 preloadDictCache 预加载并缓存，之后所有页面 / DeptPicker 直接读缓存，
    // 本地完成"输入关键字匹配部门名"、组织树渲染/搜索，以及 dept_code → 公司/部门名的回显。
    // 缓存为空时（例如直接刷新页面、未走登录）自动回源一次并补上缓存。
    // 回调数据形状与其它接口保持一致：{code, data}，调用方读 data.data 拿数组。
    async getDeptList(paras, signal, onBefore, onAfter) {
        if (_deptCache) {
            onAfter?.(true, {code: __OK__, data: _deptCache})
            return
        }
        onBefore?.();
        try {
            const ls = await loadDeptOnce()
            onAfter?.(true, {code: __OK__, data: ls})
        } catch (fail) {
            onAfter?.(false, _failBody(fail));
        }
    }

    /* ---------------- 合同模版 / 部门-模版映射 ---------------- */
    // 模版列表是低频变更的系统配置（台账页的模版下拉、模板页的预览与绑定都用它；
    // 导入页的单个部门模版来自 /deptTpl/effective，不拉这份列表），
    // 首次拉取后缓存；与其它字典一样由 clearDictCache 在登出时清掉 —— 换账号不能沿用上一个人的字典。
    async getTemplateList(paras, signal, onBefore, onAfter) {
        if (_tplCache) {
            onAfter?.(true, {code: __OK__, data: _tplCache})
            return
        }
        onBefore?.();
        try {
            const succ = await ApiX.getTemplateList(paras, signal)
            _tplCache = succ?.data?.data || []
            onAfter?.(true, {code: __OK__, data: _tplCache})
        } catch (fail) {
            onAfter?.(false, _failBody(fail));
        }
    }

    /**
     * 保存某套模版的 Excel 列顺序（模板页拖拽排序的结果）。
     * 成功后把**缓存里的那一项**一并改掉：台账页/模板页读的都是 _tplCache 里的 col_order，
     * 不跟着改的话"拖完、切到台账页导出"还是旧顺序，看着像没保存上（而缓存本来就是本页读的地方）。
     * 只改这一项、不整体失效：模版列表刷新会顺带把"哪些部门持有该模版"重新算一遍，
     * 而顺序改动不影响绑定关系，没必要为此多一次请求。
     */
    async saveTplColOrder(paras, signal, onBefore, onAfter) {
        onBefore?.();
        try {
            const succ = await ApiX.saveTplColOrder(paras, signal)
            const saved = succ?.data?.data?.col_order ?? null
            const tplId = String(paras?.tpl_id ?? '')
            if (Array.isArray(_tplCache) && tplId) {
                _tplCache = _tplCache.map(t => (String(t.id) === tplId ? {...t, col_order: saved} : t))
            }
            onAfter?.(true, succ.data)
        } catch (fail) {
            onAfter?.(false, _failBody(fail));
        }
    }

    /**
     * 部门 → 模版 绑定全量。刻意不做本地缓存：
     * 绑定刚刚才在本页改过，拿旧快照渲染会让用户以为没保存成功；
     * 保存/解除后重新拉一次就是真实回源，代价只是一次百余行的查询。
     */
    async getDeptTplList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getDeptTplList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async saveDeptTpl(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.saveDeptTpl(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    async deleteDeptTpl(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.deleteDeptTpl(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }

    /**
     * 单个部门的「生效模版」（读，不缓存）。
     * 不缓存的理由和 getDeptTplList 一样：刚在「部门合同模板」页改过绑定，
     * 导入页却拿旧快照去拦用户，会得到一个与事实相反的结论。
     */
    async getDeptTplEffective(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getDeptTplEffective(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, _failBody(fail));
        });
    }
}

export {
    SysX,
}
