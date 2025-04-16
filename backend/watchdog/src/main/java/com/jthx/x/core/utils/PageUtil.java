package com.jthx.x.core.utils;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Slf4j
@RestController
@RequestMapping("/train/page")
@Tag(name = "CPage", description = "分页测试")
public class PageUtil {
    public static <T> PageInfo<T> getPageInfo(List<T> list, int pageNum, int pageSize) {
        var p = new Page(pageNum, pageSize);
        p.setTotal(list.size());
        int startIndex = (pageNum - 1) * pageSize;
        startIndex = Math.max(startIndex, 0);
        startIndex = Math.min(startIndex, list.size());

        int endIndex = Math.min(startIndex + pageSize, list.size());
        endIndex = Math.max(endIndex, 0);
        endIndex = Math.min(endIndex, list.size());

        //从链表中截取需要显示的子链表，并加入到Page
        var subList = list.subList(startIndex, endIndex);
        p.addAll(subList);
        return new PageInfo<T>(p);
    }

    public static <T, R> PageInfo<R> doPage(int pageNum, int pageSize, Function<?, ArrayList<T>> getSqlList, Function<T, R> converter) {
        ArrayList<T> sqlList = null;
        try {
            PageHelper.startPage(pageNum, pageSize, true, true, true);
            sqlList = getSqlList.apply(null);
        } finally {
            PageHelper.clearPage();
        }

        if (sqlList != null) {
            var pi = new PageInfo<>(sqlList);
            // todo 优化：如果T和R相同，可以直接返回pi
            return PageUtil.<T, R>ofPage(pi, converter);
        }
        return null;
    }
/*
    public static <T> PageInfo ofPage(PageInfo<T> from) {
        var pi = new PageInfo<>(from.getList());
        pi.setTotal(from.getTotal());

        pi.setPageNum(from.getPageNum());
        pi.setPageSize(from.getPageSize());
        pi.setSize(from.getSize());
        pi.setStartRow(from.getStartRow());
        pi.setEndRow(from.getEndRow());
        pi.setStartRow(from.getStartRow());
        pi.setPages(from.getPages());
        pi.setPrePage(from.getPrePage());
        pi.setNextPage(from.getNextPage());
        pi.setIsFirstPage(from.isIsFirstPage());
        pi.setIsLastPage(from.isIsLastPage());
        pi.setHasPreviousPage(from.isHasPreviousPage());
        pi.setHasNextPage(from.isHasNextPage());
        pi.setNavigatePages(from.getNavigatePages());
        pi.setNavigatepageNums(from.getNavigatepageNums());
        pi.setNavigateFirstPage(from.getNavigateFirstPage());
        pi.setNavigateLastPage(from.getNavigateLastPage());

        return pi;
    }*/

    public static <T, R> PageInfo<R> ofPage(PageInfo<T> from, Function<T, R> converter) {
        var ls = new ArrayList<R>();
        var tList = from.getList();
        for (T t : tList) {
            R r = converter.apply(t);
            ls.add(r);
        }
        var pi = new PageInfo<>(ls);
        pi.setTotal(from.getTotal());

        pi.setPageNum(from.getPageNum());
        pi.setPageSize(from.getPageSize());
        pi.setSize(from.getSize());
        pi.setStartRow(from.getStartRow());
        pi.setEndRow(from.getEndRow());
        pi.setStartRow(from.getStartRow());
        pi.setPages(from.getPages());
        pi.setPrePage(from.getPrePage());
        pi.setNextPage(from.getNextPage());
        pi.setIsFirstPage(from.isIsFirstPage());
        pi.setIsLastPage(from.isIsLastPage());
        pi.setHasPreviousPage(from.isHasPreviousPage());
        pi.setHasNextPage(from.isHasNextPage());
        pi.setNavigatePages(from.getNavigatePages());
        pi.setNavigatepageNums(from.getNavigatepageNums());
        pi.setNavigateFirstPage(from.getNavigateFirstPage());
        pi.setNavigateLastPage(from.getNavigateLastPage());

        return pi;
    }
}
