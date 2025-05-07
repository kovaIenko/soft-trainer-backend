package com.backend.softtrainer.dtos.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsersWithHyperParamsDto {
    private String email;
    private String department;
    private String name;
    private String avatar;
    private List<UserHyperParamDto> hyperParams;
    private Double totalScore;
    private Integer completedSimulations;
    private Integer totalAttempts;
    private Double averageHeartsLost;
}
