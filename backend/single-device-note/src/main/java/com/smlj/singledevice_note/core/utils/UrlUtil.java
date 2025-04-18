package com.smlj.singledevice_note.core.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class UrlUtil {
    public static String getFullUrl(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return getFullUrl(request.getRequestURL().toString(), request.getQueryString());
    }

    public static String getFullUrl(String url, String query) {
        if (query == null) {
            return url;
        }
        return url + "?" + query;
    }

    // 解析查询字符串为 Map
    public static Map<String, String> getQueryMap(String query) {
        return Arrays.stream(query.split("&"))
                .map(param -> param.split("="))
                .collect(Collectors.toMap(
                        arr -> arr[0],
                        arr -> arr.length > 1 ? arr[1] : ""
                ));
    }
}
