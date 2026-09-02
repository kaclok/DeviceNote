package com.smlj.singledevice_note.core.o.from.param;

import lombok.Data;
import lombok.NoArgsConstructor;

// 用@ModelAttribute可以一次性接收多个参数
// 并且@ModelAttribute和@RequestParam可以一起混合使用
@Data
@NoArgsConstructor
public class SortParam {
    private String sortField;          // 排序字段
    private String sortOrder = "desc"; // asc 或 desc
}
