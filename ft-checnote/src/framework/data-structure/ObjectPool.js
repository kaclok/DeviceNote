export class DomPool {
    pool = [];
    tag = null;

    constructor(tag) {
        this.tag = tag;
    }

    Acquire() {
        if (this.pool.length <= 0) {
            let div = document.createElement(this.tag);
            // 新建dom添加到body上
            document.body.appendChild(div);
            return div;
        } else {
            return this.pool.shift();
        }
    }

    recovery(ele) {
        return this.pool.push(...ele);
    }
}

export class ObjectPool {
    pool = [];
    cls = null;

    constructor(cls) {
        this.cls = cls;
    }

    Acquire() {
        if (this.pool.length > 0) {
            return this.pool.pop();
        } else {
            return new this.cls();
        }
    }

    recovery(ele) {
        this.pool.push(ele);
    }

    clear() {
        this.pool = [];
        this.cls = null;
    }
}