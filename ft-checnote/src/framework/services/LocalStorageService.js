export class LocalStorageService {
    constructor() {
    }

    static setStore(name, content) {
        if (!name) {
            return false;
        }
        if (typeof (content) !== 'string') {
            content = JSON.stringify(content);
        }
        window.localStorage.setItem(name, content);
        return true;
    }

    static getStore(name) {
        if (!name) {
            return null;
        }
        let v = window.localStorage.getItem(name);
        if (v == null) {
            return null;
        }
        return v;
    }

    static removeStore(name) {
        if (!name) {
            return false;
        }
        window.localStorage.removeItem(name);
        return true;
    }

    static clearStore() {
        window.localStorage.clear();
    }
}
