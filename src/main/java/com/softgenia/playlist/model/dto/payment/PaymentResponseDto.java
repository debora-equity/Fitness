package com.softgenia.playlist.model.dto.payment;

import com.softgenia.playlist.model.constants.PaymentStatus;
import com.softgenia.playlist.model.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponseDto {
    private Integer id;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private LocalDateTime created;

    public PaymentResponseDto(Payment payment) {
        this.id = payment.getId();
        this.transactionId = payment.getTransactionId();
        this.amount = payment.getAmount();
        this.currency = payment.getCurrency();
        this.created = payment.getCreated();
        this.status = payment.getStatus();
    }
}
