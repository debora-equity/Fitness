package com.softgenia.playlist.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResponseDto<T> {
    private Long totalElements;
    private Integer totalPages;
    private List<T> elements;

    public PageResponseDto<T> ofPage(Page<?> page, List<T> elements) {
        this.elements = elements;
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        return this;
    }

    public PageResponseDto<T> ofPage(Page<T> page) {
        this.elements = page.get().toList();
        this.totalPages = page.getTotalPages();
        this.totalElements = page.getTotalElements();
        return this;
    }
}
