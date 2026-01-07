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

        long contentLength = video.contentLength();

        HttpRange range = requestHeaders.getRange().isEmpty()
                ? null
                : requestHeaders.getRange().get(0);

        long start = 0;
        if (range != null) {
            start = range.getRangeStart(contentLength);
        }

        long rangeLength = Math.min(CHUNK_SIZE, contentLength - start);

        ResourceRegion region = new ResourceRegion(video, start, rangeLength);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.valueOf("video/mp4"));
        responseHeaders.setCacheControl(CacheControl.noStore());
        responseHeaders.add(HttpHeaders.ACCEPT_RANGES, "bytes");

        return ResponseEntity
                .status(range == null ? HttpStatus.OK : HttpStatus.PARTIAL_CONTENT)
                .headers(responseHeaders)
                .body(region);
    }
}
