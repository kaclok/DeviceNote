// businessJwt.js
import jwt from 'jsonwebtoken';

export class SignJwtUtil {
    constructor(secretKey, expireSeconds = 30) {
        // 密钥从后端登录接口获取（登录时返回业务签名密钥）
        this.secretKey = secretKey;
        this.expireSeconds = expireSeconds;
    }

    /**
     * 生成业务参数JWT
     * @param {Object} params 业务参数对象
     * @returns {string} JWT字符串
     */
    generateToken(params) {
        // 添加时间戳和随机数防止重放
        const payload = {
            ...params,
            timestamp: Date.now(), // 防止重放攻击
            nonce: this.generateNonce()
        };

        // 使用HS256算法签名
        return jwt.sign(payload, this.secretKey, {
            algorithm: 'HS256',
            expiresIn: this.expireSeconds
        });
    }

    /**
     * 生成随机数
     */
    generateNonce() {
        return Math.random().toString(36).substring(2) + Date.now().toString(36);
    }
}
