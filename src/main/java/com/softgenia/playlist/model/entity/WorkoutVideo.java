package com.softgenia.playlist.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "workout_video")
public class WorkoutVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workout_id")
    private Workout workout;

    @Column(name = "position_index")
    private Integer position;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkoutVideo that)) return false;
        return Objects.equals(workout.getId(), that.workout.getId()) &&
                Objects.equals(video.getId(), that.video.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(workout.getId(), video.getId());
    }

    public WorkoutVideo(Workout workout, Video video, int position) {
        this.workout = workout;
        this.video = video;
        this.position = position;
    }

    public WorkoutVideo() {

    }
}