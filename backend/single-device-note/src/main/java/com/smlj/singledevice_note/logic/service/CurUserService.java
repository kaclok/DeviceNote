package com.smlj.singledevice_note.logic.service;

import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTRoleDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCGHTUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTRole;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 当前登录用户解析 —— 全模块唯一入口。
 * <p>
 * 为什么需要它：JWT 里只存 account（见 {@link com.smlj.singledevice_note.core.utils.JwtUtil#ACCOUNT_CLAIM}）。
 * 每个请求进来，TokenInterceptor 都得按 account 现查一次 t_user，才能拿到实时的
 * role / perms / dept_code / data_scope / open_status。带来两个直接好处：
 * <ul>
 *   <li>admin 改了姓名、角色、权限或数据范围后，最长 TTL 内全站生效，**不需要用户重新登录**；</li>
 *   <li>账号被停用或被删，下一个请求就被拒（原来能靠旧 token 一直用到 RT 过期）。</li>
 * </ul>
 * <p>
 * 为什么加 TTL 缓存：t_user 是低频变更的字典表，但每个请求都要读。用 30s 的进程内缓存把
 * "每请求一查"降成"每 30s 一查"，在权限实时性与查库量之间取折中。
 * 只做过期失效、不做主动续期，避免缓存永不过期。
 * <p>
 * 缓存同时存 role：perms 是鉴权判据（@RequirePermission），必须与 user 同一时刻取出，
 * 否则会出现"用新用户的旧权限"这种自相矛盾的判据。
 * <p>
 * ⚠️ 写路径必须主动 {@link #evict(String)}（accountSave / resetPwd / toggle / changePwd），
 * 否则改完要等 TTL 才生效。改了角色本身的 perms（目前只能改库）则须 {@link #evictAll()}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurUserService {
    /**
     * 缓存有效期：账号信息的变更最多滞后这么久全站生效。
     * 想做到"改完立即生效"把它调成 0 即可 —— t_user 是主键单行查询，成本可忽略。
     */
    private static final long TTL_MS = 30_000;

    private final TCGHTUserDao userDao;
    private final TCGHTRoleDao roleDao;

    /**
     * account -> 条目。key 用 account 而不是整串 JWT：同一账号的 AT / 多端登录共用一份缓存。
     */
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    private record Entry(TCGHTUser user, long at) {
    }

    /**
     * 按账号解析实时用户（含 role），带 TTL 缓存。
     * <p>
     * ⚠️ 返回的是缓存内的**同一个实例**，调用方只读、不要修改它；需要改请自行 query 一份。
     *
     * @return 账号不存在、或已停用，一律返回 null —— 调用方必须 fail-closed，不要退化成"放行"
     */
    public TCGHTUser byAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        Entry e = cache.get(account);
        if (e != null && System.currentTimeMillis() - e.at() < TTL_MS) {
            return e.user();
        }
        return reload(account);
    }

    /**
     * 绕过缓存重查一次并回写缓存。
     * 缓存过期后的回源走它，/account/me 也用它 —— 前端刷新快照要的就是"现在这一刻的值"。
     */
    public TCGHTUser reload(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        TCGHTUser u = userDao.query(account);
        // 账号已不存在：顺手清缓存。否则"删了账号、TTL 内还能继续用它访问"会留下一个窗口。
        if (u == null) {
            cache.remove(account);
            return null;
        }
        // 已停用同样不算可用用户：给调用方 null，由调用方 fail-closed。
        // 这里刻意不缓存 null —— 停用是错误路径，多查几次库比缓存一组"空值语义"更不容易出错。
        if (!u.isOpen_status()) {
            cache.remove(account);
            return null;
        }
        u.setRole(roleDao.query(u.getRole_code()));
        cache.put(account, new Entry(u, System.currentTimeMillis()));
        return u;
    }

    /**
     * 让某账号的缓存立即失效。必须在账号信息写入**事务提交之后**调用，
     * 否则另一个线程会回源读到未提交的旧值并缓存 30s，等于把"改完生效"打回去
     * （CCGHT.evictUserAfterCommit 负责这个时序）。
     */
    public void evict(String account) {
        if (StringUtils.hasText(account)) {
            cache.remove(account);
        }
    }

    /**
     * 清空全部缓存 —— 角色（t_role.perms）本身被改动时用。
     * 目前角色只读（只有 /role/list），改动只能直接改库，所以暂无调用方；保留给后续角色维护接口。
     */
    public void evictAll() {
        cache.clear();
    }
}
