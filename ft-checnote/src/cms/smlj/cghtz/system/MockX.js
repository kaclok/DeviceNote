import {LocalStorageService} from "@/framework/services/LocalStorageService.js"

/**
 * 合同台账 - 本地 Mock 数据层
 *
 * 用途：后端接口未开发时，SysX 请求失败后自动兜底到本模块，
 * 保证前端页面可以独立演示、联调。
 * 数据以 localStorage 持久化，刷新不丢失。
 * 后端就绪后，SysX 改为只走 ApiX，本模块可整体移除。
 */

const KEY = "cghtz_mock_db_v1"

/* ---------------- 演示账号 ---------------- */
/* auth 字段为权限码数组，与需求文档 §2.2 一致 */
const ACCOUNTS = [
    {
        account: "admin", password: "123456", realName: "管理员", dept: "信息中心",
        role: "超级管理员", status: 1, lastLogin: "2026-08-13 08:12",
        auth: ["auth.login", "contract.view", "contract.create", "contract.update", "contract.delete",
            "contract.import", "contract.export", "account.manage", "permission.assign", "notify.config"],
    },
    {
        account: "xuesj", password: "123456", realName: "薛少军", dept: "采购部",
        role: "录入员", status: 1, lastLogin: "2026-08-13 09:01",
        auth: ["auth.login", "contract.view", "contract.create", "contract.update", "contract.delete",
            "contract.import", "contract.export"],
    },
    {
        account: "zhangwei", password: "123456", realName: "张伟", dept: "采购部",
        role: "录入员", status: 1, lastLogin: "2026-08-12 17:40",
        auth: ["auth.login", "contract.view", "contract.create", "contract.update", "contract.delete",
            "contract.import", "contract.export"],
    },
    {
        account: "liting", password: "123456", realName: "李婷", dept: "财务部",
        role: "查看员", status: 1, lastLogin: "2026-08-13 10:22",
        auth: ["auth.login", "contract.view", "contract.export"],
    },
    {
        account: "wanghao", password: "123456", realName: "王浩", dept: "采购部",
        role: "录入员", status: 0, lastLogin: "2026-07-30 15:11",
        auth: ["auth.login", "contract.view", "contract.create", "contract.update",
            "contract.import"],
    },
]

/* ---------------- 演示合同数据（参照 Excel 台账） ---------------- */
const CONTRACTS = [
    {id: 1, no: "SMLJ-CG-CL-26200", name: "螺栓", signer: "薛少军", method: "网络询比价", supplier: "榆林景云五金机电设备有限公司", amount: 3836.92, signDate: "2026-04-02", payMethod: "货到票到3个月付款", payCycleMonths: 3, plannedAmount: 3836.92, stock: "已入库", remark: ""},
    {id: 2, no: "SMLJ-CG-CL-26218", name: "SKF 轴承", signer: "薛少军", method: "网络询比价", supplier: "西安科润工贸有限公司", amount: 7494.00, signDate: "2026-04-20", payMethod: "货到票到3个月付款", payCycleMonths: 3, plannedAmount: 7494.00, stock: "已入库", remark: ""},
    {id: 3, no: "SMLJ-CG-CL-26235", name: "不锈钢阀门", signer: "王浩", method: "招标", supplier: "陕西泵阀制造有限公司", amount: 85600.00, signDate: "2026-05-15", payMethod: "货到票到3个月付款", payCycleMonths: 3, plannedAmount: 85600.00, stock: "未入库", remark: ""},
    {id: 4, no: "SMLJ-CG-CL-26247", name: "电缆一批", signer: "李婷", method: "网络询比价", supplier: "西安电力物资有限公司", amount: 126500.00, signDate: "2026-06-10", payMethod: "票到2个月付款", payCycleMonths: 2, plannedAmount: 126500.00, stock: "部分入库", remark: ""},
    {id: 5, no: "SMLJ-CG-CL-26250", name: "液碱", signer: "张伟", method: "谈判", supplier: "陕西化工贸易有限公司", amount: 320000.00, signDate: "2026-05-28", payMethod: "货到票到2个月付款", payCycleMonths: 2, plannedAmount: 320000.00, stock: "已入库", remark: ""},
    {id: 6, no: "SMLJ-CG-CL-26255", name: "SKF 轴承", signer: "李婷", method: "网络询比价", supplier: "西安科润工贸有限公司", amount: 22482.00, signDate: "2026-03-12", payMethod: "货到票到3个月付款", payCycleMonths: 3, plannedAmount: 22482.00, stock: "已入库", remark: "已付款"},
    {id: 7, no: "SMLJ-CG-CL-26266", name: "润滑油", signer: "李婷", method: "网络询比价", supplier: "榆林华润油脂有限公司", amount: 18900.00, signDate: "2026-06-20", payMethod: "货到票到1个月付款", payCycleMonths: 1, plannedAmount: 18900.00, stock: "已入库", remark: ""},
    {id: 8, no: "SMLJ-CG-CL-26278", name: "仪表配件", signer: "王浩", method: "单一来源", supplier: "西安仪表厂", amount: 45600.00, signDate: "2026-07-05", payMethod: "货到票到3个月付款", payCycleMonths: 3, plannedAmount: 45600.00, stock: "未入库", remark: ""},
    {id: 9, no: "SMLJ-CG-CL-26290", name: "减速机", signer: "薛少军", method: "招标", supplier: "江苏国茂减速机股份有限公司", amount: 158000.00, signDate: "2026-07-18", payMethod: "货到票到3个月付款", payCycleMonths: 3, plannedAmount: 158000.00, stock: "未入库", remark: ""},
    {id: 10, no: "SMLJ-CG-CL-26301", name: "密封垫片", signer: "张伟", method: "网络询比价", supplier: "西安橡胶制品厂", amount: 8200.00, signDate: "2026-08-01", payMethod: "票到1个月付款", payCycleMonths: 1, plannedAmount: 8200.00, stock: "未入库", remark: ""},
    {id: 11, no: "SMLJ-CG-CL-26312", name: "管件一批", signer: "王浩", method: "网络询比价", supplier: "榆林景云五金机电设备有限公司", amount: 15000.00, signDate: "2026-08-08", payMethod: "货到票到2个月付款", payCycleMonths: 2, plannedAmount: 15000.00, stock: "未入库", remark: ""},
    {id: 12, no: "SMLJ-CG-CL-26320", name: "电动机", signer: "薛少军", method: "招标", supplier: "西安电机厂", amount: 96000.00, signDate: "2026-08-12", payMethod: "货到票到3个月付款", payCycleMonths: 3, plannedAmount: 96000.00, stock: "未入库", remark: ""},
]

/* ---------------- 提醒配置 ---------------- */
const NOTIFY_CONFIG = {
    dailyScanEnable: true,       // 每日自动扫描
    warnDays: 7,                // 预警天数：距计划付款日 N 天内进入"即将超期"
    soonWarnEnable: true,       // 即将超期预警推送
    repeatEnable: true,         // 超期重复提醒
    repeatDays: 3,              // 超期后每 3 天重复推送
    dailyLimitEnable: true,     // 单日推送上限
    template: "【合同超期提醒】合同编号 {no}（{name}），计划付款日期 {planDate}，已超期 {days} 天，请尽快处理。",
}

/* ---------------- 推送日志 ---------------- */
const NOTIFY_LOGS = [
    {id: 1, time: "2026-08-13 08:30:01", no: "SMLJ-CG-CL-26200", name: "螺栓", type: "已超期", receivers: "薛少军、张伟", status: "成功", detail: "i上陕投推送成功"},
    {id: 2, time: "2026-08-13 08:30:01", no: "SMLJ-CG-CL-26218", name: "SKF 轴承", type: "已超期", receivers: "薛少军、张伟", status: "成功", detail: "i上陕投推送成功"},
    {id: 3, time: "2026-08-13 08:30:02", no: "SMLJ-CG-CL-26235", name: "不锈钢阀门", type: "即将超期", receivers: "王浩、张伟", status: "成功", detail: "i上陕投推送成功"},
    {id: 4, time: "2026-08-13 08:30:02", no: "SMLJ-CG-CL-26247", name: "电缆一批", type: "即将超期", receivers: "李婷、张伟", status: "失败", detail: "接收人未注册 i上陕投"},
    {id: 5, time: "2026-08-13 08:30:03", no: "SMLJ-CG-CL-26250", name: "液碱", type: "已超期", receivers: "张伟、李婷", status: "成功", detail: "i上陕投推送成功"},
    {id: 6, time: "2026-08-12 08:30:01", no: "SMLJ-CG-CL-26266", name: "润滑油", type: "已超期", receivers: "李婷、张伟", status: "成功", detail: "i上陕投推送成功"},
]

/* ---------------- 权限码定义（前端展示用） ---------------- */
export const PERM_DEFS = [
    {code: "auth.login", name: "登录权限", group: "基础权限"},
    {code: "contract.view", name: "查看合同", group: "基础权限"},
    {code: "contract.create", name: "新增合同", group: "合同操作"},
    {code: "contract.update", name: "修改合同", group: "合同操作"},
    {code: "contract.delete", name: "作废合同", group: "合同操作"},
    {code: "contract.import", name: "Excel 导入", group: "合同操作"},
    {code: "contract.export", name: "Excel 导出", group: "合同操作"},
    {code: "account.manage", name: "账号管理", group: "系统管理"},
    {code: "permission.assign", name: "权限分配", group: "系统管理"},
    {code: "notify.config", name: "提醒设置", group: "系统管理"},
]

/* ---------------- 付款状态计算 ---------------- */
/**
 * 计划付款日期 = 签订时间 + 付款周期(月)
 */
function calcPlanDate(signDate, months) {
    if (!signDate) return "";
    const d = new Date(signDate);
    d.setMonth(d.getMonth() + (months || 0));
    const y = d.getFullYear(), m = String(d.getMonth() + 1).padStart(2, "0"), day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
}

/**
 * 付款状态：已付款 / 已超期 / 即将超期 / 未到期
 */
function calcPayStatus(contract, cfg) {
    if (contract.paid) return "已付款";
    const planDate = calcPlanDate(contract.signDate, contract.payCycleMonths);
    if (!planDate) return "未到期";
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const plan = new Date(planDate);
    const diffDays = Math.floor((plan - today) / 86400000);
    const warnDays = cfg?.warnDays ?? 7;
    if (diffDays < 0) return "已超期";
    if (diffDays <= warnDays) return "即将超期";
    return "未到期";
}

/* ---------------- Mock 单例 ---------------- */
function loadDB() {
    const raw = LocalStorageService.getStore(KEY);
    if (raw) {
        try {
            return JSON.parse(raw);
        } catch (e) {
            // 数据损坏，重置
        }
    }
    const db = {
        contracts: CONTRACTS.map(c => ({...c, paid: c.remark === "已付款"})),
        accounts: ACCOUNTS.map(a => ({...a})),
        notifyConfig: {...NOTIFY_CONFIG},
        notifyLogs: NOTIFY_LOGS.map(l => ({...l})),
        seq: 100,
    };
    saveDB(db);
    return db;
}

function saveDB(db) {
    LocalStorageService.setStore(KEY, JSON.stringify(db));
}

function ok(data) {
    return {code: __OK__, msg: "成功", data: data};
}

function makeId(db) {
    return ++db.seq;
}

export class MockX {

    /* ---------------- 认证 ---------------- */
    static login(account, password) {
        const db = loadDB();
        const user = db.accounts.find(a => a.account === account);
        if (!user) return ok({success: false, message: "账号不存在"});
        if (user.password !== password) return ok({success: false, message: "密码错误"});
        if (user.status !== 1) return ok({success: false, message: "账号已被停用"});
        user.lastLogin = new Date().toLocaleString("zh-CN", {hour12: false}).replace(/\//g, "-");
        saveDB(db);
        return ok({
            success: true,
            account: {account: user.account, realName: user.realName, dept: user.dept, role: user.role},
            auth: user.auth,
        });
    }

    /* ---------------- 合同台账 ---------------- */
    static getContractList(filters = {}) {
        const db = loadDB();
        let list = [...db.contracts];
        const cfg = db.notifyConfig;
        list.forEach(c => {
            c.planDate = calcPlanDate(c.signDate, c.payCycleMonths);
            c.payStatus = calcPayStatus(c, cfg);
        });
        // 筛选
        if (filters.no) list = list.filter(c => c.no.toLowerCase().includes(filters.no.toLowerCase()));
        if (filters.name) list = list.filter(c => c.name.includes(filters.name));
        if (filters.signer) list = list.filter(c => c.signer === filters.signer);
        if (filters.method) list = list.filter(c => c.method === filters.method);
        if (filters.supplier) list = list.filter(c => c.supplier.includes(filters.supplier));
        if (filters.payStatus) list = list.filter(c => c.payStatus === filters.payStatus);
        if (filters.stock) list = list.filter(c => c.stock === filters.stock);
        if (filters.dateFrom) list = list.filter(c => c.signDate >= filters.dateFrom);
        if (filters.dateTo) list = list.filter(c => c.signDate <= filters.dateTo);
        return ok(list);
    }

    static getContract(no) {
        const db = loadDB();
        const c = db.contracts.find(x => x.no === no);
        if (!c) return ok(null);
        const cfg = db.notifyConfig;
        return ok({
            ...c,
            planDate: calcPlanDate(c.signDate, c.payCycleMonths),
            payStatus: calcPayStatus(c, cfg),
        });
    }

    static checkNoExists(no) {
        const db = loadDB();
        return ok(!!db.contracts.find(c => c.no.toLowerCase() === no.toLowerCase()));
    }

    static createContract(paras) {
        const db = loadDB();
        if (db.contracts.find(c => c.no.toLowerCase() === paras.no.toLowerCase())) {
            return {code: __OK__, msg: `合同编号 ${paras.no} 已存在，禁止重复录入`, data: {duplicate: true}};
        }
        const c = {
            id: makeId(db),
            no: paras.no,
            name: paras.name,
            signer: paras.signer,
            method: paras.method,
            supplier: paras.supplier,
            amount: Number(paras.amount),
            signDate: paras.signDate,
            payMethod: paras.payMethod,
            payCycleMonths: Number(paras.payCycleMonths || 0),
            plannedAmount: paras.plannedAmount ? Number(paras.plannedAmount) : Number(paras.amount),
            stock: paras.stock || "未入库",
            paid: false,
            remark: paras.remark || "",
        };
        db.contracts.push(c);
        saveDB(db);
        return ok(c);
    }

    static updateContract(paras) {
        const db = loadDB();
        const idx = db.contracts.findIndex(c => c.no === paras.no);
        if (idx < 0) return {code: __OK__, msg: "合同不存在", data: {}};
        const old = db.contracts[idx];
        db.contracts[idx] = {
            ...old, ...paras,
            amount: Number(paras.amount),
            payCycleMonths: Number(paras.payCycleMonths || 0),
            plannedAmount: paras.plannedAmount ? Number(paras.plannedAmount) : Number(paras.amount),
        };
        saveDB(db);
        return ok(db.contracts[idx]);
    }

    static deleteContract(no) {
        const db = loadDB();
        db.contracts = db.contracts.filter(c => c.no !== no);
        saveDB(db);
        return ok(true);
    }

    static importContractExcel(rows) {
        // rows: [{no,name,signer,method,supplier,amount,signDate,payMethod,payCycleMonths,plannedAmount,stock}]
        const db = loadDB();
        const failRows = [];
        let okCnt = 0;
        rows.forEach((r, i) => {
            const no = String(r.no || "").trim();
            const reasons = [];
            if (!no) reasons.push("合同编号为空");
            else if (db.contracts.find(c => c.no.toLowerCase() === no.toLowerCase())) reasons.push("合同编号重复");
            if (!r.name) reasons.push("合同名称必填");
            if (!r.supplier) reasons.push("供应商必填");
            if (isNaN(Number(r.amount))) reasons.push("合同金额格式错误");
            if (!r.signDate) reasons.push("签订时间必填");
            if (reasons.length > 0) {
                failRows.push({row: i + 2, no: no || "-", name: r.name || "", reason: reasons.join("；")});
                return;
            }
            const c = {
                id: makeId(db),
                no: no,
                name: r.name,
                signer: r.signer || "",
                method: r.method || "网络询比价",
                supplier: r.supplier,
                amount: Number(r.amount),
                signDate: r.signDate,
                payMethod: r.payMethod || "货到票到3个月付款",
                payCycleMonths: Number(r.payCycleMonths || 0),
                plannedAmount: r.plannedAmount ? Number(r.plannedAmount) : Number(r.amount),
                stock: r.stock || "未入库",
                paid: false,
                remark: r.remark || "",
            };
            db.contracts.push(c);
            okCnt++;
        });
        saveDB(db);
        return ok({success: okCnt, fail: failRows.length, failRows: failRows});
    }

    /* ---------------- 账号与权限 ---------------- */
    static getAccountList() {
        const db = loadDB();
        return ok(db.accounts.map(({password, ...rest}) => rest));
    }

    static saveAccount(paras) {
        const db = loadDB();
        const idx = db.accounts.findIndex(a => a.account === paras.account);
        if (idx >= 0) {
            db.accounts[idx] = {...db.accounts[idx], ...paras, password: paras.password || db.accounts[idx].password};
        } else {
            db.accounts.push({...paras, password: paras.password || "123456", status: 1});
        }
        saveDB(db);
        return ok(true);
    }

    static resetPassword(account) {
        const db = loadDB();
        const a = db.accounts.find(x => x.account === account);
        if (a) a.password = "123456";
        saveDB(db);
        return ok(true);
    }

    static toggleAccountStatus(account) {
        const db = loadDB();
        const a = db.accounts.find(x => x.account === account);
        if (a) a.status = a.status === 1 ? 0 : 1;
        saveDB(db);
        return ok(a ? {account: a.account, status: a.status} : null);
    }

    /* ---------------- 提醒配置 & 推送日志 ---------------- */
    static getNotifyConfig() {
        const db = loadDB();
        return ok({...db.notifyConfig});
    }

    static saveNotifyConfig(cfg) {
        const db = loadDB();
        db.notifyConfig = {...db.notifyConfig, ...cfg};
        saveDB(db);
        return ok({...db.notifyConfig});
    }

    static getNotifyLogs(filters = {}) {
        const db = loadDB();
        let list = [...db.notifyLogs];
        if (filters.no) list = list.filter(l => l.no.includes(filters.no));
        if (filters.status) list = list.filter(l => l.status === filters.status);
        return ok(list);
    }

    static retryNotify(id) {
        const db = loadDB();
        const log = db.notifyLogs.find(l => l.id === Number(id));
        if (log) {
            log.status = "成功";
            log.detail = "重试推送成功";
        }
        saveDB(db);
        return ok(log);
    }

    /* ---------------- 统计 ---------------- */
    static getStats() {
        const db = loadDB();
        const cfg = db.notifyConfig;
        const stats = {overdue: 0, soon: 0, normal: 0, paid: 0, total: db.contracts.length};
        db.contracts.forEach(c => {
            const s = calcPayStatus(c, cfg);
            if (s === "已超期") stats.overdue++;
            else if (s === "即将超期") stats.soon++;
            else if (s === "已付款") stats.paid++;
            else stats.normal++;
        });
        return ok(stats);
    }
}
