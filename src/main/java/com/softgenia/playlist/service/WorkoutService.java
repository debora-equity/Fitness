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
    private final VideoRepository videoRepository;
    private final VideoService videoService;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public PageResponseDto<WorkoutMinDto> getWorkouts(String name, Integer pageNumber, Integer pageSize) {
        var pageable = PageRequest.of(pageNumber, pageSize);
        var page = repository.findWorkoutsWithDetails(name, pageable);

        // This mapping will now work perfectly and efficiently, with no extra queries.
        List<WorkoutMinDto> mappedData = page.stream().map(WorkoutMinDto::new).toList();
        return new PageResponseDto<WorkoutMinDto>().ofPage(page, mappedData);
    }

    @Transactional(readOnly = true)
    public WorkoutResponseDto getWorkoutById(Integer id) {
        // --- USE THE NEW, EFFICIENT METHOD ---
        Workout workout = repository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        // This mapping will also work perfectly.
        return new WorkoutResponseDto(workout);
    }

    @Transactional
    public Workout createWorkoutWithFiles(
            String name,
            MultipartFile imageFile,
            List<MultipartFile> videoFiles,
            List<CreateVideoDto> videoMetadataList,
            String username) throws IOException {

        // Validation remains the same...
        if (videoFiles != null && videoMetadataList != null && videoFiles.size() != videoMetadataList.size()) {
            throw new IllegalArgumentException("The number of video files must match the number of metadata entries.");
        }

        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Workout workout = new Workout();
        workout.setName(name);
        workout.setUser(currentUser);

        // The rest of the logic for handling the image and video files remains exactly the same...
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

        // -------- Update image if provided --------
        if (imageFile != null && !imageFile.isEmpty()) {
            String oldImage = workout.getImage();
            String newImage = fileStorageService.saveFile(imageFile);
            workout.setImage(newImage);

            if (oldImage != null) fileStorageService.deleteFile(oldImage);
        }

        // Map existing videos by ID
        Map<Integer, Video> existingVideos =
                workout.getVideos().stream()
                        .collect(Collectors.toMap(Video::getId, v -> v));

        // -------- Update each video --------
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

            // 1. Update metadata
            videoService.updateVideoMetadata(metadata);

            // 2. Replace file only if a new file is uploaded
            if (newFile != null && !newFile.isEmpty()) {
                videoService.replaceVideoFile(videoId, newFile);
            }
        }

        // NO delete, NO create

        return repository.save(workout);
    }

    @Transactional
    public void deleteWorkoutAndVideos(Integer id) throws WorkoutException {

        Workout workout = repository.findById(id).orElseThrow(WorkoutException::new);
        Set<Video> videosToDelete = new HashSet<>(workout.getVideos());
        repository.delete(workout);

        for (Video video : videosToDelete) {
            try {
                videoService.deleteVideo(video.getId());
            } catch (Exception e) {

                System.err.println("Failed to delete video with id " + video.getId() + ": " + e.getMessage());
            }
        }
    }
    }
