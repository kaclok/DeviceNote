package com.jthx.x.core.utils;

import jakarta.servlet.ServletResponse;

import java.io.IOException;

public class ServletResponseUtil {
    public static void WriteJson(ServletResponse response, String json) throws IOException {
        response.getWriter().write(json);
    }
}
