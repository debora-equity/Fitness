package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.Workout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout,Integer> {
    // --- FIX FOR getWorkouts (List View) ---
    // This query fetches the workout, its user, AND its videos all in one go.
    // We use LEFT JOIN FETCH to ensure we still get workouts even if they have no videos.
    // The "DISTINCT" keyword is important to prevent duplicate workouts in the result.
    @Query("SELECT DISTINCT w FROM Workout w LEFT JOIN FETCH w.user LEFT JOIN FETCH w.videos " +
            "WHERE (:name IS NULL OR w.name LIKE CONCAT('%', :name, '%'))")
    Page<Workout> findWorkoutsWithDetails(String name, Pageable pageable);

    // --- FIX FOR getWorkoutById (Detail View) ---
    // A similar query to fetch all details for a single workout by its ID.
    @Query("SELECT w FROM Workout w LEFT JOIN FETCH w.user LEFT JOIN FETCH w.videos WHERE w.id = :id")
    Optional<Workout> findByIdWithDetails(Integer id);


}
