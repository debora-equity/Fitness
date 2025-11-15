package com.softgenia.playlist.model.dto.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateVideoDto {
    private Integer id;
    private String name;
    private String description;
    private Integer durationInSeconds;
    private String url;
    private String thumbnailUrl;
}
