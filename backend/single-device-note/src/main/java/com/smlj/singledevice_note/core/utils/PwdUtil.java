package com.smlj.singledevice_note.core.utils;

import cn.hutool.crypto.digest.BCrypt;
import lombok.extern.slf4j.Slf4j;

/**
 * 账号密码的加密与校验 —— cght.t_user.pwd 的唯一读写口径。
 * <p>
 * 库里存的是 <b>BCrypt 加盐哈希</b>（定长 60，形如 {@code $2a$10$...}），既不是明文、也不是可逆密文：
 * <ul>
 *   <li>每行独立随机盐：同一个密码两次加密结果不同，哈希值本身不泄露"谁和谁密码一样"；</li>
 *   <li>单向不可逆：库或源码泄露都拿不回原文，只能逐条爆破，而 2^10 轮迭代把爆破成本抬高几个数量级；</li>
 *   <li>因此"忘记密码"只能重置、不能找回 —— 账号管理页的「重置密码」正好就是这个语义。</li>
 * </ul>
 * <p>
 * 为什么不复用本项目已有的 {@link AesUtil}（AES 可逆）：它的密钥硬编码在源码里，
 * 拿到源码就能把整表密码还原成明文，对"登录口令"而言等于没加密。可逆加密适合需要读回原文的
 * 配置项（如 {@code CarOpService} 里的报文），不适合口令。
 * <p>
 * 明文只存在于两个瞬间：请求参数里、以及落库前调用 {@link #encrypt(String)} 的那一刻。
 */
@Slf4j
public class PwdUtil {
    /**
     * BCrypt 轮数（2 的幂）：10 轮 ≈ 单次校验几十毫秒 —— 登录/改密完全无感，离线爆破代价却很高。
     */
    private static final int ROUNDS = 10;

    private PwdUtil() {
    }

    /**
     * 明文 → 密文。每次调用都生成新的随机盐，所以同一密码两次调用结果不同 —— 这是正确行为。
     * 落库前必须过这里，否则库里就会出现明文。
     */
    public static String encrypt(String raw) {
        if (raw == null) {
            return null;
        }
        return BCrypt.hashpw(raw, BCrypt.gensalt(ROUNDS));
    }

    /**
     * 存储值是不是 BCrypt 密文：定长 60 + {@code $2a$/$2b$/$2y$} 前缀。
     * 用形态判断而非"猜"，是为了让历史明文行可识别 —— 见 {@link #matches}。
     */
    public static boolean isEncrypted(String stored) {
        if (stored == null) {
            return false;
        }
        String s = stored.trim();
        return s.length() == 60
                && (s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$"));
    }

    /**
     * 校验明文是否匹配存储值。
     * <p>
     * 密文走 BCrypt 校验；<b>历史明文行回退成明文比对</b> —— 这是"忘了跑迁移脚本"的安全网，
     * 不是放行：迁移脚本（{@code backend/db/cght_user_pwd_bcrypt.sql}）跑完后正常路径不会再命中，
     * 一旦命中会打一条 warn 日志，顺着日志就能发现哪次上线漏了迁移。
     * 想彻底关掉这层兜底，把最后一个 return 改成 false（届时未迁移的明文行会全部登不进来）。
     */
    public static boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        if (isEncrypted(stored)) {
            try {
                return BCrypt.checkpw(raw, stored);
            } catch (Exception e) {
                // 密文被截断/手工改歪 —— 一律判不匹配，不让异常冒到登录接口变成 500
                log.warn("密码密文形态非法，按不匹配处理");
                return false;
            }
        }
        log.warn("检测到未迁移的明文密码行，已按明文放行；请执行 backend/db/cght_user_pwd_bcrypt.sql 完成迁移");
        return stored.equals(raw);
    }
}
