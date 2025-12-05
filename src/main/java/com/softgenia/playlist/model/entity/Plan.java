package com.softgenia.playlist.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "plan")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "price", precision = 7, scale = 2)
    private BigDecimal price;

    @Column(name = "is_paid")
    private Boolean isPaid;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "plan_workouts",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "workout_id")
    )
    private Set<Workout> workouts = new HashSet<>();

    @Column(name = "is_blocked")
    private Boolean isBlocked;

}