package com.softgenia.playlist.controller;

import com.softgenia.playlist.model.entity.PdfVideo;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.repository.PdfVideoRepository;
import com.softgenia.playlist.repository.VideoRepository;
import com.softgenia.playlist.security.JwtTokenProvider;
import com.softgenia.playlist.service.FileStorageService;
import com.softgenia.playlist.service.StreamingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
public class StreamingController {

    private final StreamingService streamingService;
    private final VideoRepository videoRepository;
    private final PdfVideoRepository pdfVideoRepository;
    private final JwtTokenProvider tokenProvider;
    private final FileStorageService fileStorageService;

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

    @GetMapping("/hls/{videoId}/{filename}")
    public ResponseEntity<Resource> streamHls(
            @PathVariable Integer videoId,
            @PathVariable String filename,
            @RequestParam(value = "token", required = false) String token,
            jakarta.servlet.http.HttpServletRequest request) {
        if (filename.endsWith(".m3u8")) {
            if (token == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null || !tokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {

            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new RuntimeException("Video not found"));

            Path storedPath = Paths.get(video.getUrl());
            Path folderPath = storedPath.getParent();

            String resourcePath = folderPath.resolve(filename).toString();
            Resource resource = fileStorageService.loadAsResource(resourcePath);

            String contentType = "application/octet-stream";
            if (filename.endsWith(".m3u8"))
                contentType = "application/vnd.apple.mpegurl";
            if (filename.endsWith(".ts"))
                contentType = "video/MP2T";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
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

    @GetMapping("/image/**")
    public ResponseEntity<Resource> getImage(HttpServletRequest request) {

        String path = request.getRequestURI()
                .replaceFirst("/media/image/", "");

        Resource resource = fileStorageService.loadAsResource(path);

        String contentType = "application/octet-stream";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            contentType = MediaType.IMAGE_JPEG_VALUE;
        else if (path.endsWith(".png"))
            contentType = MediaType.IMAGE_PNG_VALUE;
        else if (path.endsWith(".webp"))
            contentType = "image/webp";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/pdf-video/hls/{pdfVideoId}/{filename}")
    public ResponseEntity<Resource> streamPdfVideoHls(
            @PathVariable Integer pdfVideoId,
            @PathVariable String filename,
            @RequestParam(value = "token", required = false) String token,
            jakarta.servlet.http.HttpServletRequest request) {

        if (filename.endsWith(".m3u8")) {
            if (token == null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }

            if (token == null || !tokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        try {
            PdfVideo pdfVideo = pdfVideoRepository.findById(pdfVideoId)
                    .orElseThrow(() -> new RuntimeException("PdfVideo not found"));

            Path storedPath = Paths.get(pdfVideo.getUrl());
            Path folderPath = storedPath.getParent();

            String resourcePath = folderPath.resolve(filename).toString();
            Resource resource = fileStorageService.loadAsResource(resourcePath);

            String contentType = "application/octet-stream";
            if (filename.endsWith(".m3u8"))
                contentType = "application/vnd.apple.mpegurl";
            if (filename.endsWith(".ts"))
                contentType = "video/MP2T";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}