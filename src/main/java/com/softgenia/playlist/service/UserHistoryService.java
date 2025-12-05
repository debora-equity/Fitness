package com.softgenia.playlist.service;

import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.dto.userHistory.ContinueWatchingDto;
import com.softgenia.playlist.model.dto.userHistory.UpdateVideoProgressDto;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.model.entity.UserHistory;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private final UserHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final WorkoutRepository workoutRepository;
    private final UserSubscriptionRepository subscriptionRepository;

    @Transactional
    public void updateVideoProgress(String username, UpdateVideoProgressDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Workout workout = workoutRepository.findById(dto.getWorkoutId())
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        UserHistory history = historyRepository.findByUserAndWorkoutAndVideo_Id(user, workout, dto.getVideoId())
                .orElseGet(() -> {
                    Video video = videoRepository.findById(dto.getVideoId())
                            .orElseThrow(() -> new RuntimeException("Video not found"));


                    UserHistory newHistory = new UserHistory();
                    newHistory.setUser(user);
                    newHistory.setWorkout(workout);
                    newHistory.setVideo(video);
                    return newHistory;
                });

        history.setWatchedSeconds(dto.getWatchedSeconds());
        history.setLastWatchedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    @Transactional
    public ContinueWatchingDto getLastWatchedVideo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        UserHistory history = historyRepository.findLastWatchedByUser(user)
                .orElse(null);

        if (history == null) {
            return null;
        }
        boolean isUnlocked = checkUserAccess(user, history.getWorkout());

        List<Video> sortedVideos = history.getWorkout().getVideos().stream()
                .sorted(Comparator.comparing(Video::getId))
                .collect(Collectors.toList());

        int totalVideos = sortedVideos.size();
        int currentIndex = -1;

        for (int i = 0; i < totalVideos; i++) {
            if (sortedVideos.get(i).getId().equals(history.getVideo().getId())) {
                currentIndex = i + 1;
                break;
            }
        }

        return new ContinueWatchingDto(history, currentIndex, totalVideos, isUnlocked);
    }

    private boolean checkUserAccess(User user, Workout workout) {
        if (user.getRole().getName() == Roles.ROLE_ADMIN ||
                user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR) {
            return true;
        }

        if (!Boolean.TRUE.equals(workout.getIsPaid())) {
            return true;
        }

        int count = subscriptionRepository.countActiveAccessToWorkout(
                user.getId(),
                workout.getId(),
                LocalDateTime.now()
        );

        return count > 0;
    }
}
