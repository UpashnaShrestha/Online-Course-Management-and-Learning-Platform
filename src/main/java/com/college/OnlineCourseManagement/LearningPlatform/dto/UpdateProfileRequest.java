package com.college.OnlineCourseManagement.LearningPlatform.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    // min = 1 rejects "" (both map to nullable = false columns); null stays valid so a client
    // can leave a field out entirely.
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    /**
     * @Email alone also accepts "" — which the service would then persist into a
     * nullable = false, unique column. @NotBlank keeps "no email" representable as null
     * (leave the field out) while rejecting a blank one.
     */
    @Email(message = "Email must be valid")
    @Size(min = 1, max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;

    @Size(max = 300, message = "Address must not exceed 300 characters")
    private String address;

    private LocalDate dateOfBirth;

    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    private String profileImage;

    @Size(max = 5000, message = "Bio must not exceed 5000 characters")
    private String bio;

    @Size(max = 150, message = "Professional title must not exceed 150 characters")
    private String professionalTitle;

    @Size(max = 500, message = "Expertise must not exceed 500 characters")
    private String expertise;

    @Size(max = 1000, message = "Qualifications must not exceed 1000 characters")
    private String qualifications;

    @Size(max = 500, message = "Teaching experience must not exceed 500 characters")
    private String teachingExperience;

    private Boolean publicInstructorProfile;
}
