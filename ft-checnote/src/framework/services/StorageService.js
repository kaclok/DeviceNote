export class StorageService {
    constructor(type) {
        this.type = type;
    }

    setStore(name, content) {
        if (!name) {
            return false;
        }

        if (typeof (content) !== 'string') {
            content = JSON.stringify(content);
        }

        if (type === 'LocalStorage') {
            window.localStorage.setItem(name, content);
        } else if (type === 'SessionStorage') {
            window.sessionStorage.setItem(name, content);
        }

        return true;
    }

    getStore(name) {
        if (!name) {
            return null;
        }
        let v = null;
        if (type === 'LocalStorage') {
            v = window.localStorage.getItem(name);
        } else if (type === 'SessionStorage') {
            v = window.sessionStorage.getItem(name);
        }
        return v;
    }

    removeStore(name) {
        if (!name) {
            return false;
        }
        if (type === 'LocalStorage') {
            window.localStorage.removeItem(name);
        } else if (type === 'SessionStorage') {
            window.sessionStorage.removeItem(name);
        }
        return true;
    }

    clearStore() {
        if (type === 'LocalStorage') {
            window.localStorage.clear();
        } else if (type === 'SessionStorage') {
            window.sessionStorage.clear();
        }
    }
}

const localStoreInstance = new StorageService("LocalStorage");
const sessionStoreInstance = new StorageService("SessionStorage");

export {
    localStoreInstance,
    sessionStoreInstance,
}
