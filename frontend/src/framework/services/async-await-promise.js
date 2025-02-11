/*

async函数的返回值始终是Promise对象。 async 函数无论是否有 return 语句，都会返回一个 Promise 对象。如果 async 函数内部没有 return 语句，它会返回一个以 undefined 为 resolved 值的 Promise。如果 async
函数内部有 return 语句，return 的值会被包装成一个已解决（resolved）状态的 Promise。

.then() 方法用于处理 async 函数返回的 Promise 对象。.then() 方法接收一个回调函数作为参数，当 async 函数返回的 Promise 进入 fulfilled（已解决）状态时，这个回调函数会被调用，并且该回调函数会接收到 async 函数的返回值。

如果 async 函数内部抛出错误，返回的 Promise 会进入 rejected（已拒绝）状态。可以通过 .catch() 方法来捕获并处理这个错误，.catch() 同样是基于 async 函数返回的 Promise 来工作的。

const promise = new Promise((resolve, reject) => { // resolve和reject也是回调函数
    // 异步操作代码
    if (异步操作成功) {
        resolve(value);
    } else {
        reject(error);
    }
});

resolve：这是一个函数，当异步操作成功完成时调用它。它接收一个参数，这个参数就是 Promise 对象成功时的返回值，其实就是解决值。
reject：这是一个函数，当异步操作失败时调用它。它接收一个参数，这个参数就是 Promise 对象失败的原因（通常是一个错误对象）。

await用于 阻塞 等待Promise的实际结果（可能是成功的解决值，也可能是失败的拒绝值），
失败的拒绝值会导致异常，需要try catch捕获。

async function main() {
    const myPromise = new Promise((resolve, reject) => {
        setTimeout(() => {
            if(true) {
                resolve('操作成功的值');
            } else {
                reject(new Error('操作失败的原因'));
            }
        }, 1000);
    });

    const result = await myPromise;
    console.log(result); // 输出：操作成功的值

    try {
        const result = await myPromise;
    } catch (error) {
        console.log(error.message); // 输出：操作失败的原因
    }
}

main();

async function main() {
    const myPromise = new Promise((resolve, reject) => {
        setTimeout(() => {
            if(true) {
                resolve('操作成功的值');
            } else {
                reject(new Error('操作失败的原因'));
            }
        }, 1000);
    });

    myPromise.then(value => {
        console.log(value); // 输出：操作成功的值
    })
        .catch(error => {
            console.error(error.message); // 输出：操作失败的原因
        });
}

main();

*/