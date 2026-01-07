package com.softgenia.playlist.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StreamingService {

    @Value("${upload.path}")
    private String uploadPath;

    private static final long CHUNK_SIZE = 10 * 1024 * 1024;

    public ResponseEntity<ResourceRegion> getVideoRegion(
            String filename,
            HttpHeaders requestHeaders
    ) throws IOException {

        Path path = Paths.get(uploadPath).resolve(filename).normalize();
        UrlResource video = new UrlResource(path.toUri());

        if (!video.exists() || !video.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        long contentLength = video.contentLength();

        HttpRange range = requestHeaders.getRange().isEmpty()
                ? null
                : requestHeaders.getRange().get(0);

        ResourceRegion region;
        if (range != null) {

            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(CHUNK_SIZE, end - start + 1);
            region = new ResourceRegion(video, start, rangeLength);

        } else {
            long rangeLength = Math.min(CHUNK_SIZE, contentLength);
            region = new ResourceRegion(video, 0, rangeLength);
        }



        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)

                .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.valueOf("video/mp4")))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")

                .body(region);
    }
}
