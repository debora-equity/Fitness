package com.softgenia.playlist.model.dto.workout;


import com.softgenia.playlist.model.dto.video.VideoResponseDto;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
    private List<VideoResponseDto> videos;
    private String image;

    public WorkoutResponseDto(Workout workout) {
        this.id = workout.getId();
        this.name = workout.getName();
        this.image = workout.getImage();

        if (workout.getUser() != null) {
            this.userId = workout.getUser().getId();
        }

        // --- UPDATE: ADDED SORTING HERE ---
        this.videos = workout.getVideos().stream()
                // Sort by ID (ascending) so the order is always stable
                .sorted(Comparator.comparing(Video::getId))
                .map(VideoResponseDto::new)
                .collect(Collectors.toList());
        // ----------------------------------

        this.totalVideoCount = this.videos.size();

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
