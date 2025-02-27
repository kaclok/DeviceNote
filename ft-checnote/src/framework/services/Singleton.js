// https://segmentfault.com/a/1190000013864944
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