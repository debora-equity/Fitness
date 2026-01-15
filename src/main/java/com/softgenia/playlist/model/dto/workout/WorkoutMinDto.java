package com.softgenia.playlist.model.dto.workout;

import com.softgenia.playlist.model.dto.video.VideoResponseDto;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.model.entity.WorkoutVideo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
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
    private BigDecimal price;
    private Boolean isPaid;
    private Boolean isUnlocked;
    private Boolean isBlocked;
    private Boolean isFree;

    public WorkoutMinDto(Workout workout, Boolean hasAccess) {
        this.id = workout.getId();
        this.name = workout.getName();
        this.image = workout.getImage();
        this.price = workout.getPrice();
        this.isPaid = workout.getIsPaid();
        this.isBlocked = workout.getIsBlocked();
        this.isFree = workout.getIsFree();
        this.isUnlocked = hasAccess;


        this.videos = workout.getWorkoutVideos().stream()
                .filter(wv -> wv.getVideo() != null)
                .sorted(Comparator.comparing(WorkoutVideo::getPosition))
                .map(wv -> new VideoResponseDto(wv.getVideo()))
                .collect(Collectors.toList());



        this.totalVideoCount = workout.getWorkoutVideos().size();

        int totalSeconds = workout.getWorkoutVideos().stream()
                .map(wv -> wv.getVideo())
                .filter(v -> v.getDurationInSeconds() != null)
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
