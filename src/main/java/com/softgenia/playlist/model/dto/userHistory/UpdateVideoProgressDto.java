package com.softgenia.playlist.model.dto.userHistory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateVideoProgressDto {
    private Integer videoId;
    private Integer watchedSeconds;
    private Integer workoutId;
}
