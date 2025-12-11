package com.softgenia.playlist.model.dto.userHistory;

import com.softgenia.playlist.model.dto.video.VideoResponseDto;
import com.softgenia.playlist.model.dto.workout.WorkoutMinDto;
import com.softgenia.playlist.model.entity.UserHistory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContinueWatchingDto {
    private VideoResponseDto video;
    private Integer watchedSeconds;
    private String formattedWatchedTime;
    private WorkoutMinDto workout;
    private Integer currentVideoIndex;
    private Integer totalVideos;
    private Boolean isUnlocked;

    public ContinueWatchingDto(UserHistory history, int index, int total, Boolean hasAccess) {
        this.video = new VideoResponseDto(history.getVideo());
        this.workout = new WorkoutMinDto(history.getWorkout(), hasAccess);
        this.watchedSeconds = history.getWatchedSeconds();
        this.formattedWatchedTime = formatDuration(history.getWatchedSeconds());
        this.currentVideoIndex = index;
        this.totalVideos = total;

        this.isUnlocked = hasAccess;
    }

    private String formatDuration(Integer totalSeconds) {
        if (totalSeconds == null || totalSeconds < 0) return "0:00";
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
