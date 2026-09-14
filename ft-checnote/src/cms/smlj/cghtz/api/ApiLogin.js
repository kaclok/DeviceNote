import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

export class ApiLogin {
    /* ---------------- 认证 ---------------- */
    // 遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名
    // 必须接住 axios Promise 再回调，否则调用方的 onBefore/onAfter 永远不会执行
    static login(paras, signal, onBefore, onAfter) {
        onBefore?.();
        axiosR.post("cghtz/account/login", null, {
            params: paras, signal: signal,
        }).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    static logout(paras, signal, onBefore, onAfter) {
        onBefore?.();
        axiosR.post("cghtz/account/logout", null, {
            params: paras, signal: signal,
        }).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }

    /**
     * 修改密码（用户自助）：原密码校验通过后写入新密码。
     * 注意：account 不传，后端从请求头 at(JWT) 解析，避免越权改他人密码。
     */
    static changePwd(paras, signal, onBefore, onAfter) {
        onBefore?.();
        axiosR.post("cghtz/account/changePwd", null, {
            params: paras, signal: signal,
        }).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }
}
