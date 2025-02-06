package com.smlj.devicenote.logic.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 课程状态枚举
@Getter
@AllArgsConstructor
public enum ETeacherStage {
    Ready(0, "准备开课"),
    TeacherSubmitFile(1, "老师提交上课资料"),
    TeacherSubmimtTiku(3, "老师提交试卷库、提交线下试卷文档"),
    TeacherSelectStudents(4, "老师选择学生"),
    // Teaching(5, "上课"),
    TeacherSubmitAttachment(7, "老师提交课后附件（签到表、照片等）"),
    TeacherJudgePaper(30, "老师评卷、实操评分、提问评分、统计考试成绩"),
    //TeacherJudgeAttachment(11, "老师查阅学生提交的附件（心得报告、现场实操图等）"),
    TeacherSubmitHR(50, "老师提交人力资源"),
    TeacherWaitingHR(52, "等待人力资源确认"),
    OK(60, "培训流程结束");

    // 自定义状态码
    private final int code;
    // 自定义描述
    private final String message;

    public static ETeacherStage fromValue(int value) {
        for (var flag : ETeacherStage.values()) {
            if ((flag.code ^ value) == 0) {
                return flag;
            }
        }
        return Ready;
    }
}
