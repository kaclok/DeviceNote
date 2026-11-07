package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.logic.o.vo.table.entity.TCbsPerson;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TCbsPersonDao {

    /** 某承包商的人员，join 关联表带出 rel_id / audit_status / on_site */
    List<TCbsPerson> queryByContractor(@Param("contractorId") String contractorId,
                                       @Param("name") String name,
                                       @Param("postName") String postName,
                                       @Param("offset") int offset,
                                       @Param("limit") int limit);

    long countByContractor(@Param("contractorId") String contractorId,
                           @Param("name") String name,
                           @Param("postName") String postName);

    /** 岗位下拉：人员库里已录入过的岗位（去重） */
    List<String> queryPosts();

    TCbsPerson query(@Param("id") String id);

    /** 按身份证取人员：新增人员时复用已建档的人，避免重复建卡 */
    TCbsPerson queryByIdCard(@Param("idCard") String idCard);

    /** 身份证查重（启用行）；编辑态排除自身 */
    int countIdCard(@Param("idCard") String idCard, @Param("excludeId") String excludeId);

    int insert(@Param("p") TCbsPerson p);

    int update(@Param("p") TCbsPerson p);

    int remove(@Param("id") String id);
}
