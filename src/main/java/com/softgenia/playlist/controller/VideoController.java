package com.softgenia.playlist.controller;


import com.softgenia.playlist.exception.VideoException;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.video.CreateVideoDto;
import com.softgenia.playlist.model.dto.video.UpdateVideoDto;
import com.softgenia.playlist.model.dto.video.VideoResponseDto;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.repository.VideoRepository;
import com.softgenia.playlist.service.VideoService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoController {
    private final VideoService videoService;

    @GetMapping
    public ResponseEntity<PageResponseDto<VideoResponseDto>> getVideo(
            @RequestParam Integer pageSize,
            @RequestParam Integer pageNumber,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer durationInSeconds
    ){
        var page = videoService.getVideos(description,durationInSeconds,pageNumber,pageSize);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }
// In VideoController.java

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateVideoMetadata(@PathVariable Integer id, @Valid @RequestBody UpdateVideoDto dto) {
        // Ensure the ID in the path matches the ID in the body
        if (!id.equals(dto.getId())) {
            return ResponseEntity.badRequest().build();
        }
        videoService.updateVideoMetadata(dto);
        return ResponseEntity.ok().build();
    }

    // In VideoController.java

    @PostMapping("/{id}/file")
    public ResponseEntity<VideoResponseDto> replaceVideoFile(@PathVariable Integer id, @RequestParam("file") MultipartFile file) {
        try {
            Video updatedVideo = videoService.replaceVideoFile(id, file);
            return ResponseEntity.ok(new VideoResponseDto(updatedVideo));
        } catch (IOException | VideoException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @PutMapping
    public ResponseEntity<Void> updateVideo( @Valid @RequestBody UpdateVideoDto dto){
        videoService.updateVideoMetadata(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<VideoResponseDto> uploadVideo(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") CreateVideoDto metadataDto) {

        try {
            Video newVideo = videoService.uploadVideoAndCreateRecord(file, metadataDto);
            return new ResponseEntity<>(new VideoResponseDto(newVideo), HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
