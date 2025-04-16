package com.smlj.watchdog.core.utils;

import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtil {
    public static int copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        /*try (inputStream; outputStream) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            return FileCopyUtils.copy(bufferedInputStream, bufferedOutputStream);
        }*/

        // 4M缓冲区
        byte[] buf = new byte[1024 * 1024 * 4];
        int total = 0;
        int length = 0;
        while ((length = inputStream.read(buf)) > 0) {
            outputStream.write(buf, 0, length);
            total += length;
        }

        inputStream.close();
        outputStream.close();
        return total;
    }

    public static String fileExt(String filePath) {
        int lastIndexOf = filePath.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return null;
        } else {
            return filePath.substring(lastIndexOf + 1);
        }
    }
}
