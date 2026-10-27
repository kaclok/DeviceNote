package com.smlj.singledevice_note.gb.dao;

import com.smlj.singledevice_note.gb.o.GbLogLine;
import com.smlj.singledevice_note.gb.o.GbTimeline;
import com.smlj.singledevice_note.gb.o.GbWeighEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 过磅日志落库：device-note 库 · gb_record 模式（表名在 XML 里以 gb_record. 前缀限定）。
 * 走主数据源 main，无需切库。
 */
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

    /** 保留期清理：删除 log_time 早于 before 的原始日志行，返回删除行数 */
    int deleteExpiredLines(@Param("before") LocalDateTime before);

    /** 保留期清理：删除 event_time 早于 before 的过磅事件，返回删除行数 */
    int deleteExpiredEvents(@Param("before") LocalDateTime before);
}
