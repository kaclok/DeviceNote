package com.smlj.logic.o.vo.table.dao;

import com.smlj.logic.o.vo.table.entity.TDevice;
import com.smlj.logic.o.vo.table.entity.TGY;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TDeviceDao {
    ArrayList<TDevice> doSelect(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") ArrayList<String> conds, @Param("orderBys") String orderBys);
}
