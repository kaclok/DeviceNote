import {MathUtil} from "@/framework/utils/MathUtil.js";

export class UnorderList {
    constructor() {
        this.list = [];
    }

    add(elem) {
        let index = this.list.indexOf(elem);
        if (index === -1) {
            this.list.push(elem)
            return true;
        }
        return false;
    }

    remove(ele) {
        let index = this.list.indexOf(ele);
        if (index > -1) {
            MathUtil.Swap(this.list, index, this.length - 1);
            this.list.pop();
            return true;
        }
        return false;
    }

    clear() {
        this.list = [];
    }

    get(index) {
        return this.list[index];
    }

    length() {
        return this.list.length;
    }
}
