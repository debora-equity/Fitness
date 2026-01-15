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
import com.softgenia.playlist.model.entity.WorkoutVideo;
import com.softgenia.playlist.repository.UserHistoryRepository;
import com.softgenia.playlist.repository.UserRepository;
import com.softgenia.playlist.repository.UserSubscriptionRepository;
import com.softgenia.playlist.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        var page = repository.findWorkoutsWithDetails(name, pageable);

        List<WorkoutMinDto> mappedData = page.stream().map(workout -> {
            boolean hasAccess = false;

            if (isAdminOrCreator) {
                hasAccess = true;
            } else if (Boolean.TRUE.equals(workout.getIsFree())) {
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

        boolean hasAccess = false;

        if (user.getRole().getName() == Roles.ROLE_ADMIN || user.getRole().getName() == Roles.ROLE_CONTENT_CREATOR) {
            hasAccess = true;
        } else if (Boolean.TRUE.equals(workout.getIsFree())) {
            hasAccess = true;
        } else if (workout.getUser().getId().equals(userId)) {
            hasAccess = true;
        } else {
            int directCount = subscriptionRepository.countDirectAccess(userId, workoutId, LocalDateTime.now());
            int planCount = subscriptionRepository.countPlanAccess(userId, workoutId, LocalDateTime.now());
            if (directCount > 0 || planCount > 0) {
                hasAccess = true;
            }
        }

        if (!hasAccess) {
            throw new AccessDeniedException("You need an active subscription to view this workout.");
        }
        return new WorkoutMinDto(workout, hasAccess);
    }

    @Transactional
    public Workout createWorkoutWithFiles(
            String name,
            BigDecimal price,
            Boolean isBlocked,
            Boolean isFree,
            MultipartFile imageFile,
            List<MultipartFile> videoFiles,
            List<CreateVideoDto> metadataList,
            String username
    ) throws IOException, InterruptedException {

        if (videoFiles != null && metadataList != null &&
                videoFiles.size() != metadataList.size()) {
            throw new IllegalArgumentException("Files and metadata size mismatch");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Workout workout = new Workout();
        workout.setName(name);
        workout.setPrice(price);
        workout.setIsBlocked(Boolean.TRUE.equals(isBlocked));
        workout.setIsFree(Boolean.TRUE.equals(isFree));
        workout.setUser(user);

        if (imageFile != null && !imageFile.isEmpty()) {
            workout.setImage(fileStorageService.saveImage(imageFile));
        }

        repository.save(workout);

        if (videoFiles != null && !videoFiles.isEmpty()) {
            addInitialVideos(workout, videoFiles, metadataList);
        }

        return repository.save(workout);
    }

    @Transactional
    public Workout updateWorkoutWithFiles(
            Integer workoutId,
            String name,
            Boolean isBlocked,
            Boolean isFree,
            BigDecimal price,
            MultipartFile imageFile,
            List<MultipartFile> newVideoFiles,
            List<UpdateVideoDto> metadataList
    ) throws IOException, InterruptedException, VideoException {

        Workout workout = repository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        workout.setName(name);
        workout.setPrice(price);
        workout.setIsBlocked(Boolean.TRUE.equals(isBlocked));
        workout.setIsFree(Boolean.TRUE.equals(isFree));

        if (imageFile != null && !imageFile.isEmpty()) {
            String oldImage = workout.getImage();
            workout.setImage(fileStorageService.saveImage(imageFile));
            if (oldImage != null) fileStorageService.deleteFile(oldImage);
        }

        Map<Integer, WorkoutVideo> existingLinks =
                workout.getWorkoutVideos().stream()
                        .filter(wv -> wv.getVideo() != null)
                        .collect(Collectors.toMap(
                                wv -> wv.getVideo().getId(),
                                wv -> wv
                        ));

        List<WorkoutVideo> updatedLinks = new ArrayList<>();
        int newFileIndex = 0;

        for (UpdateVideoDto dto : metadataList) {

            if (dto.getId() != null && existingLinks.containsKey(dto.getId())) {

                WorkoutVideo link = existingLinks.get(dto.getId());

                videoService.updateVideoMetadata(dto);
                link.setPosition(dto.getPosition());

                updatedLinks.add(link);
                existingLinks.remove(dto.getId());

            } else {
                if (newVideoFiles == null || newFileIndex >= newVideoFiles.size()) {
                    throw new IllegalArgumentException(
                            "Missing video file for new video: " + dto.getName()
                    );
                }

                MultipartFile file = newVideoFiles.get(newFileIndex++);
                CreateVideoDto createDto =
                        new CreateVideoDto(dto.getName(), dto.getDescription(), null);

                Video newVideo = videoService.uploadVideoAndCreateRecord(file, createDto);

                WorkoutVideo link = new WorkoutVideo(
                        workout,
                        newVideo,
                        dto.getPosition()
                );

                updatedLinks.add(link);
            }
        }

        for (WorkoutVideo removed : existingLinks.values()) {
            workout.getWorkoutVideos().remove(removed);
            videoService.deleteVideo(removed.getVideo().getId());
        }

        workout.getWorkoutVideos().clear();
        workout.getWorkoutVideos().addAll(updatedLinks);

        normalizePositionsIfMissing(workout.getWorkoutVideos());

        return repository.save(workout);
    }
    @Transactional
    public List<Video> addVideosToWorkout(
            Integer workoutId,
            List<MultipartFile> files,
            List<CreateVideoDto> metadataList
    ) throws IOException, InterruptedException {

        Workout workout = repository.findById(workoutId)
                .orElseThrow(() -> new RuntimeException("Workout not found"));

        List<Video> addedVideos = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            Video video = videoService.uploadVideoAndCreateRecord(files.get(i), metadataList.get(i));

            WorkoutVideo link = new WorkoutVideo(
                    workout,
                    video,
                    workout.getWorkoutVideos().size() + 1
            );

            workout.getWorkoutVideos().add(link);
            addedVideos.add(video);
        }

        repository.save(workout);
        return addedVideos;
    }

    private void addInitialVideos(
            Workout workout,
            List<MultipartFile> files,
            List<CreateVideoDto> metadata
    ) throws IOException, InterruptedException {

        if (files == null || metadata == null || files.size() != metadata.size()) {
            throw new IllegalArgumentException("Files and metadata must be non-null and of same size");
        }

        List<WorkoutVideo> initialVideos = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            Video video = videoService.uploadVideoAndCreateRecord(files.get(i), metadata.get(i));
            if (video == null) {
                throw new RuntimeException("Video upload failed for file: " + files.get(i).getOriginalFilename());
            }
            workout.getWorkoutVideos().add(new WorkoutVideo(workout, video, i + 1));

        }


        workout.getWorkoutVideos().addAll(initialVideos);

        normalizePositionsInPlace(workout.getWorkoutVideos());
    }


    private void normalizePositionsIfMissing(Collection<WorkoutVideo> links) {

        boolean needsFix = links.stream()
                .anyMatch(wv -> wv.getPosition() == null || wv.getPosition() <= 0);

        if (!needsFix) return;

        List<WorkoutVideo> ordered = new ArrayList<>(links);
        ordered.sort(Comparator.comparing(
                wv -> Optional.ofNullable(wv.getPosition()).orElse(Integer.MAX_VALUE)
        ));

        int pos = 1;
        for (WorkoutVideo wv : ordered) {
            wv.setPosition(pos++);
        }
    }

    private void normalizePositionsInPlace(Collection<WorkoutVideo> links) {

        List<WorkoutVideo> ordered = new ArrayList<>(links);

        ordered.sort(Comparator.comparing(
                wv -> Optional.ofNullable(wv.getPosition()).orElse(Integer.MAX_VALUE)
        ));

        int pos = 1;
        for (WorkoutVideo wv : ordered) {
            wv.setPosition(pos++);
        }
    }

    @Transactional
    public void deleteWorkoutAndVideos(Integer id) throws WorkoutException, VideoException {

        Workout workout = repository.findById(id)
                .orElseThrow(WorkoutException::new);

        Set<Video> videos = workout.getWorkoutVideos().stream()
                .map(WorkoutVideo::getVideo)
                .collect(Collectors.toSet());

        userHistoryRepository.deleteByWorkoutId(id);
        repository.delete(workout);
        repository.flush();

        for (Video video : videos) {
            videoService.deleteVideo(video.getId());
        }
    }
}