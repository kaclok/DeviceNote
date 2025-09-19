package com.smlj.singledevice_note.core.o.converter;

import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

// https://docs.springframework.org.cn/spring-data/mongodb/reference/mongodb/mapping/custom-conversions.html
@WritingConverter
@Component
public interface MongoDocWriteConverter<T> extends Converter<T, Document> {
}
