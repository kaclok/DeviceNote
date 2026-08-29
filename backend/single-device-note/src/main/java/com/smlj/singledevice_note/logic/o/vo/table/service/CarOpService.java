package com.smlj.singledevice_note.logic.o.vo.table.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.smlj.singledevice_note.core.utils.AesUtil;
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
}
