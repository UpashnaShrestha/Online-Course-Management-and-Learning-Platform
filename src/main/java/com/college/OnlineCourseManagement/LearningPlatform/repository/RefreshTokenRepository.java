package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken token SET token.revokedAt = CURRENT_TIMESTAMP " +
            "WHERE token.tokenHash = :tokenHash AND token.revokedAt IS NULL")
    int revokeIfActive(@Param("tokenHash") String tokenHash);
}
