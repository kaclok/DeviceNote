package com.smlj.lpjtlj.core.setting.mongodb;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class MongoNoClass {
    // // mongoTemplate操作数据库，会自动添加一个_class字段，为的的设计多态集成时候的反序列化正确
    // @TypeAlias("user") 使用简短的别名而不是完整的类名

    // 这里彻底禁用_class字段
    /*@Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory databaseFactory, MappingMongoConverter converter) {
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return new MongoTemplate(databaseFactory, converter);
    }*/
}
