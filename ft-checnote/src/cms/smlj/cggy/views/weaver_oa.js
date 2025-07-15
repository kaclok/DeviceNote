// 嵌入的js脚本不能存在return语句，用if代替
// 居然没有cos的问题

function getHashParam(url, paramName) {
    // 找到hash中的问号位置
    const hashStart = url.indexOf('#');
    if (hashStart === -1) return null;

    const hash = url.substring(hashStart);
    const queryStart = hash.indexOf('?');
    if (queryStart === -1) return null;

    const queryString = hash.substring(queryStart + 1);
    const params = new URLSearchParams(queryString);
    return params.get(paramName);
}

var goods_id = getHashParam(window.location.href, 'goods_id');
var upload_ts = getHashParam(window.location.href, 'upload_ts');

console.log(goods_id + '|' + upload_ts)

// 手动补零函数
const pad = num => num.toString().padStart(2, '0');

function addRow(item) {
    WfForm.addDetailRow("detail_1",
        {
            field71763: {value: item.goods_supplier},
            field71764: {value: item.wlcc_id},
            field71765: {value: item.car_no},
            field71766: {value: item.final_weight},
            field71767: {value: item.ash_weight},
            field71768: {value: item.weight},
            field71769: {value: item.fq},
            field71770: {value: item.base_price},
            field71771: {value: item.price},
            field71772: {value: item.total_price},
            field71774: {value: item.dt}
        }
    );
}

function addRows(arr) {
    //删除明细1所有行
    WfForm.delDetailRow("detail_1", "all");

    for (let i = 0; i < arr.length; ++i) {
        let item = arr[i];
        item.dt = new Date(item.dt).toLocaleString('zh-CN', {
            timeZone: 'Asia/Shanghai', // 上海时区 = 北京时区
            hour12: false // 24小时制
        });

        addRow(item)
    }
}

/*let testArr = [
    {
        "dt": 1749087292000,
        "wlcc_id": "SH01071653",
        "goods_supplier": "兰州永诚化工有限公司",
        "car_no": "宁B91761",
        "final_weight": 0,
        "ash_weight": 0,
        "weight": 32.76,
        "fq": 281,
        "base_price": 0,
        "price": 0,
        "total_price": 0
    }
]

addRows(testArr)*/

if (upload_ts !== null && goods_id != null) {
    const reqUrl = "http://10.8.54.24:7090/x/cggy/search" + "?upload_ts=" + upload_ts + "&goods_id=" + goods_id;
    console.log("reqUrl:" + reqUrl)
    fetch(reqUrl, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
        },
    }).then(response => response.json())
        .then(data => {
            console.log("POST请求成功:")
            // console.table(data);
            addRows(data.data);
        })
        .catch(error => console.error("POST请求失败"));

    // WfForm.showMessage("运算错误", 2, 10);
}