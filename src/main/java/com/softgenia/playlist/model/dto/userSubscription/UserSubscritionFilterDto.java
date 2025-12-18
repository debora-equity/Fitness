package com.softgenia.playlist.model.dto.userSubscription;

import com.softgenia.playlist.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSubscritionFilterDto {
    private String search;
    private String userName;
    private String userSurname;
    private String planId;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private String workoutId;
    private String documentId;

    public void formatData(){
        this.search = StringUtils.getFormattedStringFilter(this.getSearch());
        this.userName = StringUtils.getFormattedStringFilter(this.getUserName());
        this.userSurname = StringUtils.getFormattedStringFilter(this.getUserSurname());
        this.planId = StringUtils.getFormattedStringFilter(this.planId);
        this.workoutId = StringUtils.getFormattedStringFilter(this.workoutId);
        this.documentId = StringUtils.getFormattedStringFilter(this.documentId);
    }
}
