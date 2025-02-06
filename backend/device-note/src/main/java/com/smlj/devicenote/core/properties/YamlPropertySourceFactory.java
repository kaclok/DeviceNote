package com.smlj.devicenote.core.properties;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;

// https://mp.weixin.qq.com/s/7l5J6nngBtMizJOnFdiIjw @PropertySource只支持properties文件，所以无法加载yml文件。不过我们可以通过如下方式加载yml配置文件并将其注册为bean对象
// https://blog.csdn.net/zhanhjxxx/article/details/122948061
// 把自定义配置文件.yml的读取方式变成跟application.yml的读取方式一致的 xx.xx.xx
public class YamlPropertySourceFactory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource encodedResource) throws IOException {
        return new YamlPropertySourceLoader().load(name, encodedResource.getResource()).get(0);
    }
}
