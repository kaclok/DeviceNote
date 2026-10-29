package com.smlj.singledevice_note.logic.service;

import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsRoleDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsRole;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * cbs 域当前登录用户解析（cbs.t_user，独立账号表）。
 * 口径与 {@link CurUserService} 完全一致（30s TTL、fail-closed、缓存同存 role），
 * 只是数据源换成 cbs schema —— 两套账号体系按 JWT realm 双向隔离，互不可见。
 * 写路径（账号/角色维护）后必须 evict / evictAll，时序要求同 CurUserService。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CurCbsUserService {
    private static final long TTL_MS = 30_000;

    private final TCbsUserDao userDao;
    private final TCbsRoleDao roleDao;

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    private record Entry(TCbsUser user, long at) {
    }

    public TCbsUser byAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        Entry e = cache.get(account);
        if (e != null && System.currentTimeMillis() - e.at() < TTL_MS) {
            return e.user();
        }
        return reload(account);
    }

    public TCbsUser reload(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        TCbsUser u = userDao.query(account);
        if (u == null) {
            cache.remove(account);
            return null;
        }
        if (!Boolean.TRUE.equals(u.getIs_using())) {
            cache.remove(account);
            return null;
        }
        u.setRole(roleDao.query(u.getRole_code()));
        cache.put(account, new Entry(u, System.currentTimeMillis()));
        return u;
    }

    public void evict(String account) {
        if (StringUtils.hasText(account)) {
            cache.remove(account);
        }
    }

    public void evictAll() {
        cache.clear();
    }
}
