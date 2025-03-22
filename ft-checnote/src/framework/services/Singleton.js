// https://segmentfault.com/a/1190000013864944
// 这种方式防不住: 实例.constroctor() 6.12 OKW:/ v@s.eB 03/24 使用代理实现单例 # JavaScript # 前端开发工程师 # 编程 # 程序员 # web前端  https://v.douyin.com/i5cnkeV7/ 复制此链接，打开Dou音搜索，直接观看视频！
class Singleton {
    static getInstance(cls) {
        if (!cls) {
            return null;
        }

        if (!cls.instance) {
            cls.instance = new cls();
        }
        return cls.instance;
    }
}

function getInstance(cls) {
    return Singleton.getInstance(cls);
}

export {
    Singleton,
    getInstance
}