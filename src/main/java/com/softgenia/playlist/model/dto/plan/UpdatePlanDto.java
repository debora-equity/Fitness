package com.softgenia.playlist.model.dto.plan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePlanDto {
    private Integer id;
    private String name;
    private BigDecimal price;
    private Boolean isPaid;
    private List<Integer> workoutIds;
    private Boolean isBlocked;
}
