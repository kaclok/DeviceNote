package com.smlj.nav.o;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NavConfig {
    private NavPlant plant;
    private List<NavNode> nodes = new ArrayList<>();
    private List<NavRoad> roads = new ArrayList<>();
    private List<NavScale> scales = new ArrayList<>();
    private List<NavGate> gates = new ArrayList<>();
    private List<NavMaterial> materials = new ArrayList<>();
    private List<NavPage> pages = new ArrayList<>();
}
