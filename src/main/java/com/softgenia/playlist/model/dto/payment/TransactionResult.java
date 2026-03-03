package com.softgenia.playlist.model.dto.payment;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResult {
    private boolean success;
    private String transactionId;
    private String errorMessage;
}

