package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsPostDao {

    TCbsPost query(@Param("id") String id);

    /** kw 模糊 post_name/post_code */
    /** 分页查询（kw 模糊匹配，见 XML cond 片段） */
    List<TCbsPost> queryPage(@Param("kw") String kw,
                            @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("kw") String kw);

    /** post_code 在启用行内是否已被占用（含排除自身） */
    int countCode(@Param("code") String code, @Param("excludeId") String excludeId);

    int insert(@Param("p") TCbsPost p);

    int update(@Param("p") TCbsPost p);

    int remove(@Param("id") String id);
}
