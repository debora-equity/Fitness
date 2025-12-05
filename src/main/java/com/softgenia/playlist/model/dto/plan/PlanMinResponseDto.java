package com.softgenia.playlist.model.dto.plan;

import com.softgenia.playlist.model.dto.workout.WorkoutResponseDto;
import com.softgenia.playlist.model.entity.Plan;
import com.softgenia.playlist.model.entity.Workout;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlanMinResponseDto {

    private Integer id;
    private String name;
    private BigDecimal price;
    private Boolean isPaid;
    private Integer totalWorkouts;
    private List<WorkoutResponseDto> workouts;
    private Boolean isUnlocked;
    private Boolean isBlocked;

    public PlanMinResponseDto(Plan plan, Boolean hasAccess) {
        this.id = plan.getId();
        this.name = plan.getName();
        this.price = plan.getPrice();
        this.isPaid = plan.getIsPaid();
        this.isBlocked = plan.getIsBlocked();
        this.isUnlocked = hasAccess;
        this.totalWorkouts = plan.getWorkouts().size();
        this.workouts = plan.getWorkouts().stream()
                .sorted(Comparator.comparing(Workout::getId))
                .map(WorkoutResponseDto::new)
                .collect(Collectors.toList());
    }
}
