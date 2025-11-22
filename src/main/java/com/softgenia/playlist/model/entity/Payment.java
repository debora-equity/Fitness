package com.softgenia.playlist.model.entity;

import com.softgenia.playlist.model.constants.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "payment")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Column(name = "transaction_id")
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "amount", precision = 7, scale = 2)
    private BigDecimal amount;

    @Size(max = 20)
    @Column(name = "currency", length = 20)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 40)
    private PaymentStatus status;

    @Column(name = "created")
    private LocalDateTime created;

}