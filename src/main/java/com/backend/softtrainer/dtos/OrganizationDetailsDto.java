package com.backend.softtrainer.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDetailsDto {
    private Long id;
    private String avatar;
    private String name;
    private String localization;
    private Set<String> availableSkills;
    private List<String> employees;
} 