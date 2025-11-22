package com.softgenia.playlist.model.dto.payment;

import com.softgenia.playlist.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// A simple DTO to hold the result from the gateway
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResult {
    private boolean success;
    private String transactionId;
    private String errorMessage;
}

