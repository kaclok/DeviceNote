package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.annotation.Acc;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsCertDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsContractorDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsEnterpriseDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsLicenseChemDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsProjectDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsPromiseDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsSuperviseProcessDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsThreeSameDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsCert;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractor;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsEnterprise;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsLicenseChem;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsProject;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsPromise;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsSuperviseProcess;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsThreeSame;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * cbs 业务模块：承包商单位 / 项目 / 安全承诺日报 / 基础信息（企业信息、企业简介、
 * 安全三同时、许可危化品、重点监管工艺、证照预警）。
 * <p>
 * 约定：列表筛选走 @RequestBody Map（前端统一 biz(url, paras)）；主键由后端 UUID 生成；
 * 删除一律软删（open_status=false）。承包商审核流转独立权限点 cbs:audit。
 * 数据范围过滤（t_project.dept_code 等）一期未启用，见 PRD M3+。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/cbs")
@Tag(name = "CCbsBiz", description = "cbs业务模块")
public class CCbsBiz {
    private final TCbsContractorDao contractorDao;
    private final TCbsProjectDao projectDao;
    private final TCbsPromiseDao promiseDao;
    private final TCbsEnterpriseDao entDao;
    private final TCbsCertDao certDao;
    private final TCbsThreeSameDao threeSameDao;
    private final TCbsLicenseChemDao chemDao;
    private final TCbsSuperviseProcessDao procDao;

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String str(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Integer intVal(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        return v instanceof Number n ? n.intValue() : null;
    }

    // ---------------- 承包商单位 ----------------

    @RequirePermission("cbs:contractor")
    @PostMapping("/contractor/list")
    public Result<?> contractorList(@RequestBody(required = false) Map<String, Object> q) {
        return Result.success(contractorDao.queryAll(str(q, "kw"), intVal(q, "status")));
    }

    @RequirePermission("cbs:contractor")
    @Transactional
    @PostMapping("/contractor/save")
    public Result<?> contractorSave(@RequestBody TCbsContractor c, @Acc TCbsUser me) {
        if (!StringUtils.hasText(c.getName())) {
            return Result.fail(ResultCode.RC10101, "承包商名称不能为空");
        }
        if (StringUtils.hasText(c.getUscc())
                && contractorDao.countUsccDup(c.getUscc(), c.getContractor_id()) > 0) {
            return Result.fail(ResultCode.RC10101, "统一社会信用代码已存在");
        }
        if (!StringUtils.hasText(c.getContractor_id())) {
            c.setContractor_id(newId());
            c.setApprove_status(0);
            c.setCreator(me.getAccount());
            c.setOpen_status(true);
            contractorDao.insert(c);
        } else {
            TCbsContractor old = contractorDao.query(c.getContractor_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "承包商不存在");
            }
            // 主键/审核状态/归属不由编辑表单改动，保留旧值
            c.setApprove_status(old.getApprove_status());
            c.setReject_reason(old.getReject_reason());
            c.setCreator(old.getCreator());
            c.setOpen_status(old.getOpen_status());
            contractorDao.update(c);
        }
        return Result.success();
    }

    /** 审核流转：status 1 通过 / 2 驳回（驳回必填原因）。 */
    @RequirePermission("cbs:audit")
    @Transactional
    @PostMapping("/contractor/audit")
    public Result<?> contractorAudit(@RequestBody Map<String, Object> b) {
        String id = str(b, "contractorId");
        Integer status = intVal(b, "status");
        String reason = str(b, "reason");
        if (!StringUtils.hasText(id) || status == null || (status != 1 && status != 2)) {
            return Result.fail(ResultCode.RC10101, "contractorId/status 不合法");
        }
        if (status == 2 && !StringUtils.hasText(reason)) {
            return Result.fail(ResultCode.RC10101, "驳回必须填写原因");
        }
        if (contractorDao.query(id) == null) {
            return Result.fail(ResultCode.RC10103, "承包商不存在");
        }
        contractorDao.audit(id, status, status == 2 ? reason : null);
        return Result.success();
    }

    @RequirePermission("cbs:contractor")
    @Transactional
    @PostMapping("/contractor/delete")
    public Result<?> contractorDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "contractorId");
        if (!StringUtils.hasText(id) || contractorDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "承包商不存在");
        }
        if (projectDao.countByContractor(id) > 0) {
            return Result.fail(ResultCode.RC10101, "该承包商名下存在项目，不能删除");
        }
        contractorDao.remove(id);
        return Result.success();
    }

    // ---------------- 承包商项目 ----------------

    @RequirePermission("cbs:project")
    @PostMapping("/project/list")
    public Result<?> projectList(@RequestBody(required = false) Map<String, Object> q) {
        return Result.success(projectDao.queryAll(str(q, "kw"), str(q, "contractorId")));
    }

    @RequirePermission("cbs:project")
    @Transactional
    @PostMapping("/project/save")
    public Result<?> projectSave(@RequestBody TCbsProject p, @Acc TCbsUser me) {
        if (!StringUtils.hasText(p.getProj_name())) {
            return Result.fail(ResultCode.RC10101, "项目名称不能为空");
        }
        if (!StringUtils.hasText(p.getContractor_id()) || contractorDao.query(p.getContractor_id()) == null) {
            return Result.fail(ResultCode.RC10101, "承包商单位不存在");
        }
        if (p.getBegin_date() != null && p.getEnd_date() != null && p.getEnd_date().isBefore(p.getBegin_date())) {
            return Result.fail(ResultCode.RC10101, "结束日期不能早于开始日期");
        }
        if (p.getProj_status() == null) {
            p.setProj_status(0);
        }
        if (!StringUtils.hasText(p.getProject_id())) {
            p.setProject_id(newId());
            p.setCreator(me.getAccount());
            p.setOpen_status(true);
            projectDao.insert(p);
        } else {
            TCbsProject old = projectDao.query(p.getProject_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "项目不存在");
            }
            p.setCreator(old.getCreator());
            p.setOpen_status(old.getOpen_status());
            projectDao.update(p);
        }
        return Result.success();
    }

    @RequirePermission("cbs:project")
    @Transactional
    @PostMapping("/project/delete")
    public Result<?> projectDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "projectId");
        if (!StringUtils.hasText(id) || projectDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "项目不存在");
        }
        projectDao.remove(id);
        return Result.success();
    }

    // ---------------- 安全承诺日报 ----------------

    @RequirePermission("cbs:promise")
    @PostMapping("/promise/get")
    public Result<?> promiseGet(@RequestBody Map<String, Object> b) {
        LocalDate date = parseDate(str(b, "date"));
        if (date == null) {
            return Result.fail(ResultCode.RC10101, "date 不能为空（yyyy-MM-dd）");
        }
        TCbsPromise p = promiseDao.queryByDate(date);
        return Result.success(p != null ? p : new TCbsPromise());
    }

    /** 按日期 upsert：一天一行，promise_date 即业务主键。 */
    @RequirePermission("cbs:promise")
    @Transactional
    @PostMapping("/promise/save")
    public Result<?> promiseSave(@RequestBody TCbsPromise p, @Acc TCbsUser me) {
        if (p.getPromise_date() == null) {
            return Result.fail(ResultCode.RC10101, "promise_date 不能为空");
        }
        boolean exists = promiseDao.queryByDate(p.getPromise_date()) != null;
        if (exists) {
            promiseDao.update(p);
        } else {
            p.setCreator(me.getAccount());
            promiseDao.insert(p);
        }
        return Result.success();
    }

    // ---------------- 基础信息：企业信息（含企业简介 ent_intro 字段在库，一期表单不展开附件） ----------------

    @RequirePermission("cbs:baseinfo")
    @PostMapping("/ent/get")
    public Result<?> entGet() {
        TCbsEnterprise e = entDao.get();
        return Result.success(e != null ? e : new TCbsEnterprise());
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/ent/save")
    public Result<?> entSave(@RequestBody TCbsEnterprise e, @Acc TCbsUser me) {
        if (entDao.get() == null) {
            entDao.insert(e);
        } else {
            entDao.update(e, me.getAccount());
        }
        return Result.success();
    }

    // ---------------- 基础信息：证照预警 ----------------

    @RequirePermission("cbs:baseinfo")
    @PostMapping("/cert/list")
    public Result<?> certList(@RequestBody(required = false) Map<String, Object> q) {
        return Result.success(certDao.queryAll(str(q, "kw")));
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/cert/save")
    public Result<?> certSave(@RequestBody TCbsCert c, @Acc TCbsUser me) {
        if (!StringUtils.hasText(c.getCert_name())) {
            return Result.fail(ResultCode.RC10101, "证照名称不能为空");
        }
        if (c.getWarn_days() == null) {
            c.setWarn_days(30);
        }
        if (!StringUtils.hasText(c.getCert_id())) {
            c.setCert_id(newId());
            c.setCreator(me.getAccount());
            c.setOpen_status(true);
            certDao.insert(c);
        } else if (certDao.query(c.getCert_id()) == null) {
            return Result.fail(ResultCode.RC10103, "证照不存在");
        } else {
            certDao.update(c);
        }
        return Result.success();
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/cert/delete")
    public Result<?> certDelete(@RequestBody Map<String, Object> b) {
        return deleteSoft(str(b, "certId"), "证照不存在", certDao::query, id -> certDao.remove(id));
    }

    // ---------------- 基础信息：安全三同时 ----------------

    @RequirePermission("cbs:baseinfo")
    @PostMapping("/ts/list")
    public Result<?> tsList(@RequestBody(required = false) Map<String, Object> q) {
        return Result.success(threeSameDao.queryAll(str(q, "kw")));
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/ts/save")
    public Result<?> tsSave(@RequestBody TCbsThreeSame t, @Acc TCbsUser me) {
        if (!StringUtils.hasText(t.getProject_name())) {
            return Result.fail(ResultCode.RC10101, "项目名称不能为空");
        }
        if (!StringUtils.hasText(t.getTs_id())) {
            t.setTs_id(newId());
            t.setCreator(me.getAccount());
            t.setOpen_status(true);
            threeSameDao.insert(t);
        } else if (threeSameDao.query(t.getTs_id()) == null) {
            return Result.fail(ResultCode.RC10103, "三同时项目不存在");
        } else {
            threeSameDao.update(t);
        }
        return Result.success();
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/ts/delete")
    public Result<?> tsDelete(@RequestBody Map<String, Object> b) {
        return deleteSoft(str(b, "tsId"), "三同时项目不存在", threeSameDao::query, id -> threeSameDao.remove(id));
    }

    // ---------------- 基础信息：许可的危险化学品 ----------------

    @RequirePermission("cbs:baseinfo")
    @PostMapping("/chem/list")
    public Result<?> chemList(@RequestBody(required = false) Map<String, Object> q) {
        return Result.success(chemDao.queryAll(str(q, "kw")));
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/chem/save")
    public Result<?> chemSave(@RequestBody TCbsLicenseChem c, @Acc TCbsUser me) {
        if (!StringUtils.hasText(c.getChem_name())) {
            return Result.fail(ResultCode.RC10101, "化学品名称不能为空");
        }
        if (c.getIs_key() == null) {
            c.setIs_key(false);
        }
        if (!StringUtils.hasText(c.getChem_id())) {
            c.setChem_id(newId());
            c.setCreator(me.getAccount());
            c.setOpen_status(true);
            chemDao.insert(c);
        } else if (chemDao.query(c.getChem_id()) == null) {
            return Result.fail(ResultCode.RC10103, "化学品不存在");
        } else {
            chemDao.update(c);
        }
        return Result.success();
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/chem/delete")
    public Result<?> chemDelete(@RequestBody Map<String, Object> b) {
        return deleteSoft(str(b, "chemId"), "化学品不存在", chemDao::query, id -> chemDao.remove(id));
    }

    // ---------------- 基础信息：重点监管的危险化工工艺 ----------------

    @RequirePermission("cbs:baseinfo")
    @PostMapping("/proc/list")
    public Result<?> procList(@RequestBody(required = false) Map<String, Object> q) {
        return Result.success(procDao.queryAll(str(q, "kw")));
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/proc/save")
    public Result<?> procSave(@RequestBody TCbsSuperviseProcess p, @Acc TCbsUser me) {
        if (!StringUtils.hasText(p.getProc_name())) {
            return Result.fail(ResultCode.RC10101, "工艺名称不能为空");
        }
        if (p.getAuto_in_use() == null) {
            p.setAuto_in_use(false);
        }
        if (!StringUtils.hasText(p.getProc_id())) {
            p.setProc_id(newId());
            p.setCreator(me.getAccount());
            p.setOpen_status(true);
            procDao.insert(p);
        } else if (procDao.query(p.getProc_id()) == null) {
            return Result.fail(ResultCode.RC10103, "工艺不存在");
        } else {
            procDao.update(p);
        }
        return Result.success();
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/proc/delete")
    public Result<?> procDelete(@RequestBody Map<String, Object> b) {
        return deleteSoft(str(b, "procId"), "工艺不存在", procDao::query, id -> procDao.remove(id));
    }

    // ---------------- 私有工具 ----------------

    private static LocalDate parseDate(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }

    /** 通用软删：id 缺失或记录不存在即拒。 */
    private Result<?> deleteSoft(String id, String notFoundMsg,
                                 java.util.function.Function<String, ?> querier,
                                 java.util.function.Consumer<String> remover) {
        if (!StringUtils.hasText(id) || querier.apply(id) == null) {
            return Result.fail(ResultCode.RC10101, notFoundMsg);
        }
        remover.accept(id);
        return Result.success();
    }
}
