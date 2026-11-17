package com.college.OnlineCourseManagement.LearningPlatform.service;

import com.college.OnlineCourseManagement.LearningPlatform.entity.PasswordResetOtp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generates and verifies short numeric one-time codes.
 *
 * Codes are never stored: only a SHA-256 of (salt + code) is persisted, and comparison is
 * constant time so a timing side channel cannot be used to recover the code.
 */
@Service
public class OtpService {

    /**
     * Compile-time constants so {@code @Pattern} on ResetPasswordRequest and the generator
     * below can never disagree. If the two ever did, the DTO would reject every code this
     * service can produce.
     */
    public static final int LENGTH = 6;
    public static final String REGEX = "^[0-9]{" + LENGTH + "}$";

    private static final int MIN_LENGTH = 4;
    private static final int MAX_LENGTH = 10;

    private final SecureRandom secureRandom = new SecureRandom();
    private final int otpLength;

    public OtpService(@Value("${app.password-reset.otp-length:" + LENGTH + "}") int otpLength) {
        if (otpLength < MIN_LENGTH || otpLength > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "app.password-reset.otp-length must be between " + MIN_LENGTH
                            + " and " + MAX_LENGTH + " but was " + otpLength);
        }
        if (otpLength != LENGTH) {
            // The regex baked into ResetPasswordRequest is a compile-time constant, so a
            // different length would make every issued code fail validation. Fail loudly at
            // startup rather than at the user's first password reset.
            throw new IllegalArgumentException(
                    "app.password-reset.otp-length is fixed at " + LENGTH
                            + " (it is compiled into ResetPasswordRequest's @Pattern) but was "
                            + otpLength);
        }
        this.otpLength = otpLength;
    }

    public int getOtpLength() {
        return otpLength;
    }

    /** Regex fragment matching exactly the number of digits this service generates. */
    public String pattern() {
        return REGEX;
    }

    public String generate() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength);

        return String.format("%0" + otpLength + "d", min + secureRandom.nextInt(max - min));
    }

    public String newSalt() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public String hash(String salt, String otp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest((salt + ':' + otp).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    /** Constant-time check of a submitted code against the stored hash. */
    public boolean matches(PasswordResetOtp record, String submittedOtp) {
        if (record == null || submittedOtp == null) {
            return false;
        }

        String expected = hash(record.getSalt(), submittedOtp);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                record.getOtpHash().getBytes(StandardCharsets.UTF_8));
    }

    /** Length assumed when nothing is configured. */
    public static int defaultLength() {
        return LENGTH;
    }
}