import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

export class ApiLogin {
    /* ---------------- 认证 ---------------- */
    // 遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名
    static login(paras, signal, onBefore, onAfter) {
        onBefore?.();
        axiosR.post("cbs/login", null, {
            params: paras, signal: signal,
        }).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    // cbs 后端没有 logout 端点：登录态是纯前端会话缓存（JWT 无服务端会话），
    // 退出 = 清本地缓存回登录页。token 未过期前仍有效，与 cghtz 服务端登出的差别仅在于
    // 旧 token 不能被主动作废 —— cbs 一期风险可接受（token 短时效 + realm 隔离）。
    // 若后续要求"登出即作废"，后端加 /cbs/logout + token 黑名单，这里补一个调用即可。

    /**
     * 当前登录用户的实时快照（/cbs/me）。
     * 返回 Promise —— 路由守卫要 await 它（见 SysX.ensureMe），
     * 必须等刷新落地再放行，否则页面 setup 里读缓存的代码会读到刷新前的旧值。
     */
    static me(signal, onBefore, onAfter) {
        onBefore?.();
        return axiosR.post("cbs/me", null, {
            signal: signal,
        }).then(succ => {
            onAfter?.(true, succ.data);
            return succ;
        }).catch(fail => {
            onAfter?.(false, fail);
            throw fail;
        });
    }
}
