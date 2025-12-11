package com.softgenia.playlist.model.dto.video;

import com.softgenia.playlist.model.entity.PdfVideo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoMinResponse {
    private Integer id;
    private String name;

    public VideoMinResponse(PdfVideo video){
        this.id = video.getId();
        this.name = video.getName();
    }
}
