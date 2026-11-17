package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.LessonResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ModuleResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Lesson;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Module;

public final class LearningMapper {

    private LearningMapper() {
    }

    public static ModuleResponse toModuleResponse(Module module) {
        return ModuleResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderNumber(module.getOrderNumber())
                .courseId(module.getCourse().getId())
                .build();
    }

    public static LessonResponse toLessonResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .content(lesson.getContent())
                .videoUrl(lesson.getVideoUrl())
                .orderNumber(lesson.getOrderNumber())
                .moduleId(lesson.getModule().getId())
                .build();
    }
}
