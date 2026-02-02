package com.softgenia.playlist.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ManualSubscriptionRequest {
    @NotNull
    private Integer userId;

    private Integer documentId;
    private Integer planId;

    private Integer workoutId;

    private Integer durationInMonths;
}
