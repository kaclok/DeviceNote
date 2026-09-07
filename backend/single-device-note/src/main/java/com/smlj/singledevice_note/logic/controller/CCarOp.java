package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.entity.ReceiptInfo;
import com.smlj.singledevice_note.logic.o.vo.table.service.CarOpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController // 注意这里因为返回的事html, 所以不能是RestController
@RequestMapping("/carOp")
// file:///C:/Users/DELL/Documents/sxigc_data/Profiles/B83DB8DA2DCE085A2476BB9833AF7B56/Cache/chat/file/202510/%E6%99%BA%E6%85%A7%E5%81%9C%E8%BD%A6%E7%B3%BB%E7%BB%9F%E5%9C%BA%E7%AB%AF%E5%B9%B3%E5%8F%B0API%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3V2_1.html
public class CCarOp {
    private final CarOpService carOpService;

    // 通过F12查看Lanes?协议，查看VehicleLaneID即为车道id
    // status==1 表示开启道闸，并且关闭雷达识别，也就是车辆经过雷达之后不会导致道闸下来
    // status==2 表示开启雷达的控制，也就是车辆经过雷达之后会导致道闸下来
    // 0开启道闸 1常开锁定  2解锁恢复
    @PostMapping("/openDoor")
    public Result<?> openDoor(String laneId, int status) throws Exception {
        return Result.success(carOpService.openDoor(laneId, status));
    }

    @GetMapping("/testxx")
    public Result<?> testxx(Date dt) {
        return Result.success(dt);
    }

    @Scheduled(fixedRate = 9000)
    @Transactional
    public void scan130() {
        // 扫描 biz_receipt_send_car_head
        try {
            // 查询符合条件的数据：receipt_status为130或144，且remark为0
            var receiptList = carOpService.select130ByStatusAndRemark();
            log.info("130 本次扫描到符合条件的数据{}条", receiptList.size());

            // 处理每条记录
            for (var receipt : receiptList) {
                processReceipt(receipt, 130);
            }
        } catch (Exception e) {
            log.error("扫描处理单据时发生异常", e);
        }
    }

    @Scheduled(fixedRate = 9000)
    @Transactional
    public void scan144() {
        log.info("scan 144, 表格biz_receipt_send_car_head_mj");

        // 扫描 biz_receipt_send_car_head_mj
        try {
            // 查询符合条件的数据：receipt_status为130或144，且remark为0
            var receiptList = carOpService.select144ByStatusAndRemark();
            log.info("144 本次扫描到符合条件的数据{}条", receiptList.size());

            // 处理每条记录
            for (var receipt : receiptList) {
                processReceipt(receipt, 144);
            }
        } catch (Exception e) {
            log.error("扫描处理单据时发生异常", e);
        }
    }

    @Transactional
    public void processReceipt(ReceiptInfo receipt, int status) throws Exception {
        log.info("开始处理单据:{}，车牌号：{} 状态:{}", receipt.getId(), receipt.getCar_number(), status);

        // 130 调门禁接口将其删除， 同时remark设置为1，防止重复扫描
        // 144 调门禁接口， 将其添加到门禁系统中（会返回一个类似door_car_id） 并更新 car_head的door_car_id字段
        // 根据不同状态处理
        if (130 == status) {
            boolean success = carOpService.removeCar(receipt, true);
            log.info("单据:{} 车牌号：{} 调用removeCar接口,处理结果:{}", receipt.getId(), receipt.getCar_number(), success);
        } else if (144 == status) {
            // 调用添加车辆接口，获取返回的id
            Long dorCarId = carOpService.addCar(receipt, true);
            // 更新当前表的remark为1
            // 更新关联表的door_car_id
            log.info("单据:{} 车牌号：{} 调用addCar接口,处理结果dorCarId:{}", receipt.getId(), receipt.getCar_number(), dorCarId);
        }
    }
}
