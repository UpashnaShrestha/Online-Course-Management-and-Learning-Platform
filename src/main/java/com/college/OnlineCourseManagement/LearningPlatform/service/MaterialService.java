package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.MaterialRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.MaterialResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Lesson;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Material;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Module;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.MaterialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final LessonRepository lessonRepository;
    private final FileStorageService fileStorageService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @Transactional
    public MaterialResponse addMaterial(Long lessonId, MaterialRequest request) {
        Lesson lesson = getLesson(lessonId);
        User uploader = userService.getAuthenticatedUser();
        assertCanManage(lesson, uploader);

        // Handle file upload if a file is provided in the request
        if (request.getStoredFilePath() != null && !request.getStoredFilePath().isEmpty()) {
            // File was already stored (e.g., by controller), use the stored path
            // fileName and fileType are already set from the stored file
        } else if (request.getFile() != null && !request.getFile().isEmpty()) {
            // File is directly in the request, store it now
            try {
                String storedFileName = fileStorageService.storeFile(request.getFile());
                request.setFileName(request.getFile().getOriginalFilename());
                request.setStoredFilePath(storedFileName);
                String extension = storedFileName.substring(storedFileName.lastIndexOf('.') + 1).toLowerCase();
                request.setFileType(extension);
            } catch (Exception e) {
                throw new ResourceNotFoundException("Failed to store uploaded file: " + e.getMessage());
            }
        }

        // Use storedFilePath if available (always set when file is uploaded)
        String filePath = request.getStoredFilePath();

        Material material = Material.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .fileName(request.getFileName() != null ? request.getFileName() : "unknown")
                .filePath(filePath)
                .fileType(request.getFileType())
                .uploadedBy(uploader)
                .lesson(lesson)
                .build();

        return toResponse(materialRepository.save(material));
    }

    @Transactional
    public MaterialResponse updateMaterial(Long id, MaterialRequest request) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        assertCanManage(material.getLesson(), userService.getAuthenticatedUser());
        material.setTitle(request.getTitle().trim());
        material.setDescription(request.getDescription());

        // Update file info if a new file is provided or stored path is given
        if (request.getStoredFilePath() != null && !request.getStoredFilePath().isEmpty()) {
            material.setFileName(request.getFileName() != null ? request.getFileName() : "unknown");
            material.setFilePath(request.getStoredFilePath());
            String extension = request.getStoredFilePath().substring(request.getStoredFilePath().lastIndexOf('.') + 1).toLowerCase();
            material.setFileType(extension);
        } else if (request.getFile() != null && !request.getFile().isEmpty()) {
            try {
                String storedFileName = fileStorageService.storeFile(request.getFile());
                material.setFileName(request.getFile().getOriginalFilename());
                material.setFilePath(storedFileName);
                String extension = storedFileName.substring(storedFileName.lastIndexOf('.') + 1).toLowerCase();
                material.setFileType(extension);
            } catch (Exception e) {
                throw new ResourceNotFoundException("Failed to store uploaded file: " + e.getMessage());
            }
        } else {
            // Keep existing file info if no new file uploaded
            material.setFileName(request.getFileName() != null ? request.getFileName() : material.getFileName());
            material.setFilePath(request.getStoredFilePath() != null ? request.getStoredFilePath() : material.getFilePath());
        }

        return toResponse(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> getMaterials(Long lessonId) {
        Lesson lesson = getLesson(lessonId);
        Course course = lesson.getModule().getCourse();
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Lesson not found");
        }
        enrollmentService.assertCanViewCourseContent(course, userService.getAuthenticatedUser());
        return materialRepository.findByLessonIdOrderByUploadedAtDesc(lessonId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> getManagedMaterials(Long lessonId) {
        Lesson lesson = getLesson(lessonId);
        assertCanManage(lesson, userService.getAuthenticatedUser());
        return materialRepository.findByLessonIdOrderByUploadedAtDesc(lessonId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteMaterial(Long id) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found"));
        assertCanManage(material.getLesson(), userService.getAuthenticatedUser());
        materialRepository.delete(material);
    }

    private Lesson getLesson(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found"));
    }

    private void assertCanManage(Lesson lesson, User user) {
        Module module = lesson.getModule();
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() != Role.INSTRUCTOR || module.getCourse().getInstructors().stream()
                .noneMatch(instructor -> instructor.getId().equals(user.getId()))) {
            throw new ForbiddenException("You are not allowed to manage this course");
        }
    }

    private MaterialResponse toResponse(Material material) {
        return MaterialResponse.builder()
                .id(material.getId())
                .title(material.getTitle())
                .description(material.getDescription())
                .fileName(material.getFileName())
                .filePath(material.getFilePath())
                .fileType(material.getFileType())
                .lessonId(material.getLesson().getId())
                .uploadedById(material.getUploadedBy().getId())
                .uploadedAt(material.getUploadedAt())
                .build();
    }
}