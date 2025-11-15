package com.softgenia.playlist.model.dto.user.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDto {
    private Integer id;
    private String username;
    private String name;
    private String surname;
    private String email;
    private String profileImageUrl;
}
