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
 * - data/gd.json：固定数据（签订方式枚举等前端固定配置）
 * - data/hd.json：将来后端下发的数据（角色、权限码字典、账号、合同）
 *
 * 合同字段（v3 - 对齐后端 TCGHTContract 24 字段）：
 *   基本：id / title / amount / date_sign / sign_person / sign_type / supplier
 *   付款：pay_type / paycycle_dh / paycycle_zb
 *         rate_yfk / rate_dhk / rate_zbj
 *         date_yfk / date_dhk / date_zbj / date_rk
 *   其他：bz / settle_amount / hq
 *   移交：date_htyj / date_fpyj / date_actual_dh / date_ruzlyj
 */

const KEY = "cghtz_mock_db_v4"

/* ---------------- 固定数据（来自 gd.json） ---------------- */
export const METHOD_OPTIONS = gd.methodOptions

/* ---------------- 后端下发数据（来自 hd.json，将来由后端接口返回） ---------------- */
const ROLES = hd.roles
const PERM_DEFS = hd.permDefs
const SIGNERS = hd.signers
const ACCOUNTS = hd.accounts
const CONTRACTS = hd.contracts

/* ---------------- 合同字段白名单（用于 create/update/import 接收合法字段） ---------------- */
const CONTRACT_FIELDS = [
    "id", "title", "amount", "date_sign", "sign_person", "sign_type", "supplier",
    "pay_type", "paycycle_dh", "paycycle_zb",
    "rate_yfk", "rate_dhk", "rate_zbj",
    "date_yfk", "date_dhk", "date_zbj", "date_rk",
    "bz", "settle_amount", "hq",
    "date_htyj", "date_fpyj", "date_actual_dh", "date_ruzlyj",
    "finish_step", "has_rk",
]

/* 浮点字段（导入/创建时统一转 Number） */
const FLOAT_FIELDS = [
    "amount", "paycycle_dh", "paycycle_zb",
    "rate_yfk", "rate_dhk", "rate_zbj",
    "settle_amount",
]
/* 整数字段 */
const INT_FIELDS = ["hq", "sign_type", "finish_step"]
/* 布尔字段 */
const BOOL_FIELDS = ["has_rk"]

/**
 * 将外部传入的合同对象按字段白名单清洗并强制类型转换
 */
function normalizeContract(raw = {}) {
    const out = {}
    CONTRACT_FIELDS.forEach(k => {
        if (raw[k] === undefined || raw[k] === null || raw[k] === "") {
            if (FLOAT_FIELDS.includes(k) || INT_FIELDS.includes(k)) out[k] = 0
            else if (BOOL_FIELDS.includes(k)) out[k] = false
            else out[k] = ""
            return
        }
        if (FLOAT_FIELDS.includes(k)) out[k] = Number(raw[k]) || 0
        else if (INT_FIELDS.includes(k)) out[k] = parseInt(raw[k], 10) || 0
        else if (BOOL_FIELDS.includes(k)) out[k] = raw[k] === true || raw[k] === 'true' || raw[k] === 1
        else out[k] = raw[k]
    })
    return out
}

/* ---------------- Mock 单例 ---------------- */
function loadDB() {
    const raw = LocalStorageService.getStore(KEY)
    if (raw) {
        try {
            return JSON.parse(raw)
        } catch (e) {
            // 数据损坏，重置
        }
    }
    const db = {
        roles: ROLES.map(r => ({...r, perms: [...r.perms]})),
        signers: SIGNERS.map(s => ({...s})),
        contracts: CONTRACTS.map(c => normalizeContract(c)),
        accounts: ACCOUNTS.map(a => ({...a})),
    }
    saveDB(db)
    return db
}

function saveDB(db) {
    LocalStorageService.setStore(KEY, JSON.stringify(db));
}

function ok(data) {
    return {code: __OK__, msg: "成功", data: data};
}

export class MockX {

    /* ---------------- 认证 ---------------- */
    static login(account, password) {
        const db = loadDB();
        const user = db.accounts.find(a => a.account === account);
        if (!user) return ok({success: false, message: "账号不存在"});
        if (user.password !== password) return ok({success: false, message: "密码错误"});
        if (user.status !== 1) return ok({success: false, message: "账号已被停用"});
        // 按 role_code 关联角色，登录响应中带上完整 role（含 perms）供路由守卫使用
        const role = db.roles.find(r => r.role_code === user.role_code) || {role_code: user.role_code, role_name: "未知", perms: []};
        user.lastLogin = new Date().toLocaleString("zh-CN", {hour12: false}).replace(/\//g, "-");
        saveDB(db);
        return ok({
            success: true,
            account: {account: user.account, username: user.username, role},
            auth: role.perms,
        });
    }

    /* ---------------- 合同台账 ---------------- */
    static getContractList(filters = {}) {
        const db = loadDB();
        let list = db.contracts.map(c => ({...c}));
        if (filters.id) list = list.filter(c => c.id.toLowerCase().includes(filters.id.toLowerCase()));
        if (filters.title) list = list.filter(c => c.title.includes(filters.title));
        if (filters.sign_person) list = list.filter(c => c.sign_person === filters.sign_person);
        if (filters.sign_type !== undefined && filters.sign_type !== '' && filters.sign_type !== null) {
            list = list.filter(c => c.sign_type === Number(filters.sign_type));
        }
        if (filters.supplier) list = list.filter(c => c.supplier.includes(filters.supplier));
        if (filters.dateFrom) list = list.filter(c => c.date_sign >= filters.dateFrom);
        if (filters.dateTo) list = list.filter(c => c.date_sign <= filters.dateTo);
        if (filters.finish_step !== undefined && filters.finish_step !== '' && filters.finish_step !== null) {
            const fv = Number(filters.finish_step) || 0
            list = list.filter(c => Number(c.finish_step) === fv)
        }
        if (filters.has_rk !== undefined && filters.has_rk !== '' && filters.has_rk !== null) {
            list = list.filter(c => c.has_rk === (filters.has_rk === true || filters.has_rk === 'true' || filters.has_rk === 1));
        }
        const total = list.length;
        // 分页（pageNum 1-based；pageSize <= 0 或 pageNum <= 0 视为不分页，对齐后端 PageHelper reasonable + pageSizeZero）
        const pageNum = Number(filters.pageNum) || 0;
        const pageSize = Number(filters.pageSize) || 0;
        if (pageNum > 0 && pageSize > 0) {
            const start = (pageNum - 1) * pageSize;
            list = list.slice(start, start + pageSize);
        }
        return ok({list, total});
    }

    static getContract(id) {
        const db = loadDB();
        const c = db.contracts.find(x => x.id === id);
        if (!c) return ok(null);
        return ok({...c});
    }

    static checkIdExists(id) {
        const db = loadDB();
        return ok(!!db.contracts.find(c => c.id.toLowerCase() === id.toLowerCase()));
    }

    static createContract(paras) {
        const db = loadDB();
        if (db.contracts.find(c => c.id.toLowerCase() === paras.id.toLowerCase())) {
            return {code: __OK__, msg: `合同编号 ${paras.id} 已存在，禁止重复录入`, data: {duplicate: true}};
        }
        const c = normalizeContract(paras);
        db.contracts.push(c);
        saveDB(db);
        return ok(c);
    }

    static updateContract(paras) {
        const db = loadDB();
        const idx = db.contracts.findIndex(c => c.id === paras.id);
        if (idx < 0) return {code: __OK__, msg: "合同不存在", data: {}};
        const old = db.contracts[idx];
        db.contracts[idx] = normalizeContract({...old, ...paras});
        saveDB(db);
        return ok(db.contracts[idx]);
    }

    static deleteContract(id) {
        const db = loadDB();
        db.contracts = db.contracts.filter(c => c.id !== id);
        saveDB(db);
        return ok(true);
    }

    static importContractExcel(rows) {
        const db = loadDB();
        const failRows = [];
        let okCnt = 0;
        rows.forEach((r, i) => {
            const id = String(r.id || "").trim();
            const reasons = [];
            if (!id) reasons.push("合同编号为空");
            else if (db.contracts.find(c => c.id.toLowerCase() === id.toLowerCase())) reasons.push("合同编号重复");
            if (!r.title) reasons.push("合同名称必填");
            if (!r.supplier) reasons.push("供应商必填");
            if (isNaN(Number(r.amount))) reasons.push("合同金额格式错误");
            if (!r.date_sign) reasons.push("签订时间必填");
            if (reasons.length > 0) {
                failRows.push({row: i + 2, id: id || "-", title: r.title || "", reason: reasons.join("；")});
                return;
            }
            db.contracts.push(normalizeContract(r));
            okCnt++;
        });
        saveDB(db);
        return ok({success: okCnt, fail: failRows.length, failRows: failRows});
    }

    /* ---------------- 账号与权限 ---------------- */
    /**
     * 账号列表：账号本身只存 role_code，返回时关联 roles 表补全 role 对象
     */
    static getAccountList() {
        const db = loadDB();
        return ok(db.accounts.map(({password, ...rest}) => {
            const role = db.roles.find(r => r.role_code === rest.role_code);
            return {...rest, role: role || {role_code: rest.role_code, role_name: "未知", perms: []}};
        }));
    }

    /**
     * 保存账号：只接收账号字段（含 role_code），不接收 role 对象本身
     */
    static saveAccount(paras) {
        const db = loadDB();
        // 只保留账号字段，避免把关联查出的 role 对象写回账号
        const {role, ...accountData} = paras;
        const idx = db.accounts.findIndex(a => a.account === accountData.account);
        if (idx >= 0) {
            db.accounts[idx] = {
                ...db.accounts[idx],
                ...accountData,
                password: accountData.password || db.accounts[idx].password,
            };
        } else {
            db.accounts.push({
                ...accountData,
                password: accountData.password || "123456",
                status: accountData.status ?? 1,
            });
        }
        saveDB(db);
        return ok(true);
    }

    /**
     * 角色列表：供账号表单的角色下拉与权限预览使用
     */
    static getRoleList() {
        const db = loadDB();
        return ok(db.roles.map(r => ({...r, perms: [...r.perms]})));
    }

    /**
     * 权限码字典：供账号表单的权限勾选展示使用
     */
    static getPermDefs() {
        return ok(PERM_DEFS.map(p => ({...p})));
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

    /* ---------------- 签订人字典 ---------------- */
    /**
     * 签订人列表：供合同表单/筛选的下拉使用
     */
    static getSignerList() {
        const db = loadDB();
        return ok(db.signers.map(s => ({...s})));
    }
}
