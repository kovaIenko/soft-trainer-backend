package com.backend.softtrainer.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialMetadataDto {
    private Long id;
    private String fileName;
    private String tag;
} 