package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTContract;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TCGHTContractDao {
    ArrayList<TCGHTContract> queryAll(
            @Param("id") String id
            , @Param("title") String title
            , @Param("sign_person") String sign_person
            , @Param("sign_type") int sign_type
            , @Param("supplier") String supplier
            , @Param("queryBegin") String queryBegin
            , @Param("queryEnd") String queryEnd);

    TCGHTContract query(@Param("id") String id);

    int exist(@Param("id") String id);
}
