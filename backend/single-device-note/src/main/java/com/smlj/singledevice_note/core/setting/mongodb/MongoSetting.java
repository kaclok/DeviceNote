package com.smlj.singledevice_note.core.setting.mongodb;

import com.smlj.singledevice_note.logic.controller.lp.LPReadConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// https://docs.springframework.org.cn/spring-data/mongodb/reference/mongodb/mapping/custom-conversions.html
// CustomConversions Bean 和 重写 configureConverters 方法）最终效果是等价的，但它们在使用方式和底层实现上存在一些细微差别。选择哪种方式主要取决于你的配置习惯和项目结构
// 集成AbstractMongoClientConfiguration的形式虽然可以方便的注册Converter，但是对于禁止_class不方便，同时AbstractMongoClientConfiguration内部会用127.0.0.1默认重写mongoClient，导致yml中配置的uri失效
@RequiredArgsConstructor
@Configuration
@Component
public class MongoSetting /*extends AbstractMongoClientConfiguration */ {
    private final LPReadConverter lpReadConverter;

    @Bean
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converterList = new ArrayList<>();
        converterList.add(lpReadConverter);
        return new MongoCustomConversions(converterList);
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory databaseFactory, MongoMappingContext mappingContext) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(databaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
        // 禁止写入_class字段
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        // 注册自定义的 Converterss
        converter.setCustomConversions(customConversions());
        converter.setCodecRegistryProvider(databaseFactory);
        return converter;
    }
}
