package com.softgenia.playlist.controller;


import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.video.VideoMinResponse;
import com.softgenia.playlist.model.entity.PdfVideo;
import com.softgenia.playlist.service.PdfVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class PdfVideoController {

    private final PdfVideoService pdfVideoService;

    @GetMapping("/videoPdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_CREATOR')")
    public ResponseEntity<PageResponseDto<VideoMinResponse>> getVideoPdf(
            @RequestParam Integer pageSize,
            @RequestParam Integer pageNumber,
            @RequestParam(required = false) String name
    ){
        var page = pdfVideoService.getVideoPdf(name,pageNumber,pageSize);
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @PostMapping(value = "/pdf", consumes = {"multipart/form-data"})
    @PreAuthorize("hasAnyRole('ADMIN', 'CONTENT_CREATOR')")
    public ResponseEntity<VideoMinResponse> uploadVideoPdf(
            @RequestPart("file") MultipartFile file,
            @RequestPart("name") String name) {

        try {
            PdfVideo newVideo = pdfVideoService.uploadVideoPdf(file, name);
            return new ResponseEntity<>(new VideoMinResponse(newVideo), HttpStatus.CREATED);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/pdf/{id}")
    public ResponseEntity<Void> deleteVideosPdf(@PathVariable Integer id)  {
        pdfVideoService.deleteVideosPdf(id);
        return  new ResponseEntity<>(HttpStatus.OK);
    }
}
