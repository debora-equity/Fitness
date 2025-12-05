package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByTransactionId(String orderId);

    boolean existsByPlanId(Integer id);

    boolean existsByDocumentId(Integer documentId);
}
