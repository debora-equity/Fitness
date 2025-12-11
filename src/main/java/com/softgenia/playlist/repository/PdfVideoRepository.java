package com.softgenia.playlist.repository;

import com.softgenia.playlist.model.entity.PdfVideo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PdfVideoRepository extends JpaRepository<PdfVideo,Integer> {

    @Query("""
           SELECT v FROM PdfVideo v
           WHERE (:name IS NULL OR v.name LIKE %:name%)
           """)
    Page<PdfVideo> getVideoPdf(String name, Pageable pageable);
}
