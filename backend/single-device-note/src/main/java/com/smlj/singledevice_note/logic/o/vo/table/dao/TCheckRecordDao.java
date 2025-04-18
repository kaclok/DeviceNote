package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCheckRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TCheckRecordDao {
    ArrayList<TCheckRecord> doSelect(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") ArrayList<String> conds, @Param("orderBys") String orderBys);

    ArrayList<TCheckRecord> doSelectSimple(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") String conds, @Param("orderBys") String orderBys);

    void insert(@Param("tableName") String tableName, @Param("item") TCheckRecord item);

    void update(@Param("tableName") String tableName, @Param("item") TCheckRecord item);

    Integer exist(@Param("tableName") String tableName, @Param("id") Integer id);

    void delete(@Param("tableName") String tableName, @Param("id") Integer id);
}
