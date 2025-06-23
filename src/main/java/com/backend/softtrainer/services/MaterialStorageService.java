package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.NewSkillPayload;
import com.backend.softtrainer.entities.Material;
import com.backend.softtrainer.entities.Skill;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MaterialStorageService {

    public List<Material> storeMaterials(NewSkillPayload payload, Skill skill) {
        if (CollectionUtils.isEmpty(payload.getMaterials())) {
            return Collections.emptyList();
        }

        return payload.getMaterials().stream()
                .map(materialDto -> {
                    byte[] decodedContent = Base64.getDecoder().decode(materialDto.getFileContent());
                    return Material.builder()
                            .fileName(materialDto.getFileName())
                            .tag(materialDto.getTag())
                            .fileContent(decodedContent)
                            .skill(skill)
                            .build();
                })
                .collect(Collectors.toList());
    }
} 