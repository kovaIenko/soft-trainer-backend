package com.backend.softtrainer.dtos.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponseDto {
    private String type;
    private Object data;
} 