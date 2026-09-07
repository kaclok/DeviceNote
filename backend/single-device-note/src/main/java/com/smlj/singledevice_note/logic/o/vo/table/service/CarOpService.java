package com.smlj.singledevice_note.logic.o.vo.table.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.smlj.singledevice_note.core.utils.AesUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.ReceiptMapper;
import com.smlj.singledevice_note.logic.o.vo.table.entity.ReceiptInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CarOpService {
    private static final String CAR_OP_API_URL = "http://10.8.209.213:80/vms/BookingCar";

    private static final String LANE_OP_API_URL = "http://10.8.209.213:80/vms/LaneControl";

    private static final List<Integer> laneAuthnew = new ArrayList<>(Arrays.asList(5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16));

    private final ReceiptMapper receiptMapper;

    @Data
    @NoArgsConstructor
    public class Req {
        private String cipher;
        private String version;
        private long timestamp;
    }

    @Data
    @NoArgsConstructor
    public class Resp {
        private String msg;
        private String cipher;
        private int code;
    }

    @Data
    @Accessors(chain = true)  // 开启链式编程
    @NoArgsConstructor
    public static class LaneOp {
        private String ID;
        private int Status;
    }

    @Data
    @Accessors(chain = true)  // 开启链式编程
    @NoArgsConstructor
    public static class CarAddItem {
        private String PlateNo;
        private int ReqSeq;
        private long StartTime;
        private long EndTime;
        private String OwnerName;
        private int PlateType;
        private int PlateColor;
        private int VehicleType;
        private int VehicleColor;
        private String Phone;
        private String Rmark;
        private List<Integer> LaneAuth;
    }

    @Data
    @Accessors(chain = true)  // 开启链式编程
    @NoArgsConstructor
    public static class CarAdd {
        private int Num;
        private List<CarAddItem> BookingList;
        private String BookingName;
        private String ParkingLotsName;
    }

    @Data
    @Accessors(chain = true)  // 开启链式编程
    @NoArgsConstructor
    public static class CarDeleteItem {
        private int ID;

        public CarDeleteItem(String dcId) {
            if (!StrUtil.isEmpty(dcId)) {
                this.ID = Integer.parseInt(dcId);
            }
        }
    }

    @Data
    @Accessors(chain = true)  // 开启链式编程
    @NoArgsConstructor
    public static class CarDelete {
        private int Num;
        private List<CarDeleteItem> IDList;
    }

    @Data
    @NoArgsConstructor
    public static class DecryptRespItem {
        private Long ID;
        private Long ReqSeq;
        private Integer ResultCode;
    }

    @Data
    @NoArgsConstructor
    public static class DecryptResp {
        private Integer Num;
        private ArrayList<DecryptRespItem> ResultInfoList;
    }

    private String payload(String encrypted) {
        Req py = new Req();
        py.setCipher(encrypted);
        py.setVersion("V1.0");
        py.setTimestamp(System.currentTimeMillis());

        return JSONUtil.toJsonStr(py);
    }

    public boolean openDoor(String laneId, int status) throws Exception {
        LaneOp p = new LaneOp();
        p.setID(laneId);
        p.setStatus(status);

        String json = JSONUtil.toJsonStr(p);
        String encrypted = AesUtil.encryptAES(json);
        String payload = payload(encrypted);

        String response = NetSender.send(LANE_OP_API_URL, payload, "PUT");
        Resp r = JSONUtil.toBean(response, Resp.class);

        log.info("response：{}", r.getMsg());
        if (r == null || r.getCode() != 200) {
            return false;
        }

        return true;
    }

    public boolean _removeCar(ReceiptInfo receipt) throws Exception {
        CarDelete cd = new CarDelete();
        cd.setNum(1);
        cd.setIDList(new ArrayList<>(Arrays.asList(new CarDeleteItem(receipt.getDoor_car_id()))));

        String json = JSONUtil.toJsonStr(cd);
        String encrypted = AesUtil.encryptAES(json);
        String payload = payload(encrypted);

        String response = NetSender.send(CAR_OP_API_URL, payload, "DELETE");
        Resp r = JSONUtil.toBean(response, Resp.class);

        log.info("删除的response：{}", r.getMsg());
        if (r == null || r.getCode() != 200) {
            return false;
        }

        var decrypted = AesUtil.decryptAES(r.getCipher());
        DecryptResp rr = JSONUtil.toBean(decrypted, DecryptResp.class);
        return rr != null && rr.getNum() > 0 && rr.getResultInfoList().get(0).getResultCode() == 200;
    }

    // 130 调门禁接口将其删除， 同时remark设置为1，防止重复扫描
    public boolean removeCar(ReceiptInfo receipt, boolean updateRemark) throws Exception {
        boolean success = _removeCar(receipt);
        if (!success) {
            return false;
        }

        if (updateRemark) {
            int cnt = receiptMapper.updateRemark(receipt.getId(), 1);
        }
        return true;
    }

    public long _addCar(String carNumber, int durationSeconds, String name) throws Exception {
        CarAdd p = new CarAdd();
        p.setNum(1);
        p.setBookingName("提货车辆");
        // p.setParkingLotsName("1234");

        long cur = System.currentTimeMillis() / 1000;
        p.setBookingList(new ArrayList<>(Arrays.asList(new CarAddItem().setPlateNo(carNumber).setStartTime(cur).setEndTime(cur + durationSeconds).setOwnerName(name).setPhone(carNumber).setLaneAuth(laneAuthnew)
                /*
                .setReqSeq(12)
                .setPlateType(1)
                .setPlateColor(1)
                .setVehicleType(1)
                .setVehicleColor(1)
                */)));

        String json = JSONUtil.toJsonStr(p);
        String encrypted = AesUtil.encryptAES(json);
        String payload = payload(encrypted);

        String response = NetSender.send(CAR_OP_API_URL, payload, "POST");
        Resp r = JSONUtil.toBean(response, Resp.class);

        log.info("添加的response：{}", r.getMsg());
        if (r == null || r.getCode() != 200) {
            return -1;
        }

        var decrypted = AesUtil.decryptAES(r.getCipher());
        DecryptResp rr = JSONUtil.toBean(decrypted, DecryptResp.class);
        return rr.getResultInfoList().get(0).getID();
    }

    public long _addCar(ReceiptInfo receipt) throws Exception {
        CarAdd p = new CarAdd();
        p.setNum(1);
        p.setBookingName("提货车辆");
        // p.setParkingLotsName("1234");

        long cur = System.currentTimeMillis() / 1000;
        p.setBookingList(new ArrayList<>(Arrays.asList(new CarAddItem().setPlateNo(receipt.getCar_number()).setStartTime(cur).setEndTime(cur + 2 * 3600).setOwnerName(receipt.getName()).setPhone(receipt.getCar_number()).setLaneAuth(laneAuthnew)
                /*
                .setReqSeq(12)
                .setPlateType(1)
                .setPlateColor(1)
                .setVehicleType(1)
                .setVehicleColor(1)
                */)));

        String json = JSONUtil.toJsonStr(p);
        String encrypted = AesUtil.encryptAES(json);
        String payload = payload(encrypted);

        String response = NetSender.send(CAR_OP_API_URL, payload, "POST");
        Resp r = JSONUtil.toBean(response, Resp.class);

        log.info("添加的response：{}", r.getMsg());
        if (r == null || r.getCode() != 200) {
            return -1;
        }

        var decrypted = AesUtil.decryptAES(r.getCipher());
        DecryptResp rr = JSONUtil.toBean(decrypted, DecryptResp.class);
        return rr.getResultInfoList().get(0).getID();
    }

    public Long addCar(ReceiptInfo receipt, boolean updateRemark) throws Exception {
        long dorCarId = _addCar(receipt);
        if (dorCarId != -1) {
            int cnt = receiptMapper.updateDoorCarId(receipt.getId(), String.valueOf(dorCarId));
        }
        if (updateRemark) {
            receiptMapper.updateRemark(receipt.getId(), 1);
        }

        return dorCarId;
    }

    public List<ReceiptInfo> select130ByStatusAndRemark() {
        return receiptMapper.selectStatus130ByStatusAndRemark();
    }

    public List<ReceiptInfo> select144ByStatusAndRemark() {
        return receiptMapper.selectStatus144ByStatusAndRemark();
    }

    public ReceiptInfo selectOneByStatusAndRemark(long id) {
        return receiptMapper.selectOneByStatusAndRemark(id);
    }
}
