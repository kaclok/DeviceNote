package com.smlj.nav.dao;

import com.smlj.nav.o.NavGate;
import com.smlj.nav.o.NavMaterial;
import com.smlj.nav.o.NavNode;
import com.smlj.nav.o.NavPage;
import com.smlj.nav.o.NavPlant;
import com.smlj.nav.o.NavRoad;
import com.smlj.nav.o.NavScale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface NavDao {
    NavPlant selectPlant(@Param("plant_id") String plantId);

    List<NavNode> selectNodes(@Param("plant_id") String plantId);

    List<NavRoad> selectRoads(@Param("plant_id") String plantId);

    List<NavScale> selectScales(@Param("plant_id") String plantId);

    List<NavGate> selectGates(@Param("plant_id") String plantId);

    List<NavMaterial> selectMaterials(@Param("plant_id") String plantId);

    List<NavPage> selectPages(@Param("plant_id") String plantId);

    int deleteMaterials(@Param("plant_id") String plantId);

    int deleteScales(@Param("plant_id") String plantId);

    int deleteGates(@Param("plant_id") String plantId);

    int deleteRoads(@Param("plant_id") String plantId);

    int deleteNodes(@Param("plant_id") String plantId);

    int deletePages(@Param("plant_id") String plantId);

    int upsertPlant(NavPlant plant);

    int insertNodes(@Param("list") List<NavNode> list);

    int insertRoads(@Param("list") List<NavRoad> list);

    int insertScales(@Param("list") List<NavScale> list);

    int insertGates(@Param("list") List<NavGate> list);

    int insertMaterials(@Param("list") List<NavMaterial> list);

    int insertPages(@Param("list") List<NavPage> list);
}
