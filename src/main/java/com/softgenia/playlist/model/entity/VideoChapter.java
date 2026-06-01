package com.softgenia.playlist.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "video_chapters")
public class VideoChapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @Size(max = 255)
    @Column(name = "title")
    private String title;

    @Column(name = "start_time_seconds")
    private Integer startTimeSeconds;

    @Column(name = "end_time_seconds")
    private Integer endTimeSeconds;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @Size(max = 100)
    @Column(name = "description", length = 100)
    private String description;

}