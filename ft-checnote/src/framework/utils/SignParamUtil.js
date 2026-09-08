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
