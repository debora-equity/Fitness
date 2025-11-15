package com.softgenia.playlist.model.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AcceptInvitationRequest {
    private String token;
    private String username;
    private String password;
    private String name;
    private String surname;
}
