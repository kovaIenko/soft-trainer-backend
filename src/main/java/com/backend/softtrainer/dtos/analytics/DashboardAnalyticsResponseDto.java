package com.backend.softtrainer.dtos.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAnalyticsResponseDto {
    private List<UsersWithHyperParamsDto> users;
    private boolean success;
    private String message;
} 