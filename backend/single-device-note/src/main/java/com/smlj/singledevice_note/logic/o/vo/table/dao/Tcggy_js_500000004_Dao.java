package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_js_500000004;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Mapper
@Repository
public interface Tcggy_js_500000004_Dao {
    ArrayList<Tcggy_js_500000004> doSelect(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") ArrayList<String> conds, @Param("orderBys") String orderBys);

    ArrayList<Tcggy_js_500000004> doSelectSimple(@Param("tableName") String tableName, @Param("select") String select, @Param("conds") String conds, @Param("orderBys") String orderBys);

    void Clear(@Param("tableName") String tableName);

    void InsertBatch(@Param("tableName") String tableName, List<Tcggy_js_500000004> list);
}
