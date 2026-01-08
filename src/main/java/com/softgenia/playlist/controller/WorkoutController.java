package com.softgenia.playlist.controller;


import com.softgenia.playlist.exception.WorkoutException;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.video.CreateVideoDto;
import com.softgenia.playlist.model.dto.video.UpdateVideoDto;
import com.softgenia.playlist.model.dto.video.VideoResponseDto;
import com.softgenia.playlist.model.dto.workout.WorkoutMinDto;
import com.softgenia.playlist.model.dto.workout.WorkoutResponseDto;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/workout")
public class WorkoutController {
    private final WorkoutService workoutService;

    @GetMapping
    public ResponseEntity<PageResponseDto<WorkoutMinDto>> getWorkouts(
            @RequestParam Integer pageSize,
            @RequestParam Integer pageNumber,
            @RequestParam(required = false) String name
    ) {
        var page = workoutService.getWorkouts(name, pageNumber, pageSize);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public WorkoutMinDto getWorkout(@PathVariable Integer id,
                                    Authentication authentication) throws AccessDeniedException {

        return workoutService.getWorkoutById(id, authentication.getName());
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<WorkoutResponseDto> createWorkout(
            @RequestPart("name") String name,
            @RequestParam("price") BigDecimal price,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "videoFiles", required = false) List<MultipartFile> videoFiles,
            @RequestPart(value = "videoMetadata", required = false) List<CreateVideoDto> videoMetadataList,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            Workout newWorkout = workoutService.createWorkoutWithFiles(name, price, isBlocked,isFree, imageFile, videoFiles, videoMetadataList, username);
            return new ResponseEntity<>(new WorkoutResponseDto(newWorkout), HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<WorkoutResponseDto> updateWorkout(
            @PathVariable Integer id,
            @RequestPart("name") String name,
            @RequestParam(value = "isBlocked",required = false) Boolean isBlocked,
            @RequestParam(required = false) Boolean isFree,
            @RequestParam("price") BigDecimal price,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "videoFiles", required = false) List<MultipartFile> videoFiles,
            @RequestPart(value = "videoMetadata", required = false) List<UpdateVideoDto> videoMetadataList) {

        try {
            Workout updatedWorkout = workoutService.updateWorkoutWithFiles(
                    id, name, isBlocked,isFree, price, imageFile, videoFiles, videoMetadataList
            );
            return ResponseEntity.ok(new WorkoutResponseDto(updatedWorkout));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/{workoutId}/videos", consumes = {"multipart/form-data"})
    public ResponseEntity<VideoResponseDto> addVideoToWorkout(
            @PathVariable Integer workoutId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") CreateVideoDto metadataDto) {

        try {
            Video newVideo = workoutService.addVideoToWorkout(workoutId, file, metadataDto);
            return new ResponseEntity<>(new VideoResponseDto(newVideo), HttpStatus.CREATED);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteWorkout(@PathVariable Integer id) {
        try {
            workoutService.deleteWorkoutAndVideos(id);
            return ResponseEntity.noContent().build();

        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cannot delete this workout because it has been purchased by users or belongs to a Plan."));

        } catch (WorkoutException e) {
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
