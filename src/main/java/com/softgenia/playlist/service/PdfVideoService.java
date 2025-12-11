package com.softgenia.playlist.service;

import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.video.VideoMinResponse;
import com.softgenia.playlist.model.entity.PdfVideo;
import com.softgenia.playlist.repository.PdfVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
@Service
@RequiredArgsConstructor
public class PdfVideoService {
    private final PdfVideoRepository repository;
    private final FileStorageService fileStorageService;

    public PageResponseDto<VideoMinResponse> getVideoPdf(String name, Integer pageNumber, Integer pageSize){
        var pageable = PageRequest.of(pageNumber, pageSize);
        var page = repository.getVideoPdf(name, pageable);
        List<VideoMinResponse> mappedData = page.stream().map(VideoMinResponse::new).toList();
        return new PageResponseDto<VideoMinResponse>().ofPage(page, mappedData);
    }

    @Transactional
    public PdfVideo uploadVideoPdf(MultipartFile file, String name) throws IOException {

        String videoUrl = fileStorageService.saveFile(file);

        PdfVideo pdfVideo = new PdfVideo();
        pdfVideo.setName(name);

        pdfVideo.setUrl(videoUrl);

        return repository.save(pdfVideo);
    }
    @Transactional
    public void deleteVideosPdf(Integer id) {
        PdfVideo video = repository.findById(id).orElseThrow();
        if (video.getUrl() != null) {
            fileStorageService.deleteFile(video.getUrl());
        }
        repository.delete(video);
    }

}
