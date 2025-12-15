package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.entity.PdfVideo;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.repository.PdfVideoRepository;
import com.softgenia.playlist.repository.VideoRepository;
import com.softgenia.playlist.security.JwtTokenProvider;
import com.softgenia.playlist.service.StreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.nio.file.Paths;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;
    private final VideoRepository videoRepository;
    private final PdfVideoRepository pdfVideoRepository;
    private final JwtTokenProvider tokenProvider;

    @GetMapping("/video/{videoId}")
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable Integer videoId,
            @RequestParam("token") String token,
            @RequestHeader HttpHeaders headers) {

        if (token == null || !tokenProvider.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));


            String filename = Paths.get(video.getUrl()).getFileName().toString();

            return streamingService.getVideoRegion(filename, headers);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/pdf-video/{pdfVideoId}")
    public ResponseEntity<ResourceRegion> streamPdfVideo(
            @PathVariable Integer pdfVideoId,
            @RequestHeader HttpHeaders headers) {

        try {
            PdfVideo pdfVideo = pdfVideoRepository.findById(pdfVideoId)
                    .orElseThrow(() -> new RuntimeException("PdfVideo not found"));

            if (pdfVideo.getUrl() == null) {
                return ResponseEntity.notFound().build();
            }

            String filename = Paths.get(pdfVideo.getUrl()).getFileName().toString();

            return streamingService.getVideoRegion(filename, headers);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}