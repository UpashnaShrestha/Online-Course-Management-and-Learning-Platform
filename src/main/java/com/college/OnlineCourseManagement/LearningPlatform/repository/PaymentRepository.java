package com.college.OnlineCourseManagement.LearningPlatform.repository;

import com.college.OnlineCourseManagement.LearningPlatform.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByStudentId(Long studentId);

    List<Payment> findByStudentIdOrderByCreatedAtDesc(Long studentId);


    Optional<Payment> findByIdAndStudentId(Long id, Long studentId);

    Optional<Payment> findByKhaltiPidx(String khaltiPidx);

    Optional<Payment> findByPurchaseOrderId(String purchaseOrderId);

    Optional<Payment> findByTransactionId(String transactionId);

        Optional<Payment> findFirstByStudentIdAndCourseIdAndPaymentStatusOrderByCreatedAtDesc(
            Long studentId,
            Long courseId,
            com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentStatus paymentStatus
        );

        @Query("""
                        SELECT COALESCE(SUM(payment.amount), 0) FROM Payment payment
                        WHERE payment.course.id = :courseId
                            AND payment.paymentStatus = com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentStatus.SUCCESS
                        """)
        BigDecimal sumSuccessfulPaymentsByCourseId(@Param("courseId") Long courseId);

        @Query("""
                SELECT COALESCE(SUM(payment.amount), 0) FROM Payment payment
                WHERE payment.paymentStatus = com.college.OnlineCourseManagement.LearningPlatform.enums.PaymentStatus.SUCCESS
                """)
        BigDecimal sumSuccessfulPayments();
}
