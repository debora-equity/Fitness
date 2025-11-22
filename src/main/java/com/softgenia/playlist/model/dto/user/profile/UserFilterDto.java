package com.softgenia.playlist.model.dto.user.profile;

import com.softgenia.playlist.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFilterDto {
    private String search;
    private String name;
    private String surname;

    public void formatData(){
        this.search = StringUtils.getFormattedStringFilter(this.getSearch());
        this.name = StringUtils.getFormattedStringFilter(this.getName());
        this.surname = StringUtils.getFormattedStringFilter(this.getSurname());
    }
    }
