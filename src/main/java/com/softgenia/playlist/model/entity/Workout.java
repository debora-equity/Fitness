package com.softgenia.playlist.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "workout")
public class Workout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 100)
    @Column(name = "name", length = 100)
    private String name;


    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


    @OneToMany(mappedBy = "workout", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<WorkoutVideo> workoutVideos = new ArrayList<>();

    public void addVideo(Video video, int position) {
        WorkoutVideo link = new WorkoutVideo();
        link.setWorkout(this);
        link.setVideo(video);
        link.setPosition(position);

        this.workoutVideos.add(link);
    }

    @Size(max = 2048)
    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Size(max = 2048)
    @Column(name = "image", length = 2048)
    private String image;

    @Column(name = "price", precision = 7, scale = 2)
    private BigDecimal price;

    @Column(name = "is_paid")
    private Boolean isPaid;

    @ManyToMany(mappedBy = "workouts")
    private Set<Plan> plans = new HashSet<>();

    @Column(name = "is_blocked")
    private Boolean isBlocked;

    @Column(name = "is_free")
    private Boolean isFree;

}
