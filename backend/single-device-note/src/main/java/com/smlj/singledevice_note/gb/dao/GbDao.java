package com.smlj.singledevice_note.gb.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.smlj.singledevice_note.gb.o.GbLogLine;
import com.smlj.singledevice_note.gb.o.GbTimeline;
import com.smlj.singledevice_note.gb.o.GbWeighEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 过磅日志落库（gb_record 库，独立 PostgreSQL 数据库）。
 * ⚠️ 类上 @DS("gb") 切独立数据源：本 DAO 的所有方法都不得包在 @Transactional 事务里
 * （事务开启时 dynamic-datasource 已把连接绑到主库，事务内切库不生效 —— 与 CCGHT 同坑）。
 */
@DS("gb")
@Mapper
@Repository
public interface GbDao {
    /** 批量插入原始行，(source, fp) 冲突跳过；返回实际插入行数 */
    int insertLines(@Param("list") List<GbLogLine> list);

    /** 批量插入过磅事件，(source, order_no, event_time) 冲突跳过；返回实际插入行数 */
    int insertEvents(@Param("list") List<GbWeighEvent> list);

    List<GbLogLine> selectLines(@Param("source") String source,
                                @Param("beginTime") LocalDateTime beginTime,
                                @Param("endTime") LocalDateTime endTime,
                                @Param("keyword") String keyword,
                                @Param("offset") int offset,
                                @Param("size") int size);

    long countLines(@Param("source") String source,
                    @Param("beginTime") LocalDateTime beginTime,
                    @Param("endTime") LocalDateTime endTime,
                    @Param("keyword") String keyword);

    List<GbWeighEvent> selectEvents(@Param("source") String source,
                                    @Param("orderNo") String orderNo,
                                    @Param("vehicleNo") String vehicleNo,
                                    @Param("beginTime") LocalDateTime beginTime,
                                    @Param("endTime") LocalDateTime endTime,
                                    @Param("offset") int offset,
                                    @Param("size") int size);

    long countEvents(@Param("source") String source,
                     @Param("orderNo") String orderNo,
                     @Param("vehicleNo") String vehicleNo,
                     @Param("beginTime") LocalDateTime beginTime,
                     @Param("endTime") LocalDateTime endTime);

    /** 单号聚合（进/出磅时间） */
    List<GbTimeline> selectTimeline(@Param("orderNo") String orderNo);
}
