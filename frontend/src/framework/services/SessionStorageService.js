// sessionStorage顾名思义是针对一个session的数据存储，生命周期为当前窗口，一旦窗口关闭，那么存储的数据将被清空
export class SessionStorageService {
    constructor() {
    }

    static setStore(name, content) {
        if (!name) {
            return false;
        }
        if (typeof (content) !== 'string') {
            content = JSON.stringify(content);
        }
        window.sessionStorage.setItem(name, content);
        return true;
    }

    static getStore(name) {
        if (!name) {
            return null;
        }
        let v = window.sessionStorage.getItem(name);
        if (v == null) {
            return null;
        }
        return v;
    }

    static removeStore(name) {
        if (!name) {
            return false;
        }
        window.sessionStorage.removeItem(name);
        return true;
    }

    static clearStore() {
        window.sessionStorage.clear();
    }
}
