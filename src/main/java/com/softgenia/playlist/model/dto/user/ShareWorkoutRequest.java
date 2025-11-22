package com.softgenia.playlist.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShareWorkoutRequest {
    @NotNull
    private Integer workoutId;

    @NotEmpty
    @Email
    private String recipientEmail;
}
