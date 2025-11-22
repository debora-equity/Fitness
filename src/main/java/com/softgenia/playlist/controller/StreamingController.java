package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.repository.VideoRepository;
import com.softgenia.playlist.service.StreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.nio.file.Paths;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;
    private final VideoRepository videoRepository;

    @GetMapping("/video/{videoId}")
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable Integer videoId,
            @RequestHeader HttpHeaders headers) {

        try {
            // 1. Find the video record in the database
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            // 2. Get just the filename from the full URL path
            String filename = Paths.get(video.getUrl()).getFileName().toString();

            // 3. Delegate to the streaming service to handle byte-range requests
            return streamingService.getVideoRegion(filename, headers);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}