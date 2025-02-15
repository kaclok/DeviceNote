package com.smlj.o.vo.table.dao;

import com.smlj.o.vo.table.entity.TGY;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TGYDao {
    ArrayList<TGY> doSelect(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") ArrayList<String> conds, @Param("orderBys") String orderBys);

    ArrayList<TGY> doSelectSimple(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") String conds, @Param("orderBys") String orderBys);
}
