package com.softgenia.playlist.model.dto.video;

import com.softgenia.playlist.model.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoResponseDto {
    private Integer id;
    private String name;
    private String description;
    private String url;
    private String thumbnailUrl;
    private Integer durationInSeconds;
    private String durationFormatted;

    public VideoResponseDto(Video entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.url = entity.getUrl();
        this.thumbnailUrl = entity.getThumbnailUrl();
        this.durationInSeconds = entity.getDurationInSeconds();


        if (entity.getDurationInSeconds() != null && entity.getDurationInSeconds() >= 0) {
            int totalSecs = entity.getDurationInSeconds();
            int minutes = totalSecs / 60;
            int seconds = totalSecs % 60;
            // The "%02d" format specifier ensures that the number is padded with a leading zero if it's less than 10
            this.durationFormatted = String.format("%d:%02d", minutes, seconds);
        } else {
            this.durationFormatted = "0:00"; // Default value
        }
    }
}
