package com.softgenia.playlist.model.dto.user.profile;

import com.softgenia.playlist.model.dto.workout.WorkoutMinDto;
import com.softgenia.playlist.model.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponseDto {
    private Integer id;
    private String username;
    private String name;
    private String surname;
    private String email;
    private String profileImageUrl;
    private List<WorkoutMinDto> workouts;

    public UserProfileResponseDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.name = user.getName();
        this.surname = user.getSurname();
        this.email = user.getEmail();
        this.profileImageUrl = user.getProfileImage();

        if (user.getWorkouts() != null) {
            this.workouts = user.getWorkouts().stream()
                    .map(WorkoutMinDto::new)
                    .collect(Collectors.toList());
        }
    }
}
