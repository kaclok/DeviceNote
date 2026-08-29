package com.smlj.singledevice_note.logic.o.vo.table.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class NetSender {
    public static String send(String url, String payload, String method) throws Exception {
        // 创建URL对象
        URL apiUrl = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();
        // 设置请求方法
        connection.setRequestMethod(method);
        // 设置允许输入输出
        connection.setDoOutput(true);
        connection.setDoInput(true);
        // 设置请求头
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");

        // 写入请求体
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }
        // 读取响应
        StringBuilder response = new StringBuilder();
        int responseCode = connection.getResponseCode();
        // 根据响应码选择输入流（200-299为成功）
        InputStream is = (responseCode >= 200 && responseCode < 300) ? connection.getInputStream() : connection.getErrorStream();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }
        // 关闭连接
        connection.disconnect();
        // 返回响应结果（包含响应码）
        return response.toString();
    }
}
