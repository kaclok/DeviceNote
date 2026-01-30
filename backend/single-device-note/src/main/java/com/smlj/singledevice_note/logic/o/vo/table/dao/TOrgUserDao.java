package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TOrg;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TOrgUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Mapper
@Repository
public interface TOrgUserDao {
    TOrgUser queryById(@Param("id") String id);

    ArrayList<TOrgUser> queryByDepts(@Param("depts") ArrayList<String> depts);
}
