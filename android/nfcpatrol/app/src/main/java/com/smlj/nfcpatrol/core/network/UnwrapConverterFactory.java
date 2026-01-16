package com.smlj.nfcpatrol.core.network;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class UnwrapConverterFactory extends Converter.Factory {
    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(@NonNull Type type, @NonNull Annotation[] annotations, Retrofit retrofit) {
        Type wrappedType = new ParameterizedType() {
            @NonNull
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[]{type};
            }

            @Override
            public Type getOwnerType() {
                return null;
            }

            @NonNull
            @Override
            public Type getRawType() {
                return Result.class;
            }
        };

        final Converter<ResponseBody, ?> delegate = retrofit.nextResponseBodyConverter(this, wrappedType, annotations);

        return new Converter<ResponseBody, Object>() {
            @Override
            public Object convert(@NonNull ResponseBody value) throws IOException {
                try (value) {
                    // 去除Result包装
                    Result<?> result = (Result<?>) delegate.convert(value);
                    if (result == null) {
                        throw new IOException("Response body is null");
                    }

                    // 处理服务器返回的错误码
                    if (result.getCode() != 200) {
                        // 这里抛出的异常会在 onFailure 中被捕获
                        String errorMsg = result.getMessage() != null ? result.getMessage() : "服务器返回错误码: " + result.getCode();
                        throw new ApiException(result.getCode(), errorMsg);
                    }

                    // 如果 data 为 null，根据业务需求决定是否返回 null 或抛异常
                    return result.getData();
                }
            }
        };
    }
}
