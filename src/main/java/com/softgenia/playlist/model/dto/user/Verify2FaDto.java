package com.softgenia.playlist.model.dto.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Verify2FaDto {
    private String username;
    private String code;
}
