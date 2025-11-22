package com.softgenia.playlist.service;


import com.softgenia.playlist.exception.VideoException;
import com.softgenia.playlist.exception.WorkoutException;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.video.CreateVideoDto;
import com.softgenia.playlist.model.dto.video.UpdateVideoDto;
import com.softgenia.playlist.model.dto.workout.CreateWorkoutDto;
import com.softgenia.playlist.model.dto.workout.UpdateWorkoutDto;
import com.softgenia.playlist.model.dto.workout.WorkoutMinDto;
import com.softgenia.playlist.model.dto.workout.WorkoutResponseDto;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.repository.UserHistoryRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.repository.VideoRepository;
import com.softgenia.playlist.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {
    private final WorkoutRepository repository;
    private final UserRepository userRepository;
    private final UserHistoryRepository userHistoryRepository;
    private final VideoService videoService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PageResponseDto<WorkoutMinDto> getWorkouts(String name, Integer pageNumber, Integer pageSize) {
        var pageable = PageRequest.of(pageNumber, pageSize);
        var page = repository.findWorkoutsWithDetails(name, pageable);
        List<WorkoutMinDto> mappedData = page.stream().map(WorkoutMinDto::new).toList();
        return new PageResponseDto<WorkoutMinDto>().ofPage(page, mappedData);
    }

    @Transactional(readOnly = true)
    public WorkoutResponseDto getWorkoutById(Integer id) {
        Workout workout = repository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        return new WorkoutResponseDto(workout);
    }

    @Transactional
    public Workout createWorkoutWithFiles(
            String name,
            MultipartFile imageFile,
            List<MultipartFile> videoFiles,
            List<CreateVideoDto> videoMetadataList,
            String username) throws IOException {

        if (videoFiles != null && videoMetadataList != null && videoFiles.size() != videoMetadataList.size()) {
            throw new IllegalArgumentException("The number of video files must match the number of metadata entries.");
        }

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Workout workout = new Workout();
        workout.setName(name);
        workout.setUser(currentUser);


        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = fileStorageService.saveFile(imageFile);
            workout.setImage(imageUrl);
        }

        Set<Video> newVideos = new HashSet<>();
        if (videoFiles != null && !videoFiles.isEmpty()) {
            for (int i = 0; i < videoFiles.size(); i++) {
                Video newVideo = videoService.uploadVideoAndCreateRecord(videoFiles.get(i), videoMetadataList.get(i));
                newVideos.add(newVideo);
            }
        }
        workout.setVideos(newVideos);

        return repository.save(workout);
    }

    @Transactional
    public Workout updateWorkoutWithFiles(
            Integer id,
            String name,
            MultipartFile imageFile,
            List<MultipartFile> videoFiles,
            List<UpdateVideoDto> videoMetadataList) throws IOException, VideoException {

        Workout workout = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        workout.setName(name);

        if (imageFile != null && !imageFile.isEmpty()) {
            String oldImage = workout.getImage();
            String newImage = fileStorageService.saveFile(imageFile);
            workout.setImage(newImage);

            if (oldImage != null) fileStorageService.deleteFile(oldImage);
        }


        Map<Integer, Video> existingVideos =
                workout.getVideos().stream()
                        .collect(Collectors.toMap(Video::getId, v -> v));


        for (int i = 0; i < videoMetadataList.size(); i++) {

            UpdateVideoDto metadata = videoMetadataList.get(i);
            MultipartFile newFile =
                    (videoFiles != null && videoFiles.size() > i)
                            ? videoFiles.get(i)
                            : null;

            Integer videoId = metadata.getId();

            if (!existingVideos.containsKey(videoId)) {
                throw new RuntimeException("Cannot update: video " + videoId + " does not belong to workout");
            }

            videoService.updateVideoMetadata(metadata);

            if (newFile != null && !newFile.isEmpty()) {
                videoService.replaceVideoFile(videoId, newFile);
            }
        }
        return repository.save(workout);
    }

    @Transactional
    public Video addVideoToWorkout(Integer workoutId, MultipartFile file, CreateVideoDto metadataDto) throws IOException {
        // 1. Find the workout playlist that we are adding a video to
        Workout workout = repository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout not found with id: " + workoutId));

        // 2. Delegate the entire video creation process to the VideoService.
        // This includes saving the file, generating a thumbnail, getting the duration,
        // and creating the Video record in the database.
        Video newVideo = videoService.uploadVideoAndCreateRecord(file, metadataDto);

        // 3. Add the newly created and saved video to the workout's list of videos.
        workout.getVideos().add(newVideo);

        // 4. Save the workout. JPA will automatically update the workout_video join table.
        repository.save(workout);

        // 5. Return the newly created video object.
        return newVideo;
    }

    @Transactional
    public void deleteWorkoutAndVideos(Integer id) throws WorkoutException {

        Workout workout = repository.findById(id).orElseThrow(WorkoutException::new);

        userHistoryRepository.deleteByWorkoutId(id);

        Set<Video> videosToDelete = new HashSet<>(workout.getVideos());


        repository.delete(workout);

        for (Video video : videosToDelete) {
            try {
                videoService.deleteVideo(video.getId());
            } catch (Exception e) {
                System.err.println("Failed to delete orphaned video " + video.getId() + ": " + e.getMessage());
            }
        }
    }
    }
