package com.softgenia.playlist.model.dto.workout;

import com.softgenia.playlist.model.dto.video.VideoResponseDto;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutMinDto {
    private Integer id;
    private String name;
    private int totalVideoCount;
    private int totalDurationInMinutes;
    private String formattedTotalDuration;
    private List<VideoResponseDto> videos;
    private String image;

    public WorkoutMinDto(Workout workout){
        this.id = workout.getId();
        this.name = workout.getName();
        this.image = workout.getImage();
        this.videos = workout.getVideos().stream()
                .map(VideoResponseDto::new)
                .collect(Collectors.toList());

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
