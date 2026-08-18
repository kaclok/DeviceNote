import {LocalStorageService} from "@/framework/services/LocalStorageService.js"
import gd from "../data/gd.json"
import hd from "../data/hd.json"

/**
 * 合同台账 - 本地 Mock 数据层
 *
 * 用途：后端接口未开发时，SysX 请求失败后自动兜底到本模块，
 * 保证前端页面可以独立演示、联调。
 * 数据以 localStorage 持久化，刷新不丢失。
 * 后端就绪后，SysX 改为只走 ApiX，本模块可整体移除。
 *
 * 数据动静分离：
 * - data/gd.json：固定数据（权限码字典、签订方式/付款方式枚举等前端固定配置）
 * - data/hd.json：将来后端下发的数据（账号、合同、提醒配置、推送日志）
 */

const KEY = "cghtz_mock_db_v1"

/* ---------------- 固定数据（来自 gd.json） ---------------- */
/* 权限码定义（前端展示用），与需求文档 §2.2 一致 */
export const PERM_DEFS = gd.permDefs
export const METHOD_OPTIONS = gd.methodOptions
export const PAY_METHOD_OPTIONS = gd.payMethodOptions

/* ---------------- 后端下发数据（来自 hd.json，将来由后端接口返回） ---------------- */
const ACCOUNTS = hd.accounts
const CONTRACTS = hd.contracts

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
 * 预警天数固定为 7（原提醒配置已移除，如需可配置可恢复）
 */
function calcPayStatus(contract) {
    if (contract.paid) return "已付款";
    const planDate = calcPlanDate(contract.signDate, contract.payCycleMonths);
    if (!planDate) return "未到期";
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const plan = new Date(planDate);
    const diffDays = Math.floor((plan - today) / 86400000);
    if (diffDays < 0) return "已超期";
    if (diffDays <= 7) return "即将超期";
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
            auth: user.role.perms,
        });
    }

    /* ---------------- 合同台账 ---------------- */
    static getContractList(filters = {}) {
        const db = loadDB();
        let list = [...db.contracts];
        list.forEach(c => {
            c.planDate = calcPlanDate(c.signDate, c.payCycleMonths);
            c.payStatus = calcPayStatus(c);
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
        return ok({
            ...c,
            planDate: calcPlanDate(c.signDate, c.payCycleMonths),
            payStatus: calcPayStatus(c),
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

    /* ---------------- 统计 ---------------- */
    static getStats() {
        const db = loadDB();
        const stats = {overdue: 0, soon: 0, normal: 0, paid: 0, total: db.contracts.length};
        db.contracts.forEach(c => {
            const s = calcPayStatus(c);
            if (s === "已超期") stats.overdue++;
            else if (s === "即将超期") stats.soon++;
            else if (s === "已付款") stats.paid++;
            else stats.normal++;
        });
        return ok(stats);
    }
}
