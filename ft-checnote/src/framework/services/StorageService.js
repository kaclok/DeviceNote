const StorageType = Object.freeze({
    Local: 0,
    Session: 1,
})

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

        if (this.type === StorageType.Local) {
            window.localStorage.setItem(name, content);
        } else if (this.type === StorageType.Session) {
            window.sessionStorage.setItem(name, content);
        }

        return true;
    }

    getStore(name) {
        if (!name) {
            return null;
        }

        let v = null;
        if (this.type === StorageType.Local) {
            v = window.localStorage.getItem(name);
        } else if (this.type === StorageType.Session) {
            v = window.sessionStorage.getItem(name);
        }
        return v;
    }

    removeStore(name) {
        if (!name) {
            return false;
        }

        if (this.type === StorageType.Local) {
            window.localStorage.removeItem(name);
        } else if (this.type === StorageType.Session) {
            window.sessionStorage.removeItem(name);
        }
        return true;
    }

    clearStore() {
        if (this.type === StorageType.Local) {
            window.localStorage.clear();
        } else if (this.type === StorageType.Session) {
            window.sessionStorage.clear();
        }
    }
}

const localStoreInstance = new StorageService(StorageType.Local);
const sessionStoreInstance = new StorageService(StorageType.Session);

export {
    StorageType,
    localStoreInstance,
    sessionStoreInstance,
}
