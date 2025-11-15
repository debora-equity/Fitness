package com.softgenia.playlist.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "video")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Size(max = 2048)
    @Column(name = "url", length = 2048)
    private String url;


    @Column(name = "duration_in_seconds")
    private Integer durationInSeconds;


    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @Size(max = 2048)
    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @ManyToMany(mappedBy = "videos")
    private Set<Workout> workouts = new HashSet<>();

    @Size(max = 100)
    @Column(name = "name", length = 100)
    private String name;

}