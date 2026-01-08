package com.softgenia.playlist.model.dto.workout;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateWorkoutDto {
    @Size(max = 100, message = "name cannot be more than 100 characters")
    private String name;
    private Integer userId;
    private List<Integer> videoId;
    private Boolean isBlocked;
    private Boolean isFree;
}
