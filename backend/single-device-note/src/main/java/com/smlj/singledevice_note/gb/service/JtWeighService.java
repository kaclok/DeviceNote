package com.smlj.singledevice_note.gb.service;

import com.smlj.singledevice_note.gb.dao.JtWeighDao;
import com.smlj.singledevice_note.gb.o.GbVehicleWeigh;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * jt_dev 车辆过磅记录查询。
 * 速度策略：idx_plate_time(plate_no, weighing_time) 索引下，完整车牌走精确匹配（毫秒级）；
 * 未命中再 like '%x%' 模糊兜底（全表扫 ~0.3s）。另加 60s TTL 内存缓存吸收重复查询。
 * ⚠️ 调用链不可包 @Transactional（JtWeighDao 走 wlcc 数据源，事务内切库失效）。
 */
@Service
@RequiredArgsConstructor
public class JtWeighService {
    private final JtWeighDao jtWeighDao;

    /** 缓存有效期：60s */
    private static final long TTL_MS = 60_000L;
    /** 缓存条目上限：超限整体清空（简单防膨胀，车辆查询不会太多种 key） */
    private static final int MAX_ENTRIES = 200;

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    private static final class Entry {
        final long at;
        final List<GbVehicleWeigh> rows;

        Entry(long at, List<GbVehicleWeigh> rows) {
            this.at = at;
            this.rows = rows;
        }
    }

    /** 按车牌查最近过磅记录：完整车牌走索引精确匹配，查不到再模糊兜底；60s 内同 key 直接返回缓存 */
    public List<GbVehicleWeigh> vehicleWeighs(String vehicleNo, int size) {
        String key = vehicleNo + "|" + size;
        long now = System.currentTimeMillis();
        Entry hit = cache.get(key);
        if (hit != null && now - hit.at < TTL_MS) return hit.rows;
        List<GbVehicleWeigh> rows = jtWeighDao.selectVehicleWeighs(vehicleNo, size, true);
        if (rows.isEmpty()) rows = jtWeighDao.selectVehicleWeighs(vehicleNo, size, false);
        if (cache.size() >= MAX_ENTRIES) cache.clear();
        cache.put(key, new Entry(now, rows));
        return rows;
    }
}
