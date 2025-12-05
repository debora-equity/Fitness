package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Integer> {

    Optional<UserSubscription> findByUserAndWorkout(User user, Workout workout);

    Optional<UserSubscription> findByUserAndPlan(User user, Plan plan);

    @Query("""
                SELECT CASE WHEN COUNT(s) > 0 THEN TRUE ELSE FALSE END
                FROM UserSubscription s
                WHERE s.user = :user
                  AND s.document.id = :documentId
                  AND s.expiryDate > :now
            """)
    boolean hasActiveAccessToDocument(User user, Integer documentId, LocalDateTime now);

    @Query(value = """
                SELECT COUNT(*) 
                FROM user_subscriptions 
                WHERE user_id = :userId 
                  AND workout_id = :workoutId 
                  AND expiry_date > :now
            """, nativeQuery = true)
    int countDirectAccess(Integer userId, Integer workoutId, LocalDateTime now);

    @Query(value = """
                SELECT COUNT(*)
                FROM user_subscriptions s
                JOIN plan_workouts pw ON s.plan_id = pw.plan_id
                WHERE s.user_id = :userId
                  AND pw.workout_id = :workoutId
                  AND s.expiry_date > :now
            """, nativeQuery = true)
    int countPlanAccess(Integer userId, Integer workoutId, LocalDateTime now);

    Optional<UserSubscription> findByUserAndDocument(User user, SharedDocument document);

    @Query("SELECT COUNT(s) > 0 FROM UserSubscription s " +
            "WHERE s.user.id = :userId AND s.plan.id = :planId AND s.expiryDate > :now")
    boolean hasActiveSubscriptionForPlan(Integer userId, Integer planId, LocalDateTime now);

    @Query("SELECT s.workout.id FROM UserSubscription s " +
            "WHERE s.user.id = :userId AND s.workout IS NOT NULL AND s.expiryDate > :now")
    List<Integer> findActiveDirectWorkoutIds(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Query("SELECT pw.id FROM UserSubscription s " +
            "JOIN s.plan p JOIN p.workouts pw " +
            "WHERE s.user.id = :userId AND s.expiryDate > :now")
    List<Integer> findActivePlanWorkoutIds(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Query("SELECT s.plan.id FROM UserSubscription s " +
            "WHERE s.user.id = :userId AND s.plan IS NOT NULL AND s.expiryDate > :now")
    List<Integer> findActivePlanIds(@Param("userId") Integer userId, @Param("now") LocalDateTime now);

    @Query(value = """
                SELECT COUNT(*) 
                FROM user_subscriptions 
                WHERE user_id = :userId 
                  AND document_id = :documentId 
                  AND expiry_date > :now
            """, nativeQuery = true)
    int countDocumentAccess(Integer userId, Integer documentId, LocalDateTime now);

    @Query(value = "SELECT document_id FROM user_subscriptions " +
            "WHERE user_id = :userId " +
            "AND document_id IS NOT NULL " +
            "AND expiry_date > :now",
            nativeQuery = true)
    List<Integer> findActiveDocumentIds(@Param("userId") Integer userId,
                                        @Param("now") LocalDateTime now);

    @Query(value = """
                SELECT COUNT(*)
                FROM user_subscriptions s
                LEFT JOIN plan_workouts pw ON s.plan_id = pw.plan_id
                WHERE s.user_id = :userId
                  AND s.expiry_date > :now
                  AND (
                       s.workout_id = :workoutId
                       OR
                       pw.workout_id = :workoutId
                  )
            """, nativeQuery = true)
    int countActiveAccessToWorkout(@Param("userId") Integer userId,
                                   @Param("workoutId") Integer workoutId,
                                   @Param("now") LocalDateTime now);
}