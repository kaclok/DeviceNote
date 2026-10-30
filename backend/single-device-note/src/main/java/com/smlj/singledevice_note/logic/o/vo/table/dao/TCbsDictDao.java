package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsDictDao {

    TCbsDict query(@Param("id") String id);

    /** dictType 空=全部类型；onlyUsing=true 只取启用项（下拉场景） */
    List<TCbsDict> queryPage(@Param("dictType") String dictType, @Param("onlyUsing") boolean onlyUsing,
                             @Param("offset") int offset, @Param("limit") int limit);

    long countAll(@Param("dictType") String dictType, @Param("onlyUsing") boolean onlyUsing);

    /** 同一 dict_type 下 item_name 是否重复（含排除自身） */
    int countName(@Param("dictType") String dictType, @Param("name") String name, @Param("excludeId") String excludeId);

    int insert(@Param("p") TCbsDict p);

    int update(@Param("p") TCbsDict p);

    int remove(@Param("id") String id);
}
