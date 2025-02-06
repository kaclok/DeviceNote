package com.smlj.devicenote.logic.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 课程状态枚举
@Getter
@AllArgsConstructor
public enum EStudentStage {
    Ready(0, "准备开课"),
    Learning(5, "上课"),
    StudentEvaluateTeacher(6, "学生评价老师"),
    StudentExamine(8, "学生考试"),
    StudentWaitCorrect(11, "学生等待老师批改试卷"),
    StudentSubmitAttachment(20, "学生提交附件（心得报告、现场实操图等）"),
    StudentWaitHr(52, "等待人力资源确认"),
    OK(60, "培训流程结束");

    // 自定义状态码
    private final int code;
    // 自定义描述
    private final String message;

    public static EStudentStage fromValue(int value) {
        for (var flag : EStudentStage.values()) {
            if ((flag.code ^ value) == 0) {
                return flag;
            }
        }
        return Ready;
    }
}
