package com.softgenia.playlist.model.dto.userSubscription;

import com.softgenia.playlist.model.entity.UserSubscription;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSubscriptionResponseDto {
    private Integer id;
    private String userName;
    private String userSurname;
    private String planId;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private String workoutId;
    private String documentId;

    public UserSubscriptionResponseDto(UserSubscription entity) {
        this.id = entity.getId();

        if (entity.getUser() != null) {
            this.userName = entity.getUser().getName();
            this.userSurname = entity.getUser().getSurname();
        }

        if (entity.getPlan() != null) {
            this.planId = entity.getPlan().getName();
        }

        this.startDate = entity.getStartDate();
        this.expiryDate = entity.getExpiryDate();

        if (entity.getWorkout() != null) {
            this.workoutId = entity.getWorkout().getName();
        }

        if (entity.getDocument() != null) {
            this.documentId = entity.getDocument().getOriginalFilename();
        }
    }
}
