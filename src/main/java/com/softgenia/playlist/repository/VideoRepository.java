package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface VideoRepository extends JpaRepository<Video,Integer> {
    @Query("select v from Video  v" +
            " where :description is null or v.description like :description" +
            " and :durationInSeconds is null or v.durationInSeconds = :durationInSeconds" )
    Page<Video> getVideos(String description,Integer durationInSeconds,Pageable pageable);
}
