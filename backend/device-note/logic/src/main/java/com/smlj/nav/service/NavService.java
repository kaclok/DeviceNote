package com.smlj.nav.service;

import com.smlj.nav.dao.NavDao;
import com.smlj.nav.o.NavConfig;
import com.smlj.nav.o.NavGate;
import com.smlj.nav.o.NavMaterial;
import com.smlj.nav.o.NavNode;
import com.smlj.nav.o.NavPage;
import com.smlj.nav.o.NavRoad;
import com.smlj.nav.o.NavScale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NavService {
    private final NavDao navDao;

    public NavConfig getConfig(String plantId) {
        if (plantId == null || plantId.isBlank()) return null;
        var plant = navDao.selectPlant(plantId);
        if (plant == null) return null;

        NavConfig config = new NavConfig();
        config.setPlant(plant);
        config.setNodes(navDao.selectNodes(plantId));
        config.setRoads(navDao.selectRoads(plantId));
        config.setScales(navDao.selectScales(plantId));
        config.setGates(navDao.selectGates(plantId));
        config.setMaterials(navDao.selectMaterials(plantId));
        config.setPages(navDao.selectPages(plantId));
        return config;
    }

    /**
     * 整体覆盖保存：先按外键顺序删除该厂区旧数据，再重新插入。
     * 前端每次保存提交完整配置，逻辑简单且不会留下脏数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(NavConfig config) {
        if (config == null || config.getPlant() == null
                || config.getPlant().getId() == null || config.getPlant().getId().isBlank()) {
            throw new IllegalArgumentException("厂区配置不能为空");
        }

        String plantId = config.getPlant().getId();
        List<NavNode> nodes = safe(config.getNodes());
        List<NavRoad> roads = safe(config.getRoads());
        List<NavScale> scales = safe(config.getScales());
        List<NavGate> gates = safe(config.getGates());
        List<NavMaterial> materials = safe(config.getMaterials());
        List<NavPage> pages = safe(config.getPages());

        validateNoDuplicate(nodes, roads);

        navDao.deleteMaterials(plantId);
        navDao.deleteScales(plantId);
        navDao.deleteGates(plantId);
        navDao.deleteRoads(plantId);
        navDao.deleteNodes(plantId);
        navDao.upsertPlant(config.getPlant());

        if (!nodes.isEmpty()) {
            nodes.forEach(item -> {
                item.setPlantId(plantId);
                if (item.getVisible() == null) item.setVisible(Boolean.TRUE);
            });
            navDao.insertNodes(nodes);
        }
        if (!roads.isEmpty()) {
            roads.forEach(item -> item.setPlantId(plantId));
            navDao.insertRoads(roads);
        }
        if (!scales.isEmpty()) {
            scales.forEach(item -> item.setPlantId(plantId));
            navDao.insertScales(scales);
        }
        if (!gates.isEmpty()) {
            gates.forEach(item -> {
                item.setPlantId(plantId);
                if (item.getGateMode() == null || item.getGateMode().isBlank()) item.setGateMode("both");
            });
            navDao.insertGates(gates);
        }
        if (!materials.isEmpty()) {
            materials.forEach(item -> item.setPlantId(plantId));
            navDao.insertMaterials(materials);
        }
        // 切页显示方向：老客户端不带 pages 字段时保持库里原值，避免被空列表清空
        if (!pages.isEmpty()) {
            pages.forEach(item -> {
                if (item.getPageCode() == null || item.getPageCode().isBlank()) {
                    throw new IllegalArgumentException("切页标识不能为空");
                }
                item.setPlantId(plantId);
                if (item.getOrientation() == null || item.getOrientation() < 0 || item.getOrientation() > 2) {
                    item.setOrientation(0);
                }
            });
            navDao.deletePages(plantId);
            navDao.insertPages(pages);
        }
    }

    /**
     * 保存前拦截重复数据：点位名称/编号唯一；道路名称唯一，且同一对点位之间只允许一条道路。
     * 名称与编号比较均忽略大小写与首尾空格。
     */
    private void validateNoDuplicate(List<NavNode> nodes, List<NavRoad> roads) {
        Set<String> nodeNames = new HashSet<>();
        Set<String> nodeCodes = new HashSet<>();
        for (NavNode node : nodes) {
            String name = node.getName() == null ? "" : node.getName().trim();
            if (!name.isEmpty() && !nodeNames.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("点位名称重复：「" + name + "」");
            }
            String code = node.getCode() == null ? "" : node.getCode().trim();
            if (!code.isEmpty() && !nodeCodes.add(code.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("点位编号重复：「" + code + "」");
            }
        }

        Set<String> roadNames = new HashSet<>();
        Set<String> roadPairs = new HashSet<>();
        for (NavRoad road : roads) {
            String name = road.getName() == null ? "" : road.getName().trim();
            if (!name.isEmpty() && !roadNames.add(name.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("道路名称重复：「" + name + "」");
            }
            String from = road.getFromNodeId() == null ? "" : road.getFromNodeId();
            String to = road.getToNodeId() == null ? "" : road.getToNodeId();
            if (!from.isEmpty() && !to.isEmpty()) {
                String key = from.compareTo(to) <= 0 ? from + "|" + to : to + "|" + from;
                if (!roadPairs.add(key)) {
                    throw new IllegalArgumentException("道路重复：同一对点位之间已存在道路（" + from + " → " + to + "）");
                }
            }
        }
    }

    private <T> List<T> safe(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }
}
