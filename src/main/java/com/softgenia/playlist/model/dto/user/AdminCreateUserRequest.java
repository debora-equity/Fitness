package com.softgenia.playlist.model.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCreateUserRequest {
    private String name;
    private String username;
    private String password;
    private String email;
    private String surname;
    private String roleName;
}
