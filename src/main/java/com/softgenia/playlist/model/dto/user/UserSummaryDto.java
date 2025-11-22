package com.softgenia.playlist.model.dto.user;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryDto {
    private Integer id;
    private String username;
    private String name;
    private String surname;
    private String email;
    private Roles roles;
    private String image;

    public UserSummaryDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.surname = user.getSurname();
        this.email = user.getEmail();
        this.image = user.getProfileImage();
        if (user.getRole() !=null)
            this.roles = user.getRole().getName();
    }
}
