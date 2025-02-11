package com.smlj.singledevice_note.logic.configurer;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.annotation.Annotation;

@Data
public class AnnoArgResolver<T extends Annotation> implements HandlerMethodArgumentResolver {
    private final Class<T> clazz;

    public AnnoArgResolver(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(clazz);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        var req = webRequest.getNativeRequest(HttpServletRequest.class);
        try {
            return req.getAttribute(clazz.getSimpleName());
        } catch (Exception e) {
            return null;
        }
    }
}
