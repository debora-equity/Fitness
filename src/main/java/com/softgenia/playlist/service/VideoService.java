package com.softgenia.playlist.service;


import com.softgenia.playlist.exception.VideoException;
import com.softgenia.playlist.model.dto.PageResponseDto;
import com.softgenia.playlist.model.dto.video.CreateVideoDto;
import com.softgenia.playlist.model.dto.video.UpdateVideoDto;
import com.softgenia.playlist.model.dto.video.VideoMinResponse;
import com.softgenia.playlist.model.dto.video.VideoResponseDto;
import com.softgenia.playlist.model.entity.Video;
import com.softgenia.playlist.model.entity.Workout;
import com.softgenia.playlist.repository.UserHistoryRepository;
import com.softgenia.playlist.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository repository;
    private final FileStorageService fileStorageService;
    private final UserHistoryRepository userHistoryRepository;


    public PageResponseDto<VideoResponseDto> getVideos(String description, Integer durationInSeconds, Integer pageNumber, Integer pageSize) {
        var pageable = PageRequest.of(pageNumber, pageSize);
        var page = repository.getVideos(description, durationInSeconds, pageable);
        List<VideoResponseDto> mappedData = page.stream().map(VideoResponseDto::new).toList();
        return new PageResponseDto<VideoResponseDto>().ofPage(page, mappedData);
    }

    public Video findById(Integer id) throws VideoException {
        return repository.findById(id).orElseThrow(VideoException::new);
    }

    public PageResponseDto<VideoMinResponse> getVideoPdf(String name,Integer pageNumber,Integer pageSize){
        var pageable = PageRequest.of(pageNumber, pageSize);
        var page = repository.getVideoPdf(name, pageable);
        List<VideoMinResponse> mappedData = page.stream().map(VideoMinResponse::new).toList();
        return new PageResponseDto<VideoMinResponse>().ofPage(page, mappedData);
    }

    @Transactional
    public Video uploadVideoAndCreateRecord(MultipartFile file, CreateVideoDto metadataDto) throws IOException {

        String videoUrl = fileStorageService.saveFile(file);

        String thumbnailUrl = fileStorageService.generateThumbnailFromVideo(videoUrl);


        int duration = fileStorageService.getVideoDurationInSeconds(videoUrl);


        Video video = new Video();
        video.setName(metadataDto.getName());
        video.setDescription(metadataDto.getDescription());


        video.setDurationInSeconds(duration);

        video.setUrl(videoUrl);
        video.setThumbnailUrl(thumbnailUrl);

        return repository.save(video);
    }
    @Transactional
    public Video uploadVideoPdf(MultipartFile file, String name) throws IOException {

        Video video = new Video();
        video.setName(name);
        return repository.save(video);
    }


    @Transactional
    public void updateVideoMetadata(UpdateVideoDto dto) {
        Video video = repository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Video not found: " + dto.getId()));


        if (dto.getName() != null) {
            video.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            video.setDescription(dto.getDescription());
        }
        if (dto.getDurationInSeconds() != null) {
            video.setDurationInSeconds(dto.getDurationInSeconds());
        }

        repository.save(video);
    }

    @Transactional
    public Video replaceVideoFile(Integer videoId, MultipartFile file) throws IOException, VideoException {
        Video video = findById(videoId);

        String oldVideoUrl = video.getUrl();
        String oldThumbnailUrl = video.getThumbnailUrl();

        String newVideoUrl = fileStorageService.saveFile(file);
        String newThumbnailUrl = fileStorageService.generateThumbnailFromVideo(newVideoUrl);

        video.setUrl(newVideoUrl);
        video.setThumbnailUrl(newThumbnailUrl);
        Video updatedVideo = repository.save(video);
        fileStorageService.deleteFile(oldVideoUrl);
        fileStorageService.deleteFile(oldThumbnailUrl);

        return updatedVideo;
    }

    @Transactional
    public void deleteVideo(Integer id) throws VideoException {

        Video video = findById(id);
        String videoUrl = video.getUrl();
        String thumbnailUrl = video.getThumbnailUrl();

        userHistoryRepository.deleteByVideoId(id);


        repository.delete(video);

        fileStorageService.deleteFile(videoUrl);
        fileStorageService.deleteFile(thumbnailUrl);
    }

    @Transactional
    public void deleteVideos(Integer id) throws VideoException {
        Video video = findById(id);
        String videoUrl = video.getUrl();
        String thumbnailUrl = video.getThumbnailUrl();

        userHistoryRepository.deleteByVideoId(id);

        for (Workout workout : new HashSet<>(video.getWorkouts())) {
            workout.getVideos().remove(video);
        }

        repository.delete(video);

        fileStorageService.deleteFile(videoUrl);
        fileStorageService.deleteFile(thumbnailUrl);
    }

}
