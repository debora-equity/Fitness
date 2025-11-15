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


    @ManyToMany
    @JoinTable(name = "workout_video",
            joinColumns = @JoinColumn(name = "workout_id"),
            inverseJoinColumns = @JoinColumn(name = "video_id"))
    private Set<Video> videos = new HashSet<>();

    @Size(max = 2048)
    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Size(max = 2048)
    @Column(name = "image", length = 2048)
    private String image;

}
