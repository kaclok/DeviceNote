package com.smlj.singledevice_note.core.o.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

// https://www.jianshu.com/p/055d0162eda3
@Component
@ReadingConverter
public interface MongoDocReadConverter<T> extends Converter<Document, T> {
}
