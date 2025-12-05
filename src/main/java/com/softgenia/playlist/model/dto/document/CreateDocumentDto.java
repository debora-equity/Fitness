package com.softgenia.playlist.model.dto.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateDocumentDto {
    private BigDecimal price;
    private Boolean isBlocked;
}
