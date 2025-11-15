package com.softgenia.playlist.model.dto.workout;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateWorkoutDto {
    @Size(max = 100, message = "name cannot be more than 100 characters")
    private String name;
}
