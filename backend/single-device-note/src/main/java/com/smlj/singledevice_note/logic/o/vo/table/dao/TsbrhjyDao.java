package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeviceRecord;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tsbrhjy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.util.ArrayList;

@Mapper
@Repository
public interface TsbrhjyDao {
    ArrayList<Tsbrhjy> doSelect(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") ArrayList<String> conds, @Param("orderBys") String orderBys);

    ArrayList<Tsbrhjy> doSelectSimple(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") String conds, @Param("orderBys") String orderBys);

    int updateTime(@Param("tableName") String tableName, @Param("id") int id, @Param("a_time") Date a_time, @Param("b_time") Date b_time);

    int update(@Param("item") Tsbrhjy item);
}
