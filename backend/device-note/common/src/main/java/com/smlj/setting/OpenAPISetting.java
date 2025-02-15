package com.smlj.setting;

import com.smlj.utils.JwtUtil;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// https://swagger.io/docs/open-source-tools/swagger-ui/usage/version-detection/ 检测swagger-ui的版本
// https://blog.csdn.net/weixin_37833693/article/details/137050893
@OpenAPIDefinition(
        info = @Info(
                title = "Train OpenAPI3.1",
                version = "0.0.1",
                description = "OpenAPI3.1结合SwaggerUI在Springboot3中的演示",
                contact = @Contact(name = "崔斌斌", email = "572005238@qq.com"),
                license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html")
        ),
        externalDocs = @ExternalDocumentation(description = "参考文档", url = "https://swagger.io/tools/swagger-ui/"),
        // https://www.cnblogs.com/xiezhr/p/18253311#:~:text=%E5%9C%A8%20application.yml%20%E4%B8%AD%E5%8F%AF%E4%BB%A5%E8%87%AA%E5%AE%9A%E4%B9%89%20api-docs%20%E5%92%8C%20swagger-ui%20%E7%9A%84%E8%AE%BF%E9%97%AE%E8%B7%AF%E5%BE%84%E3%80%82%20%E5%BD%93%E7%84%B6%E4%BA%86%EF%BC%8C%E5%A6%82%E6%9E%9C%E6%B2%A1%E9%85%8D%E7%BD%AE%EF%BC%8C%E9%BB%98%E8%AE%A4%E5%B0%B1%E6%98%AF%E4%B8%8B%E9%9D%A2%E8%B7%AF%E5%BE%84,config%20%E5%8C%85---%3E%E5%B9%B6%E5%9C%A8%E5%8C%85%E4%B8%8B%E5%BB%BA%E7%AB%8B%E4%B8%80%E4%B8%AA%20SpringDocConfig%20%E9%85%8D%E7%BD%AE%E7%B1%BB%20%E2%91%A1%20%E9%85%8D%E7%BD%AE%E6%8E%A5%E5%8F%A3%E6%96%87%E6%A1%A3%E5%9F%BA%E7%A1%80%E4%BF%A1%E6%81%AF%20%E6%88%91%E4%BB%AC%E5%9C%A8%E9%85%8D%E7%BD%AE%E7%B1%BB%E4%B8%AD%E6%B7%BB%E5%8A%A0%E5%A6%82%E4%B8%8B%E4%BB%A3%E7%A0%81%EF%BC%8C%20.info%28this.getApiInfo%28%29%29
        servers = {
                @Server(url = "http://localhost:8092",
                        description = "开发server"),
                @Server(url = "http://localhost:8090",
                        description = "测试server")
        },
        // https://springdoc.cn/spring-boot-swagger-jwt/
        // https://springdoc.cn/springdoc-openapi-form-login-and-basic-authentication/
        // https://www.baeldung-cn.com/springdoc-openapi-form-login-and-basic-authentication
        // 不添加这项：会导致每个请求后面没有小锁
        // 控制每个请求后面的小锁里面有几个填空
        security = {
                @SecurityRequirement(name = JwtUtil.JWT_HEADER),
                @SecurityRequirement(name = JwtUtil.AUTHORIZE)
        }
)

// https://swagger.org.cn/docs/open-source-tools/swagger-ui/usage/version-detection/ F12控制台输入JSON.stringify(versions)获取swagger-ui的版本

// Springdoc 使用@SecurityScheme 来定义一个安全模式，我们可以定义全局的，也可以针对某个controller定义类级别的
// https://blog.csdn.net/xiyang_1990/article/details/140739948
// 没有这个配置SecurityScheme会导致swagger-ui页面没有authority按钮,不管是每个请求的小锁 还是 总的小锁
@SecuritySchemes({
        // 每次添加一个SecurityScheme,就会在authority按钮里面添加一个选项，意味着可以同时填写多个token，多个cookie进去
        @SecurityScheme(type = SecuritySchemeType.APIKEY, name = JwtUtil.JWT_HEADER, /*bearerFormat = "JWT", scheme = "bearer",*/ in = SecuritySchemeIn.HEADER)
        , @SecurityScheme(type = SecuritySchemeType.HTTP, name = JwtUtil.AUTHORIZE, bearerFormat = "JWT", scheme = "bearer", in = SecuritySchemeIn.HEADER)
})

// https://blog.csdn.net/xiyang_1990/article/details/140739948
@Configuration
public class OpenAPISetting /*extends SwaggerWebMvcConfigurer*/ {
    @Bean
    public GroupedOpenApi allApis() {
        var builder = GroupedOpenApi.builder();
        return builder.group("--all--").pathsToMatch("/train/**").build();
    }

    @Bean
    public GroupedOpenApi api_admin() {
        var builder = GroupedOpenApi.builder();
        return builder.group("admin").pathsToMatch("/train/admin/**").build();
    }

    @Bean
    public GroupedOpenApi api_captcha() {
        var builder = GroupedOpenApi.builder();
        return builder.group("captcha").pathsToMatch("/train/captcha/**").build();
    }

    @Bean
    public GroupedOpenApi api_course() {
        var builder = GroupedOpenApi.builder();
        return builder.group("course").pathsToMatch("/train/course/**").build();
    }

    @Bean
    public GroupedOpenApi api_file() {
        var builder = GroupedOpenApi.builder();
        return builder.group("file").pathsToMatch("/train/file/**").build();
    }

    @Bean
    public GroupedOpenApi api_user() {
        var builder = GroupedOpenApi.builder();
        return builder.group("user").pathsToMatch("/train/user/**").build();
    }

    @Bean
    public GroupedOpenApi api_minioFile() {
        var builder = GroupedOpenApi.builder();
        return builder.group("minioFile").pathsToMatch("/train/minioFile/**").build();
    }

    @Bean
    public GroupedOpenApi api_sync() {
        var builder = GroupedOpenApi.builder();
        return builder.group("sync").pathsToMatch("/train/sync/**").build();
    }

    @Bean
    public GroupedOpenApi api_person() {
        var builder = GroupedOpenApi.builder();
        return builder.group("person").pathsToMatch("/train/person/**").build();
    }

    @Bean
    public GroupedOpenApi api_table() {
        var builder = GroupedOpenApi.builder();
        return builder.group("table").pathsToMatch("/train/table/**").build();
    }
}
