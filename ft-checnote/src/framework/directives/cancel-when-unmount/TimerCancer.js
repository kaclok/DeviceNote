export class TimerCancer {
    constructor(...args) {
        this.cancers = args;
    }

    Cancel() {
        for (const one of this.cancers) {
            clearTimeout(one)
        }
    }
}

/*
const timer = setTimeout(() => {
    console.log('定时器执行');
}, 1000);

// 取消定时器
clearTimeout(timer);
*/
