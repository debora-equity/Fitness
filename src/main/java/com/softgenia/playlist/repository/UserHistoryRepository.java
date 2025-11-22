package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.model.entity.UserHistory;
import com.softgenia.playlist.model.entity.Workout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Integer> {

    // --- RENAME THIS METHOD ---
    // This is the standard and correct way to write this derived query.
    // Spring understands that 'Workout' refers to the 'workout' field in the history entity.
    Optional<UserHistory> findByUserAndWorkoutAndVideo_Id(User user, Workout workout, Integer videoId);

    // A query to find the single, most recently watched video for a user
    @Query("SELECT h FROM UserHistory h WHERE h.user = :user ORDER BY h.lastWatchedAt DESC LIMIT 1")
    Optional<UserHistory> findLastWatchedByUser(@Param("user") User user);

    void deleteByVideoId(Integer videoId);

    void deleteByWorkoutId(Integer workoutId);

    void deleteByUser(User user);
}
