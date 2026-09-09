// signature.js - 签名工具
import CryptoJS from 'crypto-js';

const SECRET_KEY = 'your-secret-key'; // 与后端一致

/**
 * 生成随机数（nonce）
 */
function generateNonce() {
    // 使用时间戳 + 随机数，确保唯一性
    return Date.now() + '_' + Math.random().toString(36).substring(2, 15);
}

/**
 * 生成签名（核心算法）
 */
export function generateSign(params) {
    // 1. 过滤掉sign字段（防止死循环）
    const filtered = {};
    for (let key in params) {
        if (key !== '__sign__' && params[key] !== null && params[key] !== undefined) {
            filtered[key] = params[key];
        }
    }

    // 2. 按key排序拼接：key1=value1&key2=value2
    const sortedKeys = Object.keys(filtered).sort();
    const queryString = sortedKeys
        .map(key => `${key}=${filtered[key]}`)
        .join('&');

    // 3. 计算HMAC-SHA256签名
    return CryptoJS.HmacSHA256(queryString, SECRET_KEY).toString();
}

/**
 * 生成签名（包含时间戳和nonce）
 * @param {Object} params 业务参数
 * @returns {Object} 包含timestamp、nonce、sign的完整参数
 */
export function addSign(params) {
    // params 为 null/undefined/空对象时，不附加签名
    if (!params || Object.keys(params).length === 0) {
        return params;
    }
    // 1. 生成时间戳和nonce
    const timestamp = Date.now();
    const nonce = generateNonce();

    // 2. 合并所有参数（业务参数 + timestamp + nonce）
    const allParams = {
        ...params,
        __timestamp__: timestamp, // 放到params后面，如果有同名的会覆盖前者
        __nonce__: nonce
    };

    // 3. 计算签名
    const sign = generateSign(allParams);

    // 4. 只返回安全参数
    return {
        ...allParams,
        __sign__: sign,
    };
}

/* ---------------- 响应签名(校验后端返回的 Result.data) ---------------- */

/**
 * 数字规范文本: 统一到 JS 语义 —— Number.prototype.toString 的最短十进制(如 1→'1', 1.5→'1.5', 1e20→'1000...')
 * 与后端 ResponseSignAdvice.numberText(BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()) 对齐:
 * 整数/浮点走 toString 即得到与 Java 侧一致的十进制文本。
 * 约定范围: 数值应在 JS 安全整数(±2^53) 且非极小(≥1e-6)/极大(<1e21) 内, 超出则 toString 会走指数形式(e+21)与 Java 分叉。
 */
function numberText(n) {
    return String(n);
}

/**
 * data → 规范 JSON 文本(仅作 HMAC 输入, 不需可逆解析)
 * 规则与后端 ResponseSignAdvice.canonical 对齐:
 *   对象: 键按 UTF-16 字典序升序, 输出 {k:v,k2:v2}(键不加引号)
 *   数组: [v1,v2]; 字符串: 原样; 布尔: true/false; null: null; 数字: numberText
 * @param {*} v JSON.parse 后的 data
 * @returns {string}
 */
export function canonicalJson(v) {
    if (v === null || v === undefined) return 'null';
    const t = typeof v;
    if (t === 'string') return v;
    if (t === 'boolean') return v ? 'true' : 'false';
    if (t === 'number') return numberText(v);
    if (Array.isArray(v)) return '[' + v.map(canonicalJson).join(',') + ']';
    // 对象: 键排序(默认按 UTF-16 code unit 升序, 与 Java String.compareTo 一致)
    const keys = Object.keys(v).sort();
    return '{' + keys.map(k => k + ':' + canonicalJson(v[k])).join(',') + '}';
}

/**
 * 计算 data 的响应签名(HMAC-SHA256 hex)
 * @param {*} data 后端返回的 Result.data(已 JSON.parse)
 * @returns {string}
 */
export function signData(data) {
    return CryptoJS.HmacSHA256(canonicalJson(data), SECRET_KEY).toString();
}
