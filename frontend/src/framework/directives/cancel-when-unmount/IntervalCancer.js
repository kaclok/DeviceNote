export class IntervalCancer {
    constructor(...args) {
        this.cancers = args;
    }

    Cancel() {
        for (const one of this.cancers) {
            clearInterval(one);
        }
    }
}

/*
const interval = setInterval(() => {
  console.log('定时器执行');
}, 1000);

// 取消定时器
clearInterval(interval);
*/
