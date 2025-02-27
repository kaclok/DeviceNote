function onceAsync(cb) {
    const map = {}
    return (...args) => {
        return new Promise((resolve, reject) => {
            const key = JSON.stringify(args);
            if (!map[key]) {
                map[key] = {
                    resolve: [],
                    reject: [],
                    isPending: false
                }
            }

            const kv = map[key]
            kv.resolve.push(resolve);
            kv.reject.push(reject);
            if (kv.isPending) {
                return;
            }

            cb(...args).then(
                (res) => {
                    kv.resolve.forEach((resolve) => {
                        resolve(res);
                    });
                }
            ).catch((res) => {
                kv.reject.forEach((reject) => {
                    reject(res);
                });
            }).finally(() => {
                map[key] = null;
            });
        })
    }
}

export {
    onceAsync,
}