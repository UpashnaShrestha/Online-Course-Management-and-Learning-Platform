package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.dto.ChangePasswordRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.AccountDeletionRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.UpdateProfileRequest;
import com.college.OnlineCourseManagement.LearningPlatform.dto.UserResponse;
import com.college.OnlineCourseManagement.LearningPlatform.dto.AdminUserDetailResponse;
import com.college.OnlineCourseManagement.LearningPlatform.entity.User;
import com.college.OnlineCourseManagement.LearningPlatform.entity.AccountDeletionAudit;
import com.college.OnlineCourseManagement.LearningPlatform.entity.Enrollment;
import com.college.OnlineCourseManagement.LearningPlatform.enums.Role;
import com.college.OnlineCourseManagement.LearningPlatform.exception.BadRequestException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ForbiddenException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.DuplicateResourceException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.ResourceNotFoundException;
import com.college.OnlineCourseManagement.LearningPlatform.exception.UnauthorizedException;
import com.college.OnlineCourseManagement.LearningPlatform.repository.UserRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.AccountDeletionAuditRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.AssignmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CertificateRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.CourseRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.EnrollmentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.LessonProgressRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.NotificationRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PasswordResetOtpRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.PaymentRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.ReviewRepository;
import com.college.OnlineCourseManagement.LearningPlatform.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageStorage profileImageStorage;
    private final AccountDeletionAuditRepository accountDeletionAuditRepository;
    private final AssignmentRepository assignmentRepository;
    private final CertificateRepository certificateRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        return UserMapper.toResponse(getAuthenticatedUser());
    }

    @Transactional
    public UserResponse updateCurrentUser(UpdateProfileRequest request) {
        User user = getAuthenticatedUser();

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress().trim());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getProfileImage() != null) {
            String newImage = request.getProfileImage().trim();
            String previous = user.getProfileImage();

            user.setProfileImage(newImage);

            // Replacing or clearing the URL leaves the old file on disk. Remove it, but only
            // once the change is committed, so a failed save does not lose the picture the
            // user can still see.
            if (previous != null && !previous.equals(newImage)) {
                deleteStoredImageAfterCommit(previous);
            }
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }
        if (user.getRole() == Role.INSTRUCTOR) {
            if (request.getProfessionalTitle() != null) user.setProfessionalTitle(request.getProfessionalTitle().trim());
            if (request.getExpertise() != null) user.setExpertise(request.getExpertise().trim());
            if (request.getQualifications() != null) user.setQualifications(request.getQualifications().trim());
            if (request.getTeachingExperience() != null) user.setTeachingExperience(request.getTeachingExperience().trim());
            if (request.getPublicInstructorProfile() != null) user.setPublicInstructorProfile(request.getPublicInstructorProfile());
        }

        return UserMapper.toResponse(user);
    }

    /**
     * Replaces the signed-in user's profile picture with an uploaded image.
     *
     * The previous file is deleted by the storage service once the new one is safely written,
     * so a failure never leaves the user with no picture at all.
     */
    @Transactional
    public UserResponse updateProfileImage(MultipartFile file) {
        User user = getAuthenticatedUser();
        String url = profileImageStorage.store(user.getId(), file, user.getProfileImage());

        user.setProfileImage(url);
        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Clears the signed-in user's profile picture and deletes the stored file.
     *
     * @return true when a picture was actually removed
     */
    @Transactional
    public boolean removeProfileImage() {
        User user = getAuthenticatedUser();
        String previous = user.getProfileImage();

        if (previous == null || previous.isBlank()) {
            return false;
        }

        user.setProfileImage(null);
        userRepository.save(user);

        deleteStoredImageAfterCommit(previous);
        return true;
    }

    /**
     * Deletes a stored upload once the current transaction commits.
     *
     * Doing it immediately would remove the file even if the transaction then rolled back,
     * leaving a profile pointing at a file that no longer exists.
     */
    private void deleteStoredImageAfterCommit(String url) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    profileImageStorage.deleteQuietly(url);
                }
            });
        } else {
            profileImageStorage.deleteQuietly(url);
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirmation do not match");
        }

        User user = getAuthenticatedUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current one");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        if (getAuthenticatedUser().getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can list all users");
        }

        return userRepository.findAll().stream()
                .map(UserMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getAdminUserDetails(Long userId) {
        User admin = getAuthenticatedUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can view user details");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        UserResponse response = UserMapper.toResponse(user);

        if (user.getRole() == Role.STUDENT) {
            List<Enrollment> enrollments = enrollmentRepository.findByStudentIdOrderByEnrolledAtDesc(userId);
            List<AdminUserDetailResponse.StudentActivity.Enrollment> enrollmentDetails = enrollments.stream()
                    .map(enrollment -> new AdminUserDetailResponse.StudentActivity.Enrollment(
                            enrollment.getCourse().getId(),
                            enrollment.getCourse().getTitle(),
                            enrollment.getCourse().getInstructors().stream()
                                    .map(instructor -> instructor.getFirstName() + " " + instructor.getLastName())
                                    .toList(),
                            progressPercentage(enrollment.getCourse(), userId),
                            enrollment.getEnrolledAt(),
                            enrollment.getCompletedAt()
                    )).toList();

            List<Long> courseIds = enrollments.stream().map(enrollment -> enrollment.getCourse().getId()).toList();
            List<AdminUserDetailResponse.StudentActivity.AssignmentActivity> assignments =
                    courseIds.isEmpty() ? List.of() : assignmentRepository.findByCourseIdInOrderByDueDateAsc(courseIds).stream()
                            .map(assignment -> submissionRepository.findTopByAssignmentIdAndStudentIdOrderByAttemptNumberDesc(
                                    assignment.getId(), userId)
                                    .map(submission -> new AdminUserDetailResponse.StudentActivity.AssignmentActivity(
                                            assignment.getId(), assignment.getTitle(), assignment.getCourse().getTitle(),
                                            submission.getStatus().name(), submission.getGrade(), submission.getFeedback(),
                                            assignment.getDueDate(), submission.getSubmittedAt()))
                                    .orElseGet(() -> new AdminUserDetailResponse.StudentActivity.AssignmentActivity(
                                            assignment.getId(), assignment.getTitle(), assignment.getCourse().getTitle(),
                                            "NOT_SUBMITTED", null, null, assignment.getDueDate(), null)))
                            .toList();

            long completedLessons = enrollments.stream()
                    .mapToLong(enrollment -> lessonProgressRepository.countCompletedLessons(
                            userId, enrollment.getCourse().getId()))
                    .sum();

            List<AdminUserDetailResponse.StudentActivity.Payment> payments =
                    paymentRepository.findByStudentIdOrderByCreatedAtDesc(userId).stream()
                            .map(payment -> new AdminUserDetailResponse.StudentActivity.Payment(
                                    payment.getId(),
                                    payment.getCourse().getTitle(),
                                    payment.getAmount(),
                                    payment.getPaymentStatus().name(),
                                    payment.getTransactionId(),
                                    payment.getCreatedAt(),
                                    payment.getPaidAt()
                            )).toList();

            List<AdminUserDetailResponse.StudentActivity.Certificate> certificates =
                    certificateRepository.findByStudentIdOrderByIssuedAtDesc(userId).stream()
                            .map(certificate -> new AdminUserDetailResponse.StudentActivity.Certificate(
                                    certificate.getId(),
                                    certificate.getCourse().getTitle(),
                                    certificate.getCertificateNumber(),
                                    certificate.getIssuedAt()
                            )).toList();

            return new AdminUserDetailResponse(
                    response,
                    new AdminUserDetailResponse.StudentActivity(
                            enrollmentDetails,
                            new AdminUserDetailResponse.StudentActivity.LearningSummary(
                                    completedLessons,
                                    enrollments.stream().filter(enrollment -> enrollment.getCompletedAt() != null).count(),
                                    certificates.size()),
                            assignments,
                            payments,
                            certificates),
                    null
            );
        }

        if (user.getRole() == Role.INSTRUCTOR) {
            List<com.college.OnlineCourseManagement.LearningPlatform.entity.Course> courses =
                    courseRepository.findByInstructorIdWithDetails(userId);
            List<AdminUserDetailResponse.InstructorActivity.Course> courseDetails = courses.stream()
                    .map(course -> new AdminUserDetailResponse.InstructorActivity.Course(
                            course.getId(),
                            course.getTitle(),
                            course.getStatus().name(),
                            enrollmentRepository.countByCourseIdAndStatusNot(
                                    course.getId(), com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus.CANCELLED),
                            assignmentRepository.countByCourseId(course.getId()),
                            course.getCreatedAt()
                    )).toList();
            Set<Long> courseIds = courses.stream().map(com.college.OnlineCourseManagement.LearningPlatform.entity.Course::getId)
                    .collect(Collectors.toSet());
            long graded = courseIds.isEmpty() ? 0 : submissionRepository.findByAssignmentCourseIdIn(courseIds).stream()
                    .filter(submission -> submission.getGrade() != null)
                    .count();
            List<BigDecimal> studentProgress = courses.stream()
                    .flatMap(course -> enrollmentRepository.findByCourseIdAndStatusNot(
                                    course.getId(), com.college.OnlineCourseManagement.LearningPlatform.enums.EnrollmentStatus.CANCELLED)
                            .stream())
                    .map(enrollment -> progress(enrollment.getCourse(), enrollment.getStudent().getId()))
                    .toList();
            BigDecimal averageProgress = studentProgress.isEmpty() ? BigDecimal.ZERO
                    : studentProgress.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(studentProgress.size()), 2, java.math.RoundingMode.HALF_UP);

            return new AdminUserDetailResponse(
                    response,
                    null,
                    new AdminUserDetailResponse.InstructorActivity(
                            courseDetails,
                            courseDetails.stream().mapToLong(AdminUserDetailResponse.InstructorActivity.Course::assignments).sum(),
                            courseDetails.stream().mapToLong(AdminUserDetailResponse.InstructorActivity.Course::enrolledStudents).sum(),
                            graded,
                            courseDetails.stream().filter(course -> "PUBLISHED".equals(course.status())).count(),
                            courseDetails.stream().filter(course -> "PENDING".equals(course.status())).count(),
                            courseDetails.stream().filter(course -> "REJECTED".equals(course.status())).count(),
                            averageProgress
                    )
            );
        }

        return new AdminUserDetailResponse(response, null, null);
    }

    private BigDecimal progress(com.college.OnlineCourseManagement.LearningPlatform.entity.Course course, Long studentId) {
        long total = lessonProgressRepository.countLessonsByCourseId(course.getId());
        long completed = lessonProgressRepository.countCompletedLessons(studentId, course.getId());
        return total == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(completed * 100.0 / total).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal progressPercentage(com.college.OnlineCourseManagement.LearningPlatform.entity.Course course, Long studentId) {
        return progress(course, studentId);
    }

    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled) {
        User admin = getAuthenticatedUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can change user status");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getId().equals(admin.getId()) && !enabled) {
            throw new BadRequestException("Administrators cannot disable their own account");
        }

        user.setEnabled(enabled);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId, AccountDeletionRequest request) {
        User admin = getAuthenticatedUser();
        if (admin.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only administrators can delete accounts");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("Administrators cannot delete their own account");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Administrator accounts cannot be deleted");
        }

        if (courseRepository.existsByInstructorsId(userId)) {
            throw new BadRequestException("Reassign this instructor's courses before deleting the account");
        }
        if (assignmentRepository.existsByCreatedById(userId)
                || enrollmentRepository.existsByStudentId(userId)
                || paymentRepository.existsByStudentId(userId)
                || submissionRepository.existsByStudentId(userId)
                || certificateRepository.existsByStudentId(userId)
                || lessonProgressRepository.existsByStudentId(userId)
                || reviewRepository.existsByStudentId(userId)) {
            throw new BadRequestException("This account has learning records. Disable it instead to preserve those records");
        }
        if (notificationRepository.existsByUserId(userId)
                || passwordResetOtpRepository.existsByUserId(userId)) {
            throw new BadRequestException("This account has system records and cannot be safely deleted");
        }

        accountDeletionAuditRepository.save(AccountDeletionAudit.builder()
                .deletedUserId(user.getId())
                .deletedUsername(user.getUsername())
                .deletedUserRole(user.getRole())
                .reason(request.reason().trim())
                .adminUserId(admin.getId())
                .adminUsername(admin.getUsername())
                .build());
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new UnauthorizedException("You must be signed in to perform this action");
        }

        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
