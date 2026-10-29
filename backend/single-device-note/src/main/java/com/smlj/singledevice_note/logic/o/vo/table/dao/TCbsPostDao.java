package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsPostDao {

    TCbsPost query(@Param("id") String id);

    /** kw 模糊 post_name/post_code */
    List<TCbsPost> queryAll(@Param("kw") String kw);

    /** post_code 在启用行内是否已被占用（含排除自身） */
    int countCode(@Param("code") String code, @Param("excludeId") String excludeId);

    int insert(@Param("p") TCbsPost p);

    int update(@Param("p") TCbsPost p);

    int remove(@Param("id") String id);
}
