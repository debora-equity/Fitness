package com.softgenia.playlist.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "video")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(length = 2048)
    private String url;

    private Integer durationInSeconds;

    @Column(length = 500)
    private String description;

    @Column(length = 2048)
    private String thumbnailUrl;

    @Column(length = 100)
    private String name;

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<WorkoutVideo> workoutVideos = new HashSet<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startTimeSeconds ASC")
    private List<VideoChapter> chapters = new ArrayList<>();
}
