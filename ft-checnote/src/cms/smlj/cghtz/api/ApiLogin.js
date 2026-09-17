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

    /**
     * 当前登录用户的实时快照。
     * token 里只有 account，用户名 / 角色 / 权限 / 数据范围都在服务端按 account 现查，
     * 这里把它拉下来回填本地 ACCOUNT 缓存 —— admin 改过我的姓名/角色/权限后，
     * 不需要重新登录，下一次进页面就会看到新值。
     *
     * 与其它方法的差别：**返回 Promise**。路由守卫要 await 它（见 SysX.ensureMe），
     * 必须等刷新落地再放行，否则页面 setup 里读缓存的代码会读到刷新前的旧值。
     */
    static me(signal, onBefore, onAfter) {
        onBefore?.();
        return axiosR.post("cghtz/account/me", null, {
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
