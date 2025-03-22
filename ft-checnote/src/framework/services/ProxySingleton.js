// 6.12 OKW:/ v@s.eB 03/24 使用代理实现单例 # JavaScript # 前端开发工程师 # 编程 # 程序员 # web前端  https://v.douyin.com/i5cnkeV7/ 复制此链接，打开Dou音搜索，直接观看视频！

function proxySingleton(clsName) {
    let ins = null;
    const proxy = new Proxy(clsName, {
        construct: (target, args) => {
            if (!ins) {
                ins = Reflect.construct(target, args);
            }
            return ins;
        }
    })

    // 防止通过实例.constructor构建
    proxy.prototype.constructor = proxy;
    return proxy;
}

export {
    proxySingleton
}

/*
class MyClass {
}

const my1 = proxySingleton(MyClass)
const my2 = proxySingleton(MyClass)
console.log(my1 === my2)
*/
