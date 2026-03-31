package com.jthx.x.cms.watchdog.Detector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jthx.x.cms.watchdog.pojo.ExceptionDetail;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;

public class ExceptionDetailEncoder implements Encoder.Text<ExceptionDetail> {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String encode(ExceptionDetail object) throws EncodeException {
        return toJson(object);
    }

    public static String toJson(Object object) throws EncodeException {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new EncodeException(object, "Failed to encode", e);
        }
    }
}
