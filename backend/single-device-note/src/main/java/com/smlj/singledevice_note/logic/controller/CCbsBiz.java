package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.annotation.Acc;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsCertDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsContractorDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsDictDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsEnterpriseDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsLicenseChemDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsPostDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsProjectAcceptDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsProjectDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsProjectEnterDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsProjectPrepDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsPromiseDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsSpotcheckDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsSuperviseProcessDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsThreeSameDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsCert;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractor;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsDict;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsEnterprise;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsLicenseChem;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsPost;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsProject;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsProjectAccept;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsProjectEnter;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsProjectPrep;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsPromise;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsSpotcheck;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsSuperviseProcess;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsThreeSame;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;

import static com.smlj.singledevice_note.logic.util.CbsPage.offset;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageNo;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageOf;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageSize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
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
    private final TCbsPostDao postDao;
    private final TCbsSpotcheckDao spotcheckDao;
    private final TCbsProjectEnterDao enterDao;
    private final TCbsProjectPrepDao prepDao;
    private final TCbsProjectAcceptDao acceptDao;
    private final TCbsDictDao dictDao;

    /** 类型字典合法键（fail-closed，清单第21~23项） */
    private static final java.util.Set<String> DICT_TYPES =
            java.util.Set.of("contractor_type", "penalty_type", "project_type");

    private static String newId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    // 分页口径统一走 CbsPage（见 import static）


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
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw"), ctype = str(q, "ctype");
        Integer status = intVal(q, "status");
        long total = contractorDao.countAll(kw, status, ctype);
        return Result.success(pageOf(contractorDao.queryPage(kw, status, ctype, offset(no, size), size), total, no, size));
    }

    /** 下拉专用（不分页，只出启用且审批通过的单位） */
    @RequirePermission("cbs:contractor")
    @PostMapping("/contractor/options")
    public Result<?> contractorOptions() {
        return Result.success(contractorDao.queryOptions());
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
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw"), contractorId = str(q, "contractorId");
        Integer status = intVal(q, "projStatus");
        long total = projectDao.countAll(kw, contractorId, status);
        return Result.success(pageOf(projectDao.queryPage(kw, contractorId, status, offset(no, size), size), total, no, size));
    }

    /** 下拉专用（不分页） */
    @RequirePermission("cbs:project")
    @PostMapping("/project/options")
    public Result<?> projectOptions() {
        return Result.success(projectDao.queryOptions());
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

    // ---------------- 项目子档：入场管理 / 开工准备 / 项目验收 ----------------

    @RequirePermission("cbs:project")
    @PostMapping("/project/enter/list")
    public Result<?> enterList(@RequestBody(required = false) Map<String, Object> q) {
        int no = pageNo(q), size = pageSize(q);
        String projectId = str(q, "projectId");
        long total = enterDao.countAll(projectId);
        return Result.success(pageOf(enterDao.queryPage(projectId, offset(no, size), size), total, no, size));
    }

    @RequirePermission("cbs:project")
    @Transactional
    @PostMapping("/project/enter/save")
    public Result<?> enterSave(@RequestBody TCbsProjectEnter e, @Acc TCbsUser me) {
        if (!StringUtils.hasText(e.getProject_id()) || projectDao.query(e.getProject_id()) == null) {
            return Result.fail(ResultCode.RC10101, "关联项目不存在");
        }
        if (!StringUtils.hasText(e.getEnter_id())) {
            e.setEnter_id(newId());
            e.setCreator(me.getAccount());
            e.setOpen_status(true);
            enterDao.insert(e);
        } else {
            TCbsProjectEnter old = enterDao.query(e.getEnter_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "入场记录不存在");
            }
            e.setCreator(old.getCreator());
            e.setOpen_status(old.getOpen_status());
            enterDao.update(e);
        }
        return Result.success();
    }

    @RequirePermission("cbs:project")
    @Transactional
    @PostMapping("/project/enter/delete")
    public Result<?> enterDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "enterId");
        if (!StringUtils.hasText(id) || enterDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "入场记录不存在");
        }
        enterDao.remove(id);
        return Result.success();
    }

    @RequirePermission("cbs:project")
    @PostMapping("/project/prep/list")
    public Result<?> prepList(@RequestBody(required = false) Map<String, Object> q) {
        int no = pageNo(q), size = pageSize(q);
        String projectId = str(q, "projectId");
        long total = prepDao.countAll(projectId);
        return Result.success(pageOf(prepDao.queryPage(projectId, offset(no, size), size), total, no, size));
    }

    @RequirePermission("cbs:project")
    @Transactional
    @PostMapping("/project/prep/save")
    public Result<?> prepSave(@RequestBody TCbsProjectPrep p, @Acc TCbsUser me) {
        if (!StringUtils.hasText(p.getProject_id()) || projectDao.query(p.getProject_id()) == null) {
            return Result.fail(ResultCode.RC10101, "关联项目不存在");
        }
        if (!StringUtils.hasText(p.getContent())) {
            return Result.fail(ResultCode.RC10101, "准备事项不能为空");
        }
        if (p.getStatus() == null) {
            p.setStatus(0);
        }
        if (!StringUtils.hasText(p.getPrep_id())) {
            p.setPrep_id(newId());
            p.setCreator(me.getAccount());
            p.setOpen_status(true);
            prepDao.insert(p);
        } else {
            TCbsProjectPrep old = prepDao.query(p.getPrep_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "开工准备记录不存在");
            }
            p.setCreator(old.getCreator());
            p.setOpen_status(old.getOpen_status());
            prepDao.update(p);
        }
        return Result.success();
    }

    @RequirePermission("cbs:project")
    @Transactional
    @PostMapping("/project/prep/delete")
    public Result<?> prepDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "prepId");
        if (!StringUtils.hasText(id) || prepDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "开工准备记录不存在");
        }
        prepDao.remove(id);
        return Result.success();
    }

    @RequirePermission("cbs:project")
    @PostMapping("/project/accept/list")
    public Result<?> acceptList(@RequestBody(required = false) Map<String, Object> q) {
        int no = pageNo(q), size = pageSize(q);
        String projectId = str(q, "projectId");
        long total = acceptDao.countAll(projectId);
        return Result.success(pageOf(acceptDao.queryPage(projectId, offset(no, size), size), total, no, size));
    }

    @RequirePermission("cbs:project")
    @Transactional
    @PostMapping("/project/accept/save")
    public Result<?> acceptSave(@RequestBody TCbsProjectAccept a, @Acc TCbsUser me) {
        if (!StringUtils.hasText(a.getProject_id()) || projectDao.query(a.getProject_id()) == null) {
            return Result.fail(ResultCode.RC10101, "关联项目不存在");
        }
        if (a.getResult() == null) {
            a.setResult(1);
        }
        if (!StringUtils.hasText(a.getAccept_id())) {
            a.setAccept_id(newId());
            a.setCreator(me.getAccount());
            a.setOpen_status(true);
            acceptDao.insert(a);
        } else {
            TCbsProjectAccept old = acceptDao.query(a.getAccept_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "验收记录不存在");
            }
            a.setCreator(old.getCreator());
            a.setOpen_status(old.getOpen_status());
            acceptDao.update(a);
        }
        return Result.success();
    }

    @RequirePermission("cbs:project")
    @Transactional
    @PostMapping("/project/accept/delete")
    public Result<?> acceptDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "acceptId");
        if (!StringUtils.hasText(id) || acceptDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "验收记录不存在");
        }
        acceptDao.remove(id);
        return Result.success();
    }

    // ---------------- 岗位管理（挂 cbs:org） ----------------

    @RequirePermission("cbs:org")
    @PostMapping("/post/list")
    public Result<?> postList(@RequestBody(required = false) Map<String, Object> q) {
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw");
        long total = postDao.countAll(kw);
        return Result.success(pageOf(postDao.queryPage(kw, offset(no, size), size), total, no, size));
    }

    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/post/save")
    public Result<?> postSave(@RequestBody TCbsPost p, @Acc TCbsUser me) {
        if (!StringUtils.hasText(p.getPost_code()) || !StringUtils.hasText(p.getPost_name())) {
            return Result.fail(ResultCode.RC10101, "岗位编码/名称不能为空");
        }
        if (postDao.countCode(p.getPost_code(), p.getPost_id()) > 0) {
            return Result.fail(ResultCode.RC10101, "岗位编码已存在");
        }
        if (p.getPost_status() == null) {
            p.setPost_status(1);
        }
        if (!StringUtils.hasText(p.getPost_id())) {
            p.setPost_id(newId());
            p.setCreator(me.getAccount());
            p.setOpen_status(true);
            postDao.insert(p);
        } else {
            TCbsPost old = postDao.query(p.getPost_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "岗位不存在");
            }
            p.setCreator(old.getCreator());
            p.setOpen_status(old.getOpen_status());
            postDao.update(p);
        }
        return Result.success();
    }

    @RequirePermission("cbs:org")
    @Transactional
    @PostMapping("/post/delete")
    public Result<?> postDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "postId");
        if (!StringUtils.hasText(id) || postDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "岗位不存在");
        }
        postDao.remove(id);
        return Result.success();
    }

    // ---------------- 在线抽查（特殊作业报备，挂 cbs:spotcheck） ----------------

    @RequirePermission("cbs:spotcheck")
    @PostMapping("/spotcheck/list")
    public Result<?> spotcheckList(@RequestBody(required = false) Map<String, Object> q) {
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw"), contractorId = str(q, "contractorId");
        Integer workType = intVal(q, "workType");
        long total = spotcheckDao.countAll(kw, contractorId, workType);
        return Result.success(pageOf(spotcheckDao.queryPage(kw, contractorId, workType, offset(no, size), size), total, no, size));
    }

    @RequirePermission("cbs:spotcheck")
    @Transactional
    @PostMapping("/spotcheck/save")
    public Result<?> spotcheckSave(@RequestBody TCbsSpotcheck s, @Acc TCbsUser me) {
        if (s.getWork_type() == null || s.getWork_type() < 1 || s.getWork_type() > 8) {
            return Result.fail(ResultCode.RC10101, "特殊作业类型不合法");
        }
        if (!StringUtils.hasText(s.getSpot_content())) {
            return Result.fail(ResultCode.RC10101, "抽查内容不能为空");
        }
        if (s.getResult() == null) {
            s.setResult(1);
        }
        if (s.getResult() == 0 && s.getRectify_status() == null) {
            s.setRectify_status(1);
        }
        if (s.getRectify_status() == null) {
            s.setRectify_status(0);
        }
        if (StringUtils.hasText(s.getContractor_id()) && contractorDao.query(s.getContractor_id()) == null) {
            return Result.fail(ResultCode.RC10101, "被抽查承包商不存在");
        }
        if (!StringUtils.hasText(s.getCheck_id())) {
            s.setCheck_id(newId());
            s.setCreator(me.getAccount());
            s.setOpen_status(true);
            spotcheckDao.insert(s);
        } else {
            TCbsSpotcheck old = spotcheckDao.query(s.getCheck_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "抽查记录不存在");
            }
            s.setCreator(old.getCreator());
            s.setOpen_status(old.getOpen_status());
            spotcheckDao.update(s);
        }
        return Result.success();
    }

    @RequirePermission("cbs:spotcheck")
    @Transactional
    @PostMapping("/spotcheck/delete")
    public Result<?> spotcheckDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "checkId");
        if (!StringUtils.hasText(id) || spotcheckDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "抽查记录不存在");
        }
        spotcheckDao.remove(id);
        return Result.success();
    }

    // ---------------- 类型字典（挂 cbs:baseinfo，清单第21~23项） ----------------

    @RequirePermission("cbs:baseinfo")
    @PostMapping("/dict/list")
    public Result<?> dictList(@RequestBody(required = false) Map<String, Object> q) {
        String type = str(q, "dictType");
        if (StringUtils.hasText(type) && !DICT_TYPES.contains(type)) {
            return Result.fail(ResultCode.RC10101, "dict_type 不合法");
        }
        boolean onlyUsing = Boolean.parseBoolean(String.valueOf(q == null ? null : q.get("onlyUsing")));
        int no = pageNo(q), size = pageSize(q);
        long total = dictDao.countAll(type, onlyUsing);
        return Result.success(pageOf(dictDao.queryPage(type, onlyUsing, offset(no, size), size), total, no, size));
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/dict/save")
    public Result<?> dictSave(@RequestBody TCbsDict d, @Acc TCbsUser me) {
        if (!DICT_TYPES.contains(d.getDict_type())) {
            return Result.fail(ResultCode.RC10101, "dict_type 不合法");
        }
        if (!StringUtils.hasText(d.getItem_name())) {
            return Result.fail(ResultCode.RC10101, "字典项名称不能为空");
        }
        if (dictDao.countName(d.getDict_type(), d.getItem_name(), d.getItem_id()) > 0) {
            return Result.fail(ResultCode.RC10101, "该类型下字典项名称已存在");
        }
        if (d.getSort_no() == null) {
            d.setSort_no(0);
        }
        if (d.getIs_using() == null) {
            d.setIs_using(true);
        }
        if (!StringUtils.hasText(d.getItem_id())) {
            d.setItem_id(newId());
            d.setCreator(me.getAccount());
            d.setOpen_status(true);
            dictDao.insert(d);
        } else {
            TCbsDict old = dictDao.query(d.getItem_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "字典项不存在");
            }
            d.setCreator(old.getCreator());
            d.setOpen_status(old.getOpen_status());
            dictDao.update(d);
        }
        return Result.success();
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/dict/delete")
    public Result<?> dictDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "itemId");
        if (!StringUtils.hasText(id) || dictDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "字典项不存在");
        }
        dictDao.remove(id);
        return Result.success();
    }

    // ---------------- 安全承诺日报 ----------------

    /** 承诺列表：按日期倒序分页，一天一行（目标站「安全承诺」列表页） */
    @RequirePermission("cbs:promise")
    @PostMapping("/promise/list")
    public Result<?> promiseList(@RequestBody(required = false) Map<String, Object> q) {
        int no = pageNo(q), size = pageSize(q);
        String date = str(q, "date");
        long total = promiseDao.countAll(date);
        return Result.success(pageOf(promiseDao.queryPage(date, offset(no, size), size), total, no, size));
    }

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
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw");
        long total = certDao.countAll(kw);
        return Result.success(pageOf(certDao.queryPage(kw, offset(no, size), size), total, no, size));
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
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw");
        long total = threeSameDao.countAll(kw);
        return Result.success(pageOf(threeSameDao.queryPage(kw, offset(no, size), size), total, no, size));
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
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw");
        long total = chemDao.countAll(kw);
        return Result.success(pageOf(chemDao.queryPage(kw, offset(no, size), size), total, no, size));
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
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw");
        long total = procDao.countAll(kw);
        return Result.success(pageOf(procDao.queryPage(kw, offset(no, size), size), total, no, size));
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
