package com.softgenia.playlist.model.dto.workout;


import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutResponseDto {
    private Integer id;
    private String name;
    private Integer userId;
    private int totalVideoCount;
    private int totalDurationInMinutes;
    private String formattedTotalDuration;
    private String image;
    private BigDecimal price;
    private Boolean isBlocked;

    public WorkoutResponseDto(Workout workout) {
        this.id = workout.getId();
        this.name = workout.getName();
        this.image = workout.getImage();
        this.price = workout.getPrice();
        this.isBlocked = workout.getIsBlocked();

        if (workout.getUser() != null) {
            this.userId = workout.getUser().getId();
        }

        this.totalVideoCount = workout.getVideos().size();

        int totalSeconds = workout.getVideos().stream()
                .filter(video -> video.getDurationInSeconds() != null)
                .mapToInt(Video::getDurationInSeconds)
                .sum();

        this.totalDurationInMinutes = (int) Math.round(totalSeconds / 60.0);
        this.formattedTotalDuration = formatDuration(totalSeconds);
    }

    private String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds < 0) return "0m 00s";
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%dm %02ds", minutes, seconds);
    }
}
