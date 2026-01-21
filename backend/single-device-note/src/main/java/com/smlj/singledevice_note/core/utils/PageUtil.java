package com.smlj.singledevice_note.core.utils;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/train/page")
public class PageUtil {
    public static <T, R> PageSerializable<R> convert(PageSerializable<T> source, Function<T, R> converter) {
        PageSerializable<R> result = new PageSerializable<>();

        // 复制分页信息
        BeanUtils.copyProperties(source, result);

        // 转换数据列表
        List<R> convertedList = source.getList().stream()
                .map(converter)
                .collect(Collectors.toList());

        result.setList(convertedList);
        return result;
    }

    public static <T, R> PageSerializable<R> doPage(int pageNum, int pageSize, Function<Void, ArrayList<T>> getSqlList, Function<T, R> converter) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        PageSerializable<T> pi = new PageSerializable<>(getSqlList.apply(null));
        return convert(pi, converter);
    }

    public static <T, R> int calcTotal(Function<Void, ArrayList<T>> getSqlList, Function<T, R> converter) {
        var all = getSqlList.apply(null);
        return (int) all.stream()
                .map(converter)
                .filter(Objects::nonNull)
                .count();
    }
}
