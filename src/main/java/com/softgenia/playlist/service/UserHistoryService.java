package com.softgenia.playlist.service;

import com.softgenia.playlist.model.dto.userHistory.ContinueWatchingDto;
import com.softgenia.playlist.model.dto.userHistory.UpdateVideoProgressDto;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.model.entity.UserHistory;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.repository.UserHistoryRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.repository.VideoRepository;
import com.softgenia.playlist.repository.WorkoutRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserHistoryService {

    private final UserHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final WorkoutRepository workoutRepository;

    // --- METHOD TO UPDATE PROGRESS ---
    @Transactional
    public void updateVideoProgress(String username, UpdateVideoProgressDto dto) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Workout workout = workoutRepository.findById(dto.getWorkoutId())
                .orElseThrow(() -> new RuntimeException("Workout not found"));
        // Use the 'historyRepository' variable here
        UserHistory history = historyRepository.findByUserAndWorkoutAndVideo_Id(user, workout,dto.getVideoId())
                .orElseGet(() -> {
                    Video video = videoRepository.findById(dto.getVideoId())
                            .orElseThrow(() -> new RuntimeException("Video not found"));

                    // Make sure your entity class is named UserVideoHistory
                    UserHistory newHistory = new UserHistory();
                    newHistory.setUser(user);
                    newHistory.setWorkout(workout);
                    newHistory.setVideo(video);
                    return newHistory;
                });

        history.setWatchedSeconds(dto.getWatchedSeconds());
        history.setLastWatchedAt(LocalDateTime.now());

        // Use the 'historyRepository' variable to save the 'history' object
        historyRepository.save(history);
    }

    // --- METHOD TO GET THE "CONTINUE WATCHING" VIDEO ---
    @Transactional
    public ContinueWatchingDto getLastWatchedVideo(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Use the custom repository method to find the most recent history record
        return historyRepository.findLastWatchedByUser(user)
                .map(ContinueWatchingDto::new) // If found, convert it to the DTO
                .orElse(null); // If the user has no watch history, return null
    }
}
