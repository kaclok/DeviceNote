package com.smlj.singledevice_note.core.o.from;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

// 用@ModelAttribute可以一次性接收多个参数

// 并且@ModelAttribute和@RequestParam可以一起混合使用
@Data
@NoArgsConstructor
public class PageParam {
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum;

    // @JsonProperty处理字段page_size

    @Min(value = 1, message = "每页大小最小为1")
    private Integer pageSize;
}

//        @Data // 嵌套示例
//        public class SearchCriteria {
//            private String name;
//            private Integer age;
//            private Address address;  // 更深层嵌套
//        }
//
//        @Data
//        public class Address {
//            private String city;
//            private String street;
//            private String zipCode;
//        }
//
//        @GetMapping("/users")
//        public Result<?> test(@ModelAttribute PageParam pageParam) {
//            // 自动绑定多层嵌套
//            // 请求参数：pageNum=1&pageSize=10&criteria.name=张三&criteria.address.city=北京
//            System.out.println("城市: " + pageParam.getCriteria().getAddress().getCity());
//            return Result.success();
//        }

