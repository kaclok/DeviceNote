package com.smlj.devicenote.logic.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 课程状态枚举
@Getter
@AllArgsConstructor
public enum EStage {
    Ready(0, "准备开课"),
    TeacherSubmitFile(1, "老师提交上课资料"),
    TeacherSubmimtTiku(3, "老师提交试卷库"),
    TeacherSelectStudents(4, "老师选择学生"),
    Learning(5, "上课"),
    StudentEvaluateTeacher(6, "学生评价老师"),
    TeacherSubmitAttachment(7, "老师提交课后附件（签到表、照片等）"),
    StudentExamine(8, "学生考试"),
    StudentWaitCorrect(11, "学生等待老师批改试卷"),
    StudentSubmitAttachment(20, "学生提交附件（心得报告、现场实操图等）"),
    TeacherJudgePaper(30, "老师评卷"),
    //TeacherJudgeAttachment(11, "老师查阅学生提交的附件（心得报告、现场实操图等）"),
    TeacherSubmitHR(40, "老师提交人力资源"),
    HRComplete(50, "人力资源确认"),
    OK(60, "培训流程结束");

    // 自定义状态码
    private final int code;
    // 自定义描述
    private final String message;

    public static EStage fromValue(int value) {
        for (var flag : EStage.values()) {
            if ((flag.code ^ value) == 0) {
                return flag;
            }
        }
        return Ready;
    }
}
