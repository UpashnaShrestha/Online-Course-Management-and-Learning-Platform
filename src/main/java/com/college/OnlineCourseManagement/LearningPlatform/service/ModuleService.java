package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ModuleRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.ModuleResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Module;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<ModuleResponse> getPublishedModules(Long courseId) {
        Course course = getCourse(courseId);
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Course not found");
        }
        enrollmentService.assertCanViewCourseContent(course, userService.getAuthenticatedUser());
        return moduleRepository.findByCourseIdOrderByOrderNumberAsc(courseId).stream()
                .map(LearningMapper::toModuleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ModuleResponse> getManagedModules(Long courseId) {
        Course course = getCourse(courseId);
        assertCanManage(course);
        return moduleRepository.findByCourseIdOrderByOrderNumberAsc(courseId).stream()
                .map(LearningMapper::toModuleResponse)
                .toList();
    }

    @Transactional
    public ModuleResponse createModule(Long courseId, ModuleRequest request) {
        Course course = getCourse(courseId);
        assertCanManage(course);

        Module module = Module.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .orderNumber(request.getOrderNumber())
                .course(course)
                .build();

        return LearningMapper.toModuleResponse(moduleRepository.save(module));
    }

    @Transactional
    public ModuleResponse updateModule(Long id, ModuleRequest request) {
        Module module = getModule(id);
        assertCanManage(module.getCourse());

        module.setTitle(request.getTitle().trim());
        module.setDescription(request.getDescription());
        module.setOrderNumber(request.getOrderNumber());
        return LearningMapper.toModuleResponse(module);
    }

    @Transactional
    public void deleteModule(Long id) {
        Module module = getModule(id);
        assertCanManage(module.getCourse());
        moduleRepository.delete(module);
    }

    @Transactional(readOnly = true)
    public Module getModule(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found"));
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private void assertCanManage(Course course) {
        User currentUser = userService.getAuthenticatedUser();
        if (currentUser.getRole() == Role.ADMIN) {
            return;
        }
        if (currentUser.getRole() != Role.INSTRUCTOR || course.getInstructors().stream()
                .noneMatch(instructor -> instructor.getId().equals(currentUser.getId()))) {
            throw new ForbiddenException("You are not allowed to manage this course");
        }
    }
}
