package com.softgenia.playlist.model.dto.video;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateVideoDto {
    private String name;
    private String description;
}
