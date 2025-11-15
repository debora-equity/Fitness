package com.softgenia.playlist.controller;

import aj.org.objectweb.asm.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softgenia.playlist.exception.TrainerException;
import com.softgenia.playlist.exception.VideoException;
import com.softgenia.playlist.exception.WorkoutException;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.video.CreateVideoDto;
import com.softgenia.playlist.model.dto.video.UpdateVideoDto;
import com.softgenia.playlist.model.dto.workout.CreateWorkoutDto;
import com.softgenia.playlist.model.dto.workout.UpdateWorkoutDto;
import com.softgenia.playlist.model.dto.workout.WorkoutMinDto;
import com.softgenia.playlist.model.dto.workout.WorkoutResponseDto;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/workout")
public class WorkoutController {
        private final WorkoutService workoutService;
        private final ObjectMapper objectMapper;

        @GetMapping
        public ResponseEntity<PageResponseDto<WorkoutMinDto>> getWorkouts(
                @RequestParam Integer pageSize,
                @RequestParam Integer pageNumber,
                @RequestParam(required = false) String name
        ){
            var page = workoutService.getWorkouts(name,pageNumber,pageSize);
            return new ResponseEntity<>(page, HttpStatus.OK);
        }

        @GetMapping("/{id}")
        public ResponseEntity<WorkoutResponseDto> getWorkout(@PathVariable Integer id) {
            WorkoutResponseDto response = workoutService.getWorkoutById(id);
            return ResponseEntity.ok(response);
        }
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<WorkoutResponseDto> createWorkout(
            @RequestPart("name") String name,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "videoFiles", required = false) List<MultipartFile> videoFiles,
            @RequestPart(value = "videoMetadata", required = false) List<CreateVideoDto> videoMetadataList,
            Authentication authentication) {

        try {
            String username = authentication.getName();
            Workout newWorkout = workoutService.createWorkoutWithFiles(name, imageFile, videoFiles, videoMetadataList, username);
            return new ResponseEntity<>(new WorkoutResponseDto(newWorkout), HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<WorkoutResponseDto> updateWorkout(
            @PathVariable Integer id, // <-- The ID comes from the URL
            @RequestPart("name") String name,
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestPart(value = "videoFiles", required = false) List<MultipartFile> videoFiles,
            @RequestPart(value = "videoMetadata", required = false) List<UpdateVideoDto> videoMetadataList) {

        try {
            // Pass the 'id' from the path directly to the service
            Workout updatedWorkout = workoutService.updateWorkoutWithFiles(
                    id, name, imageFile, videoFiles, videoMetadataList
            );
            return ResponseEntity.ok(new WorkoutResponseDto(updatedWorkout));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkout(@PathVariable Integer id) {
        try {
            workoutService.deleteWorkoutAndVideos(id);
            return ResponseEntity.noContent().build();
        } catch (WorkoutException e) {
            return ResponseEntity.notFound().build();
        }
    }

}
