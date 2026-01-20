package com.smlj.singledevice_note.core.utils;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/train/page")
public class PageUtil {
    public static <T, R> PageInfo<R> convert(PageInfo<T> source, Function<T, R> converter) {
        PageInfo<R> result = new PageInfo<>();

        // 复制分页信息
        BeanUtils.copyProperties(source, result);

        // 转换数据列表
        List<R> convertedList = source.getList().stream()
                .map(converter)
                .collect(Collectors.toList());

        result.setList(convertedList);
        return result;
    }

    public static <T, R> PageInfo<R> doPage(int pageNum, int pageSize, Function<?, ArrayList<T>> getSqlList, Function<T, R> converter) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        PageInfo<T> pi = new PageInfo<>(getSqlList.apply(null));
        return convert(pi, converter);
    }

    public static <T, R> int calcTotal(Function<?, ArrayList<T>> getSqlList, Function<T, R> converter) {
        var all = getSqlList.apply(null);
        return (int) all.stream()
                .map(converter)
                .filter(Objects::nonNull)
                .count();
    }

    public static <T, R> int calcSize(int pageNum, int pageSize, int total) {
        int basicSize = pageNum * pageSize;

        // 添加缓冲（假设过滤掉30%的数据）
        double bufferFactor = 1.5; // 缓冲系数，根据实际情况调整
        int bufferedSize = (int) (basicSize * bufferFactor);

        // 但不要超过总数
        return Math.min(bufferedSize, total);
    }
}
