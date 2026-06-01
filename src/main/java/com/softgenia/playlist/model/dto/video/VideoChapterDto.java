package com.softgenia.playlist.model.dto.video;

import com.softgenia.playlist.model.entity.VideoChapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoChapterDto {
    private String title;
    private Integer startTimeSeconds;
    private Integer endTimeSeconds;
    private String description;


    public VideoChapterDto(VideoChapter chapter) {
        this.title = chapter.getTitle();
        this.startTimeSeconds = chapter.getStartTimeSeconds();
        this.endTimeSeconds = chapter.getEndTimeSeconds();
        this.description = chapter.getDescription();
    }
}
