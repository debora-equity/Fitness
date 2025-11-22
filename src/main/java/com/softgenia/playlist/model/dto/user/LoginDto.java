package com.softgenia.playlist.model.dto.user;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDto {
    private String username;
    private String password;
}
