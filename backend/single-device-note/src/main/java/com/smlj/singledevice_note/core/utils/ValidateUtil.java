package com.smlj.singledevice_note.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;

import java.util.*;

@Slf4j
public class ValidateUtil {
    public static Map<String, String> collectErrors(BindingResult bindingResult) {
        return collectErrors((Errors) bindingResult);
    }

    public static Map<String, String> collectErrors(Errors errors) {
        if (errors != null && errors.hasErrors()) {
            Map<String, String> errorLs = new HashMap<>();
            errors.getFieldErrors().forEach(error -> errorLs.put(error.getField(), error.getDefaultMessage()));
            return errorLs;
        }
        return null;
    }
}
