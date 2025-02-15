package com.smlj.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/train/beatheart")
@Tag(name = "CBeatHeart", description = "心跳相关操作")
public class CBeatHeart {
    // 考试过程中，维持考试过长不断线
    @GetMapping("/beatHeart")
    public void beatHeart() {
    }
}
