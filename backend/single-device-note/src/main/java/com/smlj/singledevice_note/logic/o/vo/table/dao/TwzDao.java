package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.Twz;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Mapper
@Repository
public interface TwzDao {
    ArrayList<Twz> doSelect(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") ArrayList<String> conds, @Param("orderBys") String orderBys);

    ArrayList<Twz> doSelectSimple(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") String conds, @Param("orderBys") String orderBys);

    void Clear(@Param("tableName") String tableName);

    void InsertBatch(@Param("tableName") String tableName, List<Twz> list);
}
