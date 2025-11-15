package com.softgenia.playlist.model.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendInvitationRequest {
    private String roleName;
    private String email;
    private Integer workoutId;
}
