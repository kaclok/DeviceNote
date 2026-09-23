// ============ 「主动取消」的唯一判据 ============
// 翻页 / 换筛选 / 改每页条数 / 离开页面 都会 abort 掉上一次在途请求，
// axios 会以 CanceledError 拒绝这个 Promise。**这不是"请求失败"** ——
// 服务端根本没返回任何东西，所以三处都必须把它排除在外：
//   1. AxiosInst 的响应失败分支：不 console.error、不进 HTTP 码表。
//      （这就是"浏览器偶尔打出 CanceledError {...ERR_CANCELED...}"的来源 —— 请求是我们自己 cancel 的）
//   2. NwCodeMap.notifyError：静默。视图层的"查询合同列表失败"兜底文案也不该弹。
//   3. 业务层 SysX / ApiLogin：不回调失败分支 —— 过期请求不得抢在接管它的那次请求前面
//      清空列表 / 关掉 loading。
// 判据集中在此，避免三处各写一份后漂移（原先 AxiosInst 与 NwCodeMap 各写了一遍）。
// 三个字段都判：axios v1 的 CanceledError 带 code + name；旧 CancelToken 走 __CANCEL__ 标记。
export function isCanceled(fail) {
    return fail?.code === "ERR_CANCELED" || fail?.name === "CanceledError" || fail?.__CANCEL__ === true;
}
