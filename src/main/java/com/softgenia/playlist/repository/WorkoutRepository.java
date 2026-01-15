package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.model.entity.Workout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WorkoutRepository extends JpaRepository<Workout, Integer> {
    @Query(
            value = """
                        SELECT DISTINCT w
                        FROM Workout w
                        LEFT JOIN FETCH w.user
                        LEFT JOIN FETCH w.workoutVideos wv
                        LEFT JOIN FETCH wv.video
                        WHERE (:name IS NULL OR w.name LIKE CONCAT('%', :name, '%'))
                        ORDER BY w.id ASC 
                    """,
            countQuery = """
                        SELECT COUNT(DISTINCT w)
                        FROM Workout w
                        WHERE (:name IS NULL OR w.name LIKE CONCAT('%', :name, '%'))
                    """
    )
    Page<Workout> findWorkoutsWithDetails(
            @Param("name") String name,
            Pageable pageable
    );


    @Query("SELECT w FROM Workout w LEFT JOIN FETCH w.user LEFT JOIN FETCH w.workoutVideos WHERE w.id = :id")
    Optional<Workout> findByIdWithDetails(Integer id);

    void deleteByUser(User user);


}
