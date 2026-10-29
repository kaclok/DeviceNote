import {ApiX} from "../api/ApiX.js";
import {ApiLogin} from "../api/ApiLogin.js";
import {ECacheType, useSessionCache} from "@/framework/composable/use/useCache.ts";

const {wsCache} = useSessionCache()

/**
 * cbs 组织管理 - 业务层
 *
 * 设计说明：
 * 1. dept/role 字典在登录后预加载并缓存，后续直接读缓存，不再请求后端；
 * 2. 组织数据变更的页面（org.vue）写库成功后调用 refreshDeptCache/refreshRoleCache 强制回源；
 * 3. 所有方法遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名。
 */

// 字典缓存
let _roleCache = null    // 角色列表
let _deptCache = null    // 部门列表（平铺，树由视图侧组装）
let _deptLoading = null  // 请求去重：登录预加载与首屏页面可能同时触发

/**
 * 部门只拉一次：命中缓存直接返回，否则复用同一个在途请求。
 * 刻意不透传调用方的 signal —— 这是全局共享的字典，不应因某个页面卸载而被 abort。
 * 失败时重置 _deptLoading，保证后续能重试。
 */
function loadDeptOnce() {
    if (_deptCache) return Promise.resolve(_deptCache)
    if (!_deptLoading) {
        _deptLoading = ApiX.getDeptList(null)
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
    const total = 2
    const check = () => {
        done++
        if (done >= total) onAfter?.()
    }
    ApiX.getRoleList(signal).then(succ => {
        _roleCache = succ.data
    }).catch(() => {}).finally(check)
    // 部门字典：人员的"归属部门"要靠它把 dept_code 回显成部门名，登录后立即拉全量
    loadDeptOnce().catch(() => {}).finally(check)
}

/** 登出时清空缓存 */
export function clearDictCache() {
    _roleCache = null
    _deptCache = null
    _deptLoading = null
}

/* ---------------- 当前用户快照 ---------------- */
// MPA 每个页面是独立文档，模块状态不跨页共享，所以下面几个变量实际是「每页一次」的粒度。
let _meAt = 0            // 上次成功刷新的时间戳
let _meLoading = null    // 在途请求去重
const ME_TTL = 60_000    // 60s 内不重复请求

/**
 * 确保本地 ACCOUNT 缓存是「当前用户最新的」（/cbs/me）。
 * 失败不 reject：调用方是路由守卫，刷新失败由 axios 拦截器统一处理登出，
 * 这里静默保留旧缓存即可，不能因为一次刷新失败把导航打断。
 */
export function ensureMe(force = false) {
    if (!force && _meAt && Date.now() - _meAt < ME_TTL) return Promise.resolve(true)
    if (_meLoading) return _meLoading
    _meLoading = ApiLogin.me(null)
        .then(succ => {
            const acc = succ?.data?.data
            if (!acc) return false
            wsCache.set(ECacheType.ACCOUNT, acc)
            // ALL_PERMS：合并去重后的权限码数组，v-hasPermission 等指令读它
            // ⚠️ jackson NON_EMPTY：perms 为空数组时该字段整个消失，须给默认 []
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
    /* ---------------- 部门 ---------------- */
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
            onAfter?.(false, fail);
        }
    }

    /** 写库成功后由 org.vue 调用：强制下一次读取回源 */
    refreshDeptCache() {
        _deptCache = null
        _deptLoading = null
    }

    async saveDept(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.saveDept(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async deleteDept(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.deleteDept(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 角色 ---------------- */
    async getRoleList(paras, signal, onBefore, onAfter) {
        if (_roleCache) {
            onAfter?.(true, _roleCache)
            return
        }
        onBefore?.();
        ApiX.getRoleList(signal).then(succ => {
            _roleCache = succ.data
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    refreshRoleCache() {
        _roleCache = null
    }

    async saveRole(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.saveRole(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async deleteRole(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.deleteRole(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 人员 ---------------- */
    async getUserList(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.getUserList(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async saveUser(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.saveUser(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async resetUserPwd(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.resetUserPwd(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    async toggleUser(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.toggleUser(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 组织树辅助列回写 ---------------- */
    async warpOrgs(signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.warpOrgs(signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /* ---------------- 业务模块通用转发（承包商/项目/安全承诺/基础信息） ----------------
     * url 为后端完整路径（如 "cbs/contractor/list"），参数约定见 ApiX.biz。
     * 无缓存语义：字典类数据由调用方自行持有。
     */
    async biz(url, paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.biz(url, paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }
}

export {
    SysX,
}
