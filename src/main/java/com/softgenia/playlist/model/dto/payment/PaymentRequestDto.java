package com.softgenia.playlist.model.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestDto {
    private Integer planId;
    private Integer workoutId;
    private Integer documentId;
    private String currency;
    @NotNull(message = "Provider must not be null")
    private String provider;
}
