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

    Optional<UserHistory> findByUserAndWorkoutAndVideo_Id(User user, Workout workout, Integer videoId);

    @Query("SELECT h FROM UserHistory h WHERE h.user = :user ORDER BY h.lastWatchedAt DESC LIMIT 1")
    Optional<UserHistory> findLastWatchedByUser(@Param("user") User user);

    void deleteByVideoId(Integer videoId);

    void deleteByWorkoutId(Integer workoutId);

    void deleteByUser(User user);
}
