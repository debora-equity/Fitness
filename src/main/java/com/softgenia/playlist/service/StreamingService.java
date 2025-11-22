package com.softgenia.playlist.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Service
public class StreamingService {

    @Value("${upload.path}")
    private String uploadPath;

    public ResponseEntity<ResourceRegion> getVideoRegion(String filename, HttpHeaders headers) throws IOException {
        Path videoFilePath = Paths.get(uploadPath).resolve(filename).normalize();
        UrlResource video = new UrlResource(videoFilePath.toUri());
        long contentLength = video.contentLength();

        HttpRange range = headers.getRange().stream().findFirst().orElse(null);

        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1_000_000L, end - start + 1);

            ResourceRegion region = new ResourceRegion(video, start, rangeLength);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
                    .contentType(MediaType.valueOf("video/mp4"))
                    .eTag(filename)
                    .body(region);
        } else {

            ResourceRegion region = new ResourceRegion(video, 0, Math.min(1_000_000L, contentLength));

            return ResponseEntity.status(HttpStatus.OK)
                    .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES))
                    .contentType(MediaType.valueOf("video/mp4"))
                    .eTag(filename)
                    .body(region);
        }
    }
}