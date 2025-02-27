import {DateTimeUtil} from "@/framework/utils/DateTimeUtil.js";

export class TimeService{
    constructor() {
        this.syncSvrTime = 0;
        this.syncCliTime = 0;
    }

    static initTime(svrTime) {
        this.syncSvrTime = svrTime;
        this.syncCliTime = DateTimeUtil.nowTimestamp();
    }

    static getSvrTime() {
        let nowCliTime = DateTimeUtil.nowTimestamp();
        let gap = nowCliTime - this.syncCliTime;
        return this.syncSvrTime + gap;
    }
}
