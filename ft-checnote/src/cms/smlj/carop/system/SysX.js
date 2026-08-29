import {ApiX} from "../api/ApiX.js";

/**
 * 车道闸控制 - 业务层
 * 所有方法遵循项目统一的 (paras, signal, onBefore, onAfter) 回调签名。
 */
class SysX {
    /**
     * 道闸操作
     * @param paras {laneId, status} laneId=车道id, status=0开启道闸 1常开锁定 2解锁恢复
     */
    async openDoor(paras, signal, onBefore, onAfter) {
        onBefore?.();
        ApiX.openDoor(paras, signal).then(succ => {
            onAfter?.(true, succ.data);
        }).catch(fail => {
            onAfter?.(false, fail);
        });
    }
}

export {
    SysX,
}
