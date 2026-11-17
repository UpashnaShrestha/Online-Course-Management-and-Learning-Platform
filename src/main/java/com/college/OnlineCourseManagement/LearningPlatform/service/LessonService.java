package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.LessonRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.LessonResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Lesson;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Module;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleService moduleService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessons(Long moduleId) {
        Module module = moduleService.getModule(moduleId);
        Course course = module.getCourse();
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Module not found");
        }
        enrollmentService.assertCanViewCourseContent(course, userService.getAuthenticatedUser());
        return lessonRepository.findByModuleIdOrderByOrderNumberAsc(moduleId).stream()
                .map(LearningMapper::toLessonResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getManagedLessons(Long moduleId) {
        Module module = moduleService.getModule(moduleId);
        assertCanManage(module);
        return lessonRepository.findByModuleIdOrderByOrderNumberAsc(moduleId).stream()
                .map(LearningMapper::toLessonResponse)
                .toList();
    }

    @Transactional
    public LessonResponse createLesson(Long moduleId, LessonRequest request) {
        Module module = moduleService.getModule(moduleId);
        assertCanManage(module);

        Lesson lesson = Lesson.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .content(request.getContent())
                .videoUrl(request.getVideoUrl())
                .orderNumber(request.getOrderNumber())
                .module(module)
                .build();

        return LearningMapper.toLessonResponse(lessonRepository.save(lesson));
    }

    @Transactional
    public LessonResponse updateLesson(Long id, LessonRequest request) {
        Lesson lesson = getLesson(id);
        assertCanManage(lesson.getModule());

        lesson.setTitle(request.getTitle().trim());
        lesson.setDescription(request.getDescription());
        lesson.setContent(request.getContent());
        lesson.setVideoUrl(request.getVideoUrl());
        lesson.setOrderNumber(request.getOrderNumber());
        return LearningMapper.toLessonResponse(lesson);
    }

    @Transactional
    public void deleteLesson(Long id) {
        Lesson lesson = getLesson(id);
        assertCanManage(lesson.getModule());
        lessonRepository.delete(lesson);
    }

    private Lesson getLesson(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
    }

    private void assertCanManage(Module module) {
        User currentUser = userService.getAuthenticatedUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() != Role.INSTRUCTOR || module.getCourse().getInstructors().stream()
                .noneMatch(instructor -> instructor.getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("You are not allowed to manage this course");
        }
    }
}
