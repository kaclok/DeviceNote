package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.service.CarOpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

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
}
