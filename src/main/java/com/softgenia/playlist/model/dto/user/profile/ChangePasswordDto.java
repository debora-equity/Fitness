package com.softgenia.playlist.model.dto.user.profile;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePasswordDto {
    @NotEmpty
    private String currentPassword;

    @NotEmpty
    @Size(min = 8, message = "New password must be at least 8 characters long")
    private String newPassword;
}
