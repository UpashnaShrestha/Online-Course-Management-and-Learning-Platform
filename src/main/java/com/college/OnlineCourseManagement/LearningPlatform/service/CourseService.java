package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseOptionResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.CourseResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Category;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Course;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseDuration;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseLevel;
import com.college.OnlineCourseManagement.LearningPlatform.enums.CourseStatus;
import com.college.OnlineCourseManagement.LearningPlatform.enums.NotificationType;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CourseService {

    /** Draft and rejected courses may be edited, resubmitted and deleted. */
    private static final Set<CourseStatus> MUTABLE_STATUSES = EnumSet.of(
            CourseStatus.DRAFT,
            CourseStatus.REJECTED
    );

    private static final Set<Role> INSTRUCTOR_ROLES = EnumSet.of(Role.INSTRUCTOR, Role.ADMIN);
    private static final Set<Role> INSTRUCTOR_ONLY = EnumSet.of(Role.INSTRUCTOR);


    private final CourseRepository courseRepository;
    private final CategoryService categoryService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<CourseResponse> getPublishedCourses() {
        return courseRepository.findByStatusWithDetails(CourseStatus.PUBLISHED).stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse getPublishedCourseById(Long id) {
        Course course = courseRepository.findByIdWithDetails(id)
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        return CourseMapper.toResponse(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        User currentUser = userService.getAuthenticatedUser();
        validateInstructorRole(currentUser);

        Category category = categoryService.getCategoryById(request.getCategoryId());
        Set<User> instructors = resolveInstructors(request.getInstructorIds(), currentUser);

        Course course = Course.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .thumbnail(request.getThumbnail())
                .level(canonicalLevel(request.getLevel()))
                .duration(canonicalDuration(request.getDuration()))
                .category(category)
                .status(CourseStatus.DRAFT)
                .instructors(instructors)
                .build();

        Course savedCourse = courseRepository.save(course);
        return CourseMapper.toResponse(loadCourseWithDetails(savedCourse.getId()));
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        User currentUser = userService.getAuthenticatedUser();
        Course course = loadCourseWithDetails(id);

        assertCanManageCourse(course, currentUser);

        if (!MUTABLE_STATUSES.contains(course.getStatus())) {
            throw new BadRequestException("Only draft or rejected courses can be updated");
        }

        Category category = categoryService.getCategoryById(request.getCategoryId());
        Set<User> instructors = resolveInstructors(request.getInstructorIds(), currentUser);

        course.setTitle(request.getTitle().trim());
        course.setDescription(request.getDescription());
        course.setPrice(request.getPrice());
        course.setThumbnail(request.getThumbnail());
        course.setLevel(canonicalLevel(request.getLevel()));
        course.setDuration(canonicalDuration(request.getDuration()));
        course.setCategory(category);
        course.setInstructors(instructors);

        return CourseMapper.toResponse(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        User currentUser = userService.getAuthenticatedUser();
        Course course = loadCourseWithDetails(id);

        assertCanManageCourse(course, currentUser);

        if (!MUTABLE_STATUSES.contains(course.getStatus())) {
            throw new BadRequestException("Published or pending courses cannot be deleted");
        }

        courseRepository.delete(course);
    }

    @Transactional
    public CourseResponse submitCourse(Long id) {
        User currentUser = userService.getAuthenticatedUser();
        Course course = loadCourseWithDetails(id);

        assertCanManageCourse(course, currentUser);

        if (!MUTABLE_STATUSES.contains(course.getStatus())) {
            throw new BadRequestException("Only draft or rejected courses can be submitted for approval");
        }

        course.setStatus(CourseStatus.PENDING_APPROVAL);
        return CourseMapper.toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getPendingCourses() {
        requireAdmin();
        return courseRepository.findByStatusWithDetails(CourseStatus.PENDING_APPROVAL).stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    /**
     * Defence in depth: the controller already restricts these paths to admins, but the
     * service refuses privileged operations regardless of how it was reached.
     */
    private void requireAdmin() {
        if (userService.getAuthenticatedUser().getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can perform this action");
        }
    }

    /**
     * Converts a submitted level to the exact label the dropdown offers.
     *
     * {@code @OneOf} already rejects values outside the closed set, so an unrecognised value
     * reaching this point can only be a blank field, which is stored as-is. Normalising here
     * means "BEGINNER", "beginner" and "Beginner " all become "Beginner" and cannot reappear
     * as separate entries in the catalog filter.
     */
    private String canonicalLevel(String level) {
        CourseLevel resolved = CourseLevel.fromText(level);
        return resolved != null ? resolved.getLabel() : level;
    }

    /** As {@link #canonicalLevel(String)}, for the duration. */
    private String canonicalDuration(String duration) {
        CourseDuration resolved = CourseDuration.fromText(duration);
        return resolved != null ? resolved.getLabel() : duration;
    }

    /**
     * The level and duration options offered when authoring a course.
     *
     * Served from the same enums the request DTO validates against, so the dropdown the
     * instructor sees and the values the API accepts cannot drift apart.
     */
    @Transactional(readOnly = true)
    public Map<String, List<CourseOptionResponse>> getCourseOptions() {
        List<CourseOptionResponse> levels = Arrays.stream(CourseLevel.values())
                .map(level -> CourseOptionResponse.builder()
                        .value(level.getLabel())
                        .label(level.getLabel())
                        .build())
                .toList();

        List<CourseOptionResponse> durations = Arrays.stream(CourseDuration.values())
                .map(duration -> CourseOptionResponse.builder()
                        .value(duration.getLabel())
                        .label(duration.getLabel())
                        .approximateHours(duration.getApproximateHours())
                        .build())
                .toList();

        return Map.of("levels", levels, "durations", durations);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getInstructorCourses() {
        User currentUser = userService.getAuthenticatedUser();
        validateInstructorRole(currentUser);
        return courseRepository.findByInstructorIdWithDetails(currentUser.getId()).stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Course getInstructorCourse(Long id) {
        User currentUser = userService.getAuthenticatedUser();
        Course course = loadCourseWithDetails(id);
        assertCanManageCourse(course, currentUser);
        return course;
    }

    @Transactional
    public CourseResponse approveCourse(Long id) {
        requireAdmin();
        Course course = loadCourseWithDetails(id);

        if (course.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending courses can be approved");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        notifyInstructors(course, "Course approved",
                "\"" + course.getTitle() + "\" is now live in the catalog.");
        return CourseMapper.toResponse(course);
    }

    @Transactional
    public CourseResponse rejectCourse(Long id) {
        requireAdmin();
        Course course = loadCourseWithDetails(id);

        if (course.getStatus() != CourseStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Only pending courses can be rejected");
        }

        course.setStatus(CourseStatus.REJECTED);
        notifyInstructors(course, "Course rejected",
                "\"" + course.getTitle() + "\" was rejected. Please review it and resubmit for approval.");
        return CourseMapper.toResponse(course);
    }

    private void notifyInstructors(Course course, String title, String message) {
        for (User instructor : course.getInstructors()) {
            notificationService.notify(instructor, title, message, NotificationType.COURSE,
                    "/instructor/courses");
        }
    }

    private Course loadCourseWithDetails(Long id) {
        return courseRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
    }

    private void validateInstructorRole(User user) {
        if (!INSTRUCTOR_ROLES.contains(user.getRole())) {
            throw new ForbiddenException("Only instructors can manage courses");
        }
    }

    private void assertCanManageCourse(Course course, User user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }

        validateInstructorRole(user);

        boolean isAssignedInstructor = course.getInstructors().stream()
                .anyMatch(instructor -> instructor.getId().equals(user.getId()));

        if (!isAssignedInstructor) {
            throw new ForbiddenException("You are not allowed to manage this course");
        }
    }

    private Set<User> resolveInstructors(
            Set<Long> instructorIds,
            User currentUser) {

        Set<User> instructors = new HashSet<>();

        // Automatically assign the authenticated instructor.
        if (currentUser.getRole() == Role.INSTRUCTOR) {
            instructors.add(currentUser);
        }

        // Admin must explicitly select at least one instructor.
        if (instructorIds == null || instructorIds.isEmpty()) {
            if (currentUser.getRole() == Role.ADMIN) {
                throw new BadRequestException(
                        "At least one instructor must be selected"
                );
            }

            return instructors;
        }

        Set<Long> uniqueIds = new HashSet<>(instructorIds);

        // Avoid adding the authenticated instructor twice.
        uniqueIds.remove(currentUser.getId());

        if (uniqueIds.isEmpty()) {
            return instructors;
        }

        List<User> additionalInstructors =
                userRepository.findByIdInAndRoleIn(
                        uniqueIds,
                        INSTRUCTOR_ONLY
                );

        if (additionalInstructors.size() != uniqueIds.size()) {
            throw new BadRequestException(
                    "One or more instructor ids are invalid"
            );
        }

        instructors.addAll(additionalInstructors);

        return instructors;
    }
}
