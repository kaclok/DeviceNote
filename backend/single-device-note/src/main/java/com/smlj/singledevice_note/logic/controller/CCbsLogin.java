package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.core.utils.PwdUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsUserDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsDept;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsUser;
import com.smlj.singledevice_note.logic.service.CurCbsUserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * cbs 域登录（独立账号表 cbs.t_user）。
 * <p>
 * 与 cghtz 的关键差异：这里是**真校验口令**的登录入口（cght.z 的 /x/login* 系列是历史遗留的
 * 免验后门，靠"两表无交集"挡着，cbs 不复制那个坑）。签发的 AT/RT 都带 realm=cbs，
 * TokenInterceptor 按域隔离 —— cbs token 进不了 /cghtz/**，cghtz token 也进不了 /cbs/**。
 * <p>
 * RT 只装 {account, realm}（口径同 CJwt：用户信息一律现查，快照不进 token）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cbs")
@Tag(name = "CCbsLogin", description = "cbs登录")
public class CCbsLogin {
    private final TCbsUserDao userDao;
    private final com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsDeptDao deptDao;
    private final CurCbsUserService curCbsUserService;

    @Transactional
    @PostMapping(value = "/login")
    public Result<?> login(String account, String password, HttpServletResponse response) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            return Result.fail(ResultCode.RC10101, "account/password 不能为空");
        }
        TCbsUser user = userDao.query(account);
        // 账号不存在 与 停用 同一口径，不暴露存在性
        if (user == null || !Boolean.TRUE.equals(user.getIs_using())) {
            return Result.fail(ResultCode.RC10301);
        }
        if (!PwdUtil.matches(password, user.getPwd())) {
            return Result.fail(ResultCode.RC10302);
        }

        String deptName = null;
        TCbsDept org = deptDao.queryById(user.getDept_code());
        if (org != null) {
            deptName = org.getDept_name();
        }

        // AT：带展示字段的快照仅作首屏兜底，权限/范围一律由 TokenInterceptor 现查
        HashMap<String, Object> atClaims = new HashMap<>();
        atClaims.put(JwtUtil.ACCOUNT_CLAIM, user.getAccount());
        atClaims.put(JwtUtil.REALM_CLAIM, JwtUtil.REALM_CBS);
        atClaims.put("name", user.getUsername());
        atClaims.put("dept_code", user.getDept_code());
        if (deptName != null) {
            atClaims.put("dept_name", deptName);
        }
        JwtUtil.setAccessTokenHeader(response, JwtUtil.getToken(atClaims, JwtUtil.ACCESS_EXPIRE));

        // RT：只装身份 + 域，刷新时原样透传（见 CJwt.refreshAccessToken）
        Map<String, Object> rtClaims = Map.of(
                JwtUtil.ACCOUNT_CLAIM, user.getAccount(),
                JwtUtil.REALM_CLAIM, JwtUtil.REALM_CBS);
        JwtUtil.setRefreshTokenHeader(response, JwtUtil.getToken(rtClaims, JwtUtil.RRFRESH_EXPIRE));

        // 预热缓存（可选优化）：登录成功即回源一次，首个业务请求免查库
        curCbsUserService.reload(user.getAccount());

        HashMap<String, Object> r = new HashMap<>();
        r.put("account", user.getAccount());
        r.put("username", user.getUsername());
        r.put("dept_code", user.getDept_code());
        r.put("user_type", user.getUser_type());
        if (deptName != null) {
            r.put("dept_name", deptName);
        }
        return Result.success(r);
    }
}
