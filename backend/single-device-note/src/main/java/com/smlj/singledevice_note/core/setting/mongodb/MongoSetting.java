package com.smlj.singledevice_note.core.setting.mongodb;

import cn.hutool.json.JSONUtil;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.TlpBase;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.Tlp_czp_2;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.Tlp_gzp_2648;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.*;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// https://docs.springframework.org.cn/spring-data/mongodb/reference/mongodb/mapping/custom-conversions.html
// CustomConversions Bean 和 重写 configureConverters 方法）最终效果是等价的，但它们在使用方式和底层实现上存在一些细微差别。选择哪种方式主要取决于你的配置习惯和项目结构
// 集成AbstractMongoClientConfiguration的形式虽然可以方便的注册Converter，但是对于禁止_class不方便，同时AbstractMongoClientConfiguration内部会用127.0.0.1默认重写mongoClient，导致yml中配置的uri失效
@RequiredArgsConstructor
@Configuration
@Component
public class MongoSetting /*extends AbstractMongoClientConfiguration */ {
    public static Map<Integer, Class<? extends TlpBase>> WORKFLOW_CLS = Map.ofEntries(
            Map.entry(2648, Tlp_gzp_2648.class),
            Map.entry(2085502, Tlp_czp_2.class)
    );

    public static <T extends TlpBase> T Convert(Document source, Class<T> cls) {
        String json = source.toJson();
        T t = JSONUtil.toBean(json, cls);
        t.setRequest_id(source.getString("_id"));
        return t;
    }

    @Bean
    public CustomConversions customConversions() {
        List<Converter<?, ?>> converterList = new ArrayList<>();

        converterList.add(new Converter<Document, TlpBase>() {
            @Override
            public TlpBase convert(Document source) {
                return Convert(source, TlpBase.class);
            }
        });

        converterList.add(new Converter<Document, Tlp_gzp_2648>() {
            @Override
            public Tlp_gzp_2648 convert(Document source) {
                return Convert(source, Tlp_gzp_2648.class);
            }
        });

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
