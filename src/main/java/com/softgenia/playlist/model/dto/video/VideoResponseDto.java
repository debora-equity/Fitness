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
    private String streamUrl;
    private String thumbnailUrl;
    private Integer durationInSeconds;
    private String durationFormatted;
    private Integer position;

    public VideoResponseDto(Video entity) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.description = entity.getDescription();
        this.thumbnailUrl = entity.getThumbnailUrl();
        this.durationInSeconds = entity.getDurationInSeconds();
        this.position = entity.getWorkoutVideos().stream()
                .map(wv -> wv.getPosition())
                .findFirst()
                .orElse(null);


        if (entity.getId() != null) {
            if (entity.getUrl() != null && entity.getUrl().contains("master.m3u8")) {
                this.streamUrl = "/api/stream/hls/" + entity.getId() + "/master.m3u8";
            } else {
                this.streamUrl = "/api/stream/video/" + entity.getId();
            }
        }
        if (entity.getDurationInSeconds() != null && entity.getDurationInSeconds() >= 0) {
            int totalSecs = entity.getDurationInSeconds();
            int minutes = totalSecs / 60;
            int seconds = totalSecs % 60;
            this.durationFormatted = String.format("%d:%02d", minutes, seconds);
        } else {
            this.durationFormatted = "0:00";
        }
    }
}
