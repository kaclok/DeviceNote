package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsPromise;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsPromiseDao {

    TCbsPromise queryByDate(@Param("date") java.time.LocalDate date);

    List<TCbsPromise> queryPage(@Param("date") String date,
                                @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("date") String date);

    int insert(@Param("p") TCbsPromise p);

    int update(@Param("p") TCbsPromise p);
}
