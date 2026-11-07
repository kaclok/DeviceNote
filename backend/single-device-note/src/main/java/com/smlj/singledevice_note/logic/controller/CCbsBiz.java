package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.annotation.Acc;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsCertDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsContractorDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsContractorQualDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsContractorChangeDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsContractorPunishDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractorChange;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractorPunish;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsDictDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCbsMetaDao;
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
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsContractorQual;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsDict;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsMeta;
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

import com.smlj.singledevice_note.logic.util.CbsApproveStatus;
import static com.smlj.singledevice_note.logic.util.CbsPage.offset;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageNo;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageOf;
import static com.smlj.singledevice_note.logic.util.CbsPage.pageSize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final TCbsContractorQualDao cqualDao;
    private final TCbsMetaDao metaDao;
    private final TCbsContractorChangeDao changeDao;
    private final TCbsContractorPunishDao punishDao;

    /** 通用基础数据表白名单（开工准备条件 / 资料清单），fail-closed */
    private static final java.util.Set<String> META_TABLES =
            java.util.Set.of("t_prep_condition", "t_doc_list");

    /** 资料清单分类（t_doc_list.file_type）：1=项目条件 2=开工前安全条件，fail-closed */
    private static final String TABLE_DOC_LIST = "t_doc_list";
    private static final java.util.Set<Integer> DOC_FILE_TYPES = java.util.Set.of(1, 2);

    /** 批量操作单次上限（与分页上限 200 对齐） */
    private static final int BATCH_MAX = 200;

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
        Integer status = intVal(q, "status");   // 审批状态：0 待审 / 1 通过 / 2 驳回（单位管理页四个切页）
        Integer coop = intVal(q, "coop");       // 合作状态：1 合作中 / 2 曾合作 / 3 黑名单（审核页三个切页）
        boolean recycled = q != null && Boolean.TRUE.equals(q.get("recycled"));
        long total = contractorDao.countAll(kw, status, coop, recycled, ctype);
        return Result.success(pageOf(contractorDao.queryPage(kw, status, coop, recycled, ctype, offset(no, size), size), total, no, size));
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
        if (c.getCoop_status() != null && (c.getCoop_status() < 1 || c.getCoop_status() > 3)) {
            return Result.fail(ResultCode.RC10101, "合作状态不合法");
        }
        if (StringUtils.hasText(c.getUscc())
                && contractorDao.countUsccDup(c.getUscc(), c.getContractor_id()) > 0) {
            return Result.fail(ResultCode.RC10101, "统一社会信用代码已存在");
        }
        if (!StringUtils.hasText(c.getContractor_id())) {
            c.setContractor_id(newId());
            c.setApprove_status(CbsApproveStatus.PENDING.getCode());
            if (c.getCoop_status() == null) {
                c.setCoop_status(1);
            }
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
            if (c.getCoop_status() == null) {
                c.setCoop_status(old.getCoop_status());
            }
            contractorDao.update(c);
        }
        return Result.success();
    }

    /** 审批状态流转：0 待审批 / 1 审批通过 / 2 未通过（未通过必填原因，其余状态清空原因）。 */
    @RequirePermission("cbs:audit")
    @Transactional
    @PostMapping("/contractor/audit")
    public Result<?> contractorAudit(@RequestBody Map<String, Object> b) {
        String id = str(b, "contractorId");
        Integer status = intVal(b, "status");
        String reason = str(b, "reason");
        CbsApproveStatus target = CbsApproveStatus.of(status);
        if (!StringUtils.hasText(id) || target == null) {
            return Result.fail(ResultCode.RC10101, "contractorId/status 不合法");
        }
        if (target.needReason() && !StringUtils.hasText(reason)) {
            return Result.fail(ResultCode.RC10101, "未通过必须填写原因");
        }
        if (contractorDao.query(id) == null) {
            return Result.fail(ResultCode.RC10103, "承包商不存在");
        }
        contractorDao.audit(id, target.getCode(), target.needReason() ? reason : null);
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

    /** 回收站恢复（软删逆操作） */
    @RequirePermission("cbs:contractor")
    @Transactional
    @PostMapping("/contractor/restore")
    public Result<?> contractorRestore(@RequestBody Map<String, Object> b) {
        String id = str(b, "contractorId");
        if (!StringUtils.hasText(id) || contractorDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "承包商不存在");
        }
        contractorDao.restore(id);
        return Result.success();
    }

    /** 彻底删除（物理删，参考站 deleteTruth；名下有项目仍拒绝） */
    @RequirePermission("cbs:contractor")
    @Transactional
    @PostMapping("/contractor/purge")
    public Result<?> contractorPurge(@RequestBody Map<String, Object> b) {
        String id = str(b, "contractorId");
        if (!StringUtils.hasText(id) || contractorDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "承包商不存在");
        }
        if (projectDao.countByContractor(id) > 0) {
            return Result.fail(ResultCode.RC10101, "该承包商名下存在项目，不能删除");
        }
        contractorDao.purge(id);
        return Result.success();
    }

    /** 保证金变更记录分页（弹窗列表），附带承包商名称与当前余额 */
    @RequirePermission("cbs:contractor")
    @PostMapping("/contractor/changeLog/list")
    public Result<?> changeLogList(@RequestBody Map<String, Object> q) {
        TCbsContractor c = requireContractor(str(q, "contractorId"));
        if (c == null) {
            return Result.fail(ResultCode.RC10103, "承包商不存在");
        }
        int no = pageNo(q), size = pageSize(q);
        long total = changeDao.countAll(c.getContractor_id());
        Map<String, Object> page = pageOf(changeDao.queryPage(c.getContractor_id(), offset(no, size), size), total, no, size);
        page.put("contractor", c);
        return Result.success(page);
    }

    /** 保证金变更：type 1=扣款 2=增加；事务内同步增减 t_contractor.deposit */
    @RequirePermission("cbs:contractor")
    @Transactional
    @PostMapping("/contractor/changeLog/save")
    public Result<?> changeLogSave(@RequestBody TCbsContractorChange p, @Acc TCbsUser me) {
        TCbsContractor c = requireContractor(p.getContractor_id());
        if (c == null) {
            return Result.fail(ResultCode.RC10103, "承包商不存在");
        }
        if (p.getType() == null || (p.getType() != 1 && p.getType() != 2)) {
            return Result.fail(ResultCode.RC10101, "变更类型不合法");
        }
        if (p.getMoney() == null || p.getMoney().signum() <= 0) {
            return Result.fail(ResultCode.RC10101, "变更金额必须大于 0");
        }
        BigDecimal delta = p.getType() == 1 ? p.getMoney().negate() : p.getMoney();
        BigDecimal balance = c.getDeposit() == null ? BigDecimal.ZERO : c.getDeposit();
        if (balance.add(delta).signum() < 0) {
            return Result.fail(ResultCode.RC10101, "扣款后保证金余额不能为负");
        }
        p.setId(newId());
        p.setCreator(me.getAccount());
        if (p.getChange_time() == null) {
            p.setChange_time(LocalDate.now());
        }
        changeDao.insert(p);
        contractorDao.changeDeposit(p.getContractor_id(), delta);
        return Result.success();
    }

    /** 处罚记录分页（弹窗列表），附带承包商名称与当前余额 */
    @RequirePermission("cbs:contractor")
    @PostMapping("/contractor/punish/list")
    public Result<?> punishList(@RequestBody Map<String, Object> q) {
        TCbsContractor c = requireContractor(str(q, "contractorId"));
        if (c == null) {
            return Result.fail(ResultCode.RC10103, "承包商不存在");
        }
        int no = pageNo(q), size = pageSize(q);
        long total = punishDao.countAll(c.getContractor_id());
        Map<String, Object> page = pageOf(punishDao.queryPage(c.getContractor_id(), offset(no, size), size), total, no, size);
        page.put("contractor", c);
        return Result.success(page);
    }

    /** 处罚表单下拉：该承包商名下启用项目 + 处罚类型字典（启用项） */
    @RequirePermission("cbs:contractor")
    @PostMapping("/contractor/punish/options")
    public Result<?> punishOptions(@RequestBody Map<String, Object> b) {
        String contractorId = str(b, "contractorId");
        if (!StringUtils.hasText(contractorId)) {
            return Result.fail(ResultCode.RC10101, "contractorId 不能为空");
        }
        return Result.success(Map.of(
                "projects", projectDao.queryOptionsByContractor(contractorId).stream()
                        .map(p -> Map.<String, Object>of("id", p.getProject_id(), "name", p.getProj_name()))
                        .toList(),
                "punishTypes", dictDao.queryOptions("penalty_type").stream()
                        .map(d -> Map.<String, Object>of("id", d.getItem_id(), "name", d.getItem_name()))
                        .toList()));
    }

    /** 新增处罚记录（附件/签字照片涉及文件服务，一期不做） */
    @RequirePermission("cbs:contractor")
    @Transactional
    @PostMapping("/contractor/punish/save")
    public Result<?> punishSave(@RequestBody TCbsContractorPunish p, @Acc TCbsUser me) {
        if (requireContractor(p.getContractor_id()) == null) {
            return Result.fail(ResultCode.RC10103, "承包商不存在");
        }
        if (!StringUtils.hasText(p.getProject_id()) || projectDao.query(p.getProject_id()) == null) {
            return Result.fail(ResultCode.RC10101, "关联项目不合法");
        }
        if (!StringUtils.hasText(p.getType_id())) {
            return Result.fail(ResultCode.RC10101, "处罚类型不能为空");
        }
        if (!StringUtils.hasText(p.getPunish_user())) {
            return Result.fail(ResultCode.RC10101, "违规人不能为空");
        }
        if (p.getMoney() == null || p.getMoney().signum() <= 0) {
            return Result.fail(ResultCode.RC10101, "处罚金额必须大于 0");
        }
        if (p.getPunish_time() == null) {
            return Result.fail(ResultCode.RC10101, "处罚时间不能为空");
        }
        p.setId(newId());
        p.setCreator(me.getAccount());
        punishDao.insert(p);
        return Result.success();
    }

    private TCbsContractor requireContractor(String id) {
        return StringUtils.hasText(id) ? contractorDao.query(id) : null;
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

    // ---------------- 特殊作业报备（原「在线抽查」，挂 cbs:spotcheck） ----------------

    /** 报备列表：报备人员 / 作业票类型 / 报备状态 / 报备日期区间 / 作业时间区间 */
    @RequirePermission("cbs:spotcheck")
    @PostMapping("/spotcheck/list")
    public Result<?> spotcheckList(@RequestBody(required = false) Map<String, Object> q) {
        int no = pageNo(q), size = pageSize(q);
        String reporter = str(q, "reporter");
        Integer workType = intVal(q, "workType"), status = intVal(q, "status");
        String rs = str(q, "reportStart"), re = str(q, "reportEnd");
        String bs = str(q, "beginStart"), be = str(q, "beginEnd");
        long total = spotcheckDao.countAll(reporter, workType, status, rs, re, bs, be);
        return Result.success(pageOf(spotcheckDao.queryPage(reporter, workType, status, rs, re, bs, be,
                offset(no, size), size), total, no, size));
    }

    /** 导出：与列表同条件取全量（最多 5000 行，Excel 由前端生成） */
    @RequirePermission("cbs:spotcheck")
    @PostMapping("/spotcheck/export")
    public Result<?> spotcheckExport(@RequestBody(required = false) Map<String, Object> q) {
        final int exportMax = 5000;
        List<TCbsSpotcheck> all = spotcheckDao.queryAll(str(q, "reporter"), intVal(q, "workType"),
                intVal(q, "status"), str(q, "reportStart"), str(q, "reportEnd"),
                str(q, "beginStart"), str(q, "beginEnd"));
        return Result.success(all.size() > exportMax ? all.subList(0, exportMax) : all);
    }

    @RequirePermission("cbs:spotcheck")
    @Transactional
    @PostMapping("/spotcheck/save")
    public Result<?> spotcheckSave(@RequestBody TCbsSpotcheck s, @Acc TCbsUser me) {
        String err = checkSpotcheck(s);
        if (err != null) {
            return Result.fail(ResultCode.RC10101, err);
        }
        if (!StringUtils.hasText(s.getCheck_id())) {
            s.setCheck_id(newId());
            s.setCreator(me.getAccount());
            s.setOpen_status(true);
            s.setReporter(me.getUsername());
            if (s.getStatus() == null) {
                s.setStatus(1);
            }
            if (s.getReport_date() == null) {
                s.setReport_date(LocalDate.now());
            }
            spotcheckDao.insert(s);
        } else {
            TCbsSpotcheck old = spotcheckDao.query(s.getCheck_id());
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "报备记录不存在");
            }
            if (s.getStatus() == null) {
                s.setStatus(old.getStatus());
            }
            spotcheckDao.update(s);
        }
        return Result.success();
    }

    /** 作废：已完成 / 已作废的报备不可再作废（与目标站一致） */
    @RequirePermission("cbs:spotcheck")
    @Transactional
    @PostMapping("/spotcheck/invalidate")
    public Result<?> spotcheckInvalidate(@RequestBody Map<String, Object> b) {
        String id = str(b, "checkId");
        TCbsSpotcheck old = StringUtils.hasText(id) ? spotcheckDao.query(id) : null;
        if (old == null) {
            return Result.fail(ResultCode.RC10101, "报备记录不存在");
        }
        if (old.getStatus() != null && (old.getStatus() == 2 || old.getStatus() == 3)) {
            return Result.fail(ResultCode.RC10101, "已完成或已作废的报备不可作废");
        }
        spotcheckDao.invalidate(id);
        return Result.success();
    }

    /** 删除：列表每行都有「删除」按钮，物理删除（含已作废/已完成的报备） */
    @RequirePermission("cbs:spotcheck")
    @Transactional
    @PostMapping("/spotcheck/delete")
    public Result<?> spotcheckDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "checkId");
        if (!StringUtils.hasText(id) || spotcheckDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "报备记录不存在");
        }
        spotcheckDao.delete(id);
        return Result.success();
    }

    /**
     * 批量报备：前端解析 Excel 后按行提交，逐行校验——合格行入库、不合格行带行号原因返回。
     * 单次上限 500 行（与分页上限同源思路，防止一次塞几万行）。
     */
    @RequirePermission("cbs:spotcheck")
    @Transactional
    @PostMapping("/spotcheck/importBatch")
    public Result<?> spotcheckImport(@RequestBody(required = false) Map<String, Object> b, @Acc TCbsUser me) {
        final int importMax = 500;
        Object raw = b == null ? null : b.get("rows");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return Result.fail(ResultCode.RC10101, "导入内容为空");
        }
        if (list.size() > importMax) {
            return Result.fail(ResultCode.RC10101, "单次最多导入 " + importMax + " 行");
        }
        int inserted = 0;
        List<Map<String, Object>> failed = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            int rowNo = i + 2; // 第 1 行是表头
            if (!(list.get(i) instanceof Map<?, ?>)) {
                failed.add(Map.<String, Object>of("row", rowNo, "msg", "行格式错误"));
                continue;
            }
            @SuppressWarnings("unchecked")
            TCbsSpotcheck s = spotFromRow((Map<String, Object>) list.get(i));
            String err = checkSpotcheck(s);
            if (err != null) {
                failed.add(Map.<String, Object>of("row", rowNo, "msg", err));
                continue;
            }
            s.setCheck_id(newId());
            s.setCreator(me.getAccount());
            s.setReporter(me.getUsername());
            s.setOpen_status(true);
            if (s.getStatus() == null) {
                s.setStatus(1);
            }
            if (s.getReport_date() == null) {
                s.setReport_date(LocalDate.now());
            }
            spotcheckDao.insert(s);
            inserted++;
        }
        return Result.success(Map.<String, Object>of("inserted", inserted, "failed", failed));
    }

    /** 报备表单校验：返回 null 表示通过，否则为提示语（前后端同一套口径，后端为准） */
    private static String checkSpotcheck(TCbsSpotcheck s) {
        if (s.getWork_type() == null || s.getWork_type() < 1 || s.getWork_type() > 15) {
            return "作业票类型不合法";
        }
        if (!StringUtils.hasText(s.getPlace())) {
            return "作业地点不能为空";
        }
        if (!StringUtils.hasText(s.getContent())) {
            return "作业内容不能为空";
        }
        if (s.getBegin_time() == null || s.getEnd_time() == null) {
            return "作业时间不能为空";
        }
        if (!s.getEnd_time().isAfter(s.getBegin_time())) {
            return "作业结束时间必须晚于开始时间";
        }
        if (!StringUtils.hasText(s.getUnit_name())) {
            return "施工单位/人员不能为空";
        }
        if (!StringUtils.hasText(s.getOperation_person())) {
            return "项目负责人不能为空";
        }
        if (!StringUtils.hasText(s.getOperation_phone())) {
            return "联系方式不能为空";
        }
        if (!s.getOperation_phone().matches("^1[3-9]\\d{9}$")) {
            return "联系方式格式不正确";
        }
        if (s.getStatus() != null && (s.getStatus() < 1 || s.getStatus() > 4)) {
            return "报备状态不合法";
        }
        return null;
    }

    /** 导入行 → 实体（前端已把中文枚举翻译成 id，这里只做类型兜底） */
    private static TCbsSpotcheck spotFromRow(Map<String, Object> m) {
        TCbsSpotcheck s = new TCbsSpotcheck();
        s.setWork_type(intVal(m, "work_type"));
        s.setPlace(str(m, "place"));
        s.setBegin_time(parseTime(str(m, "begin_time")));
        s.setEnd_time(parseTime(str(m, "end_time")));
        s.setIs_contractor_operate(boolVal(m, "is_contractor_operate"));
        s.setUnit_name(str(m, "unit_name"));
        s.setOperation_person(str(m, "operation_person"));
        s.setOperation_phone(str(m, "operation_phone"));
        s.setContent(str(m, "content"));
        s.setStatus(intVal(m, "status"));
        s.setReport_date(parseDate(str(m, "report_date")));
        return s;
    }

    private static Boolean boolVal(Map<String, Object> m, String key) {
        Object v = m == null ? null : m.get(key);
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        if (v == null) {
            return false;
        }
        String t = String.valueOf(v).trim();
        return "是".equals(t) || "true".equalsIgnoreCase(t) || "1".equals(t) || "Y".equalsIgnoreCase(t);
    }

    /** 解析 "yyyy-MM-dd HH:mm[:ss]" 或 ISO "yyyy-MM-ddTHH:mm[:ss]" */
    private static LocalDateTime parseTime(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim().replace(' ', 'T');
        try {
            return LocalDateTime.parse(t.length() == 16 ? t + ":00" : t);
        } catch (Exception e) {
            try {
                return LocalDate.parse(s.trim()).atStartOfDay();
            } catch (Exception ignore) {
                return null;
            }
        }
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
        Boolean recycled = q == null ? null : (Boolean) q.get("recycled");
        int no = pageNo(q), size = pageSize(q);
        long total = dictDao.countAll(type, onlyUsing, recycled);
        return Result.success(pageOf(dictDao.queryPage(type, onlyUsing, recycled, offset(no, size), size), total, no, size));
    }

    /**
     * 字典下拉选项（承包商类型 / 承包商处罚类型 / 项目类型）。
     * <p>
     * 与 /dict/list 的区别：不分页、只取启用项，专供业务表单的下拉使用。
     * 这几类字典由用户在「基础设置」里自行增减，所以选项必须现查，前端不得写死。
     * <p>
     * 权限：只读基础数据，任何已登录的 cbs 账号都可能要用（承包商页 cbs:contractor、
     * 项目页 cbs:project 的筛选与表单都要取），故不加 @RequirePermission；
     * 字典本身的增删改仍由 /dict/save、/dict/delete 上的 cbs:baseinfo 把守。
     */
    @PostMapping("/dict/options")
    public Result<?> dictOptions(@RequestBody(required = false) Map<String, Object> b) {
        String type = str(b, "dictType");
        if (!StringUtils.hasText(type) || !DICT_TYPES.contains(type)) {
            return Result.fail(ResultCode.RC10101, "dict_type 不合法");
        }
        List<Map<String, Object>> items = dictDao.queryOptions(type).stream()
                .map(d -> Map.<String, Object>of("id", d.getItem_id(), "name", d.getItem_name()))
                .toList();
        return Result.success(items);
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

    /** 回收站 → 恢复（open_status 置回 true） */
    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/dict/restore")
    public Result<?> dictRestore(@RequestBody Map<String, Object> b) {
        String id = str(b, "itemId");
        if (!StringUtils.hasText(id) || dictDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "字典项不存在");
        }
        dictDao.restore(id);
        return Result.success();
    }

    /** 回收站 → 彻底删除（物理删除，不可恢复） */
    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/dict/purge")
    public Result<?> dictPurge(@RequestBody Map<String, Object> b) {
        String id = str(b, "itemId");
        if (!StringUtils.hasText(id) || dictDao.query(id) == null) {
            return Result.fail(ResultCode.RC10101, "字典项不存在");
        }
        dictDao.purge(id);
        return Result.success();
    }

    // ---------------- 承包商类型所需资质（资质管理） ----------------

    @RequirePermission("cbs:baseinfo")
    @PostMapping("/cqual/list")
    public Result<?> cqualList(@RequestBody Map<String, Object> b) {
        String typeId = str(b, "typeId");
        if (!StringUtils.hasText(typeId)) {
            return Result.fail(ResultCode.RC10101, "typeId 不能为空");
        }
        Integer qualType = qualTypeOf(b.get("qualType"));
        if (qualType == null) {
            return Result.fail(ResultCode.RC10101, "qualType 只能是 1（资质管理）或 2（审核所需文件）");
        }
        return Result.success(cqualDao.listByType(typeId, qualType));
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/cqual/save")
    public Result<?> cqualSave(@RequestBody TCbsContractorQual q, @Acc TCbsUser me) {
        if (!StringUtils.hasText(q.getType_id())) {
            return Result.fail(ResultCode.RC10101, "所属承包商类型不能为空");
        }
        if (q.getQual_type() == null || (q.getQual_type() != 1 && q.getQual_type() != 2)) {
            return Result.fail(ResultCode.RC10101, "qualType 只能是 1（资质管理）或 2（审核所需文件）");
        }
        if (!StringUtils.hasText(q.getQual_name())) {
            return Result.fail(ResultCode.RC10101, q.getQual_type() == 2 ? "文件名称不能为空" : "资质名称不能为空");
        }
        if (q.getValidity() == null) q.setValidity(0);
        if (q.getValidity() < 0) {
            return Result.fail(ResultCode.RC10101, "有效期(月)不能为负数");
        }
        if (q.getIs_using() == null) q.setIs_using(true);
        if (StringUtils.hasText(q.getQual_id())) {
            cqualDao.update(q);
            return Result.success();
        }
        q.setQual_id(newId());
        q.setCreator(me.getAccount());
        q.setOpen_status(true);
        cqualDao.insert(q);
        return Result.success();
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/cqual/toggle")
    public Result<?> cqualToggle(@RequestBody Map<String, Object> b) {
        String id = str(b, "qualId");
        if (!StringUtils.hasText(id)) {
            return Result.fail(ResultCode.RC10101, "qualId 不能为空");
        }
        Object using = b.get("isUsing");
        if (!(using instanceof Boolean)) {
            return Result.fail(ResultCode.RC10101, "isUsing 必须是布尔值");
        }
        cqualDao.toggleUsing(id, (Boolean) using);
        return Result.success();
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/cqual/delete")
    public Result<?> cqualDelete(@RequestBody Map<String, Object> b) {
        String id = str(b, "qualId");
        if (!StringUtils.hasText(id)) {
            return Result.fail(ResultCode.RC10101, "qualId 不能为空");
        }
        cqualDao.delete(id);
        return Result.success();
    }

    /** qualType 白名单：1=资质管理 2=审核所需文件，其余一律 null（fail-closed） */
    private Integer qualTypeOf(Object v) {
        if (v == null) return null;
        int n;
        try {
            n = Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
        return (n == 1 || n == 2) ? n : null;
    }

    /** 资料清单分类：仅 t_doc_list 生效，白名单外一律 null（调用方据此 fail-closed） */
    private Integer docFileTypeOf(String table, Object v) {
        if (!TABLE_DOC_LIST.equals(table)) {
            return null;
        }
        Integer n;
        try {
            n = Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
        return DOC_FILE_TYPES.contains(n) ? n : null;
    }

    // ---------------- 通用基础数据（开工准备文件 / 资料清单） ----------------

    @RequirePermission("cbs:baseinfo")
    @PostMapping("/meta/list")
    public Result<?> metaList(@RequestBody Map<String, Object> q) {
        String table = str(q, "table");
        if (!META_TABLES.contains(table)) {
            return Result.fail(ResultCode.RC10101, "table 不合法");
        }
        String typeId = str(q, "typeId");
        if (!StringUtils.hasText(typeId)) {
            return Result.fail(ResultCode.RC10101, "typeId 不能为空");
        }
        Integer fileType = docFileTypeOf(table, q.get("fileType"));
        if (fileType == null && TABLE_DOC_LIST.equals(table)) {
            return Result.fail(ResultCode.RC10101, "资料清单分类不合法");
        }
        int no = pageNo(q), size = pageSize(q);
        String kw = str(q, "kw");
        long total = metaDao.countAll(table, typeId, fileType, kw);
        return Result.success(pageOf(metaDao.queryPage(table, typeId, fileType, kw, offset(no, size), size), total, no, size));
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/meta/save")
   public Result<?> metaSave(@RequestBody Map<String, Object> b, @Acc TCbsUser me) {
        String table = str(b, "table");
        if (!META_TABLES.contains(table)) {
            return Result.fail(ResultCode.RC10101, "table 不合法");
        }
        String typeId = str(b, "typeId");
        if (!StringUtils.hasText(typeId)) {
            return Result.fail(ResultCode.RC10101, "所属项目类型不能为空");
        }
        String id = str(b, "id");
        String name = str(b, "name");
        if (!StringUtils.hasText(name)) {
            return Result.fail(ResultCode.RC10101, "名称不能为空");
        }
        String remark = str(b, "remark");
        Integer fileType = null;
        if (!StringUtils.hasText(id)) {
            fileType = docFileTypeOf(table, b.get("fileType"));
            if (fileType == null && TABLE_DOC_LIST.equals(table)) {
                return Result.fail(ResultCode.RC10101, "资料清单分类不合法");
            }
        }
        if (StringUtils.hasText(id)) {
            TCbsMeta old = metaDao.query(table, id);
            if (old == null) {
                return Result.fail(ResultCode.RC10103, "记录不存在");
            }
            TCbsMeta m = new TCbsMeta();
            m.setId(id);
            m.setName(name);
            m.setRemark(remark);
            metaDao.update(table, m);
        } else {
            TCbsMeta m = new TCbsMeta();
            m.setId(newId());
            m.setType_id(typeId);
            m.setName(name);
            m.setRemark(remark);
            m.setCreator(me.getAccount());
            m.setOpen_status(true);
            m.setFile_type(fileType);
            metaDao.insert(table, m);
        }
        return Result.success();
    }

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/meta/delete")
    public Result<?> metaDelete(@RequestBody Map<String, Object> b) {
        String table = str(b, "table");
        String id = str(b, "id");
        if (!META_TABLES.contains(table)) {
            return Result.fail(ResultCode.RC10101, "table 不合法");
        }
        if (!StringUtils.hasText(id)) {
            return Result.fail(ResultCode.RC10101, "id 不能为空");
        }
        metaDao.remove(table, id);
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
        // certType：1=企业证书 2=企业人员证书；两类各自一页，缺省不过滤
        Integer certType = intVal(q, "certType");
        long total = certDao.countAll(kw, certType);
        return Result.success(pageOf(certDao.queryPage(kw, certType, offset(no, size), size), total, no, size));
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
        if (c.getCert_type() == null) {
            c.setCert_type(1);
        }
        if (c.getCert_type() == 2 && !StringUtils.hasText(c.getHolder_name())) {
            return Result.fail(ResultCode.RC10101, "人员证书必须填写持证人");
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
        if (c.getIs_poison() == null) {
            c.setIs_poison(false);
        }
        if (c.getIs_major_hazard() == null) {
            c.setIs_major_hazard(false);
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

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/chem/deleteBatch")
    public Result<?> chemDeleteBatch(@RequestBody Map<String, Object> b) {
        return deleteBatch(b, chemDao::removeBatch);
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
        if (p.getEsd_in_use() == null) {
            p.setEsd_in_use(false);
        }
        if (p.getSis_in_use() == null) {
            p.setSis_in_use(false);
        }
        if (p.getHazop_done() == null) {
            p.setHazop_done(false);
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

    @RequirePermission("cbs:baseinfo")
    @Transactional
    @PostMapping("/proc/deleteBatch")
    public Result<?> procDeleteBatch(@RequestBody Map<String, Object> b) {
        return deleteBatch(b, procDao::removeBatch);
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

    /** 批量软删：ids 去空去重，单次上限 BATCH_MAX（与分页上限一致），一条不剩即拒。 */
    private Result<?> deleteBatch(Map<String, Object> b, java.util.function.Consumer<List<String>> remover) {
        Object raw = b == null ? null : b.get("ids");
        if (!(raw instanceof List<?> list)) {
            return Result.fail(ResultCode.RC10101, "请选择要删除的记录");
        }
        List<String> ids = list.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(BATCH_MAX)
                .toList();
        if (ids.isEmpty()) {
            return Result.fail(ResultCode.RC10101, "请选择要删除的记录");
        }
        remover.accept(ids);
        return Result.success();
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
