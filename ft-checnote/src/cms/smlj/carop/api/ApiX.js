import {axiosInst as axiosR} from "@/framework/services/net/AxiosInst.js"

/**
 * 车道闸控制 - 后端接口层
 *
 * 后端: CCarOp -> POST /carOp/openDoor?laneId=xx&status=xx
 * status: 0开启道闸 1常开锁定 2解锁恢复
 * 该接口不在 AccessInterceptor 拦截范围(/cghtz/** 之外),无需登录态。
 *
 * 注意: 后端参数为 @RequestParam(query 参数),所以第二参传 null,
 * 第三参用 {params, signal},与 cghtz 中列表接口写法一致。
 */
export class ApiX {
    static openDoor(paras, signal) {
        return axiosR.post("carOp/openDoor", null, {
            params: paras, signal: signal,
        })
    }
}
