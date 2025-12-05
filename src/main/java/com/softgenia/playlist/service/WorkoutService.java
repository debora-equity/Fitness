package com.softgenia.playlist.service;


import com.softgenia.playlist.exception.VideoException;
import com.softgenia.playlist.exception.WorkoutException;
import com.softgenia.playlist.model.constants.Roles;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.video.CreateVideoDto;
import com.softgenia.playlist.model.dto.video.UpdateVideoDto;
import com.softgenia.playlist.model.dto.workout.WorkoutMinDto;
import com.softgenia.playlist.model.entity.User;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.repository.UserHistoryRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.repository.UserSubscriptionRepository;
import com.softgenia.playlist.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkoutService {
    private final WorkoutRepository repository;
    private final UserRepository userRepository;
    private final UserHistoryRepository userHistoryRepository;
    private final VideoService videoService;
    private final FileStorageService fileStorageService;
    private final UserSubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public PageResponseDto<WorkoutMinDto> getWorkouts(String name, Integer pageNumber, Integer pageSize) {


        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Integer userId = user.getId();


        boolean isAdminOrCreator = user.getRole().getName() == Roles.ROLE_ADMIN ||
                user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR;


        Set<Integer> accessibleWorkoutIds = new HashSet<>();

        if (!isAdminOrCreator) {
            LocalDateTime now = LocalDateTime.now();
            accessibleWorkoutIds.addAll(subscriptionRepository.findActiveDirectWorkoutIds(userId, now));
            accessibleWorkoutIds.addAll(subscriptionRepository.findActivePlanWorkoutIds(userId, now));
        }

        var pageable = PageRequest.of(pageNumber, pageSize);
        var page = repository.findWorkoutsWithDetails(name, pageable); // Use your optimized fetch query

        List<WorkoutMinDto> mappedData = page.stream().map(workout -> {
            boolean hasAccess = false;

            if (isAdminOrCreator) {
                hasAccess = true;
            } else if (workout.getUser().getId().equals(userId)) {
                hasAccess = true;
            } else if (accessibleWorkoutIds.contains(workout.getId())) {
                hasAccess = true;
            }

            return new WorkoutMinDto(workout, hasAccess);

        }).toList();

        return new PageResponseDto<WorkoutMinDto>().ofPage(page, mappedData);
    }

    @Transactional(readOnly = true)
    public WorkoutMinDto getWorkoutById(Integer workoutId, String username) throws AccessDeniedException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Integer userId = user.getId();

        Workout workout = repository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout not found"));
        boolean hasAccess = user.getRole().getName() == Roles.ROLE_ADMIN || user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR;

        if (!hasAccess) {
            int directCount = subscriptionRepository.countDirectAccess(userId, workoutId, LocalDateTime.now());
            int planCount = subscriptionRepository.countPlanAccess(userId, workoutId, LocalDateTime.now());

            if (directCount > 0 || planCount > 0) {
                hasAccess = true;
            }
        }
        int directCount = subscriptionRepository.countDirectAccess(userId, workoutId, LocalDateTime.now());
        System.out.println("Direct Subscription Count: " + directCount);

        int planCount = subscriptionRepository.countPlanAccess(userId, workoutId, LocalDateTime.now());
        System.out.println("Plan Subscription Count: " + planCount);

        if (directCount > 0 || planCount > 0) {
            hasAccess = true;
        }

        if (!hasAccess) {
            throw new AccessDeniedException("You need to pay.");
        }
        return new WorkoutMinDto(workout, hasAccess);
    }

    @Transactional
    public Workout createWorkoutWithFiles(
            String name,
            BigDecimal price,
            Boolean isBlocked,
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
        workout.setPrice(price);
        workout.setUser(currentUser);
        workout.setIsBlocked(false);


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
            Boolean isBlocked,
            BigDecimal price,
            MultipartFile imageFile,
            List<MultipartFile> videoFiles,
            List<UpdateVideoDto> videoMetadataList) throws IOException, VideoException {

        Workout workout = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        workout.setName(name);
        workout.setPrice(price);
        workout.setIsBlocked(isBlocked);

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
        Workout workout = repository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout not found with id: " + workoutId));


        Video newVideo = videoService.uploadVideoAndCreateRecord(file, metadataDto);


        workout.getVideos().add(newVideo);


        repository.save(workout);

        return newVideo;
    }

    @Transactional
    public void deleteWorkoutAndVideos(Integer id) throws WorkoutException {

        Workout workout = repository.findById(id).orElseThrow(WorkoutException::new);

        userHistoryRepository.deleteByWorkoutId(id);

        Set<Video> videosToDelete = new HashSet<>(workout.getVideos());


        repository.delete(workout);
        repository.flush();

        for (Video video : videosToDelete) {
            try {
                videoService.deleteVideo(video.getId());
            } catch (Exception e) {
                System.err.println("Failed to delete orphaned video " + video.getId() + ": " + e.getMessage());
            }
        }
    }
}
