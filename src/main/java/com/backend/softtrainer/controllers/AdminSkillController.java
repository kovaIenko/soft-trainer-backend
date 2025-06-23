package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ApiResponseDto;
import com.backend.softtrainer.dtos.NewSkillPayload;
import com.backend.softtrainer.dtos.SkillDetailDto;
import com.backend.softtrainer.dtos.SkillSummaryDto;
import com.backend.softtrainer.dtos.UpdateSkillVisibilityDto;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.services.SkillService;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import static com.backend.softtrainer.services.auth.AuthUtils.userIsOwnerApp;

@RestController
@RequestMapping("/api/admin/skills")
@AllArgsConstructor
@Slf4j
public class AdminSkillController {

    private final SkillService skillService;
    private final CustomUsrDetailsService customUsrDetailsService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    public ResponseEntity<ApiResponseDto<SkillDetailDto>> createSkill(@Valid @RequestBody NewSkillPayload payload, Authentication authentication) {
        var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());

        // All skills must be associated with the organization of the user who creates them
        // Both owners and admins create skills for their organization
        Skill createdSkill = skillService.createSkillForOrganization(payload, userDetails.user().getOrganization());

        ApiResponseDto<SkillDetailDto> response = ApiResponseDto.<SkillDetailDto>builder()
                .success(true)
                .message("Skill created successfully")
                .data(toSkillDetailDto(createdSkill))
                .build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    public ResponseEntity<ApiResponseDto<List<SkillSummaryDto>>> getSkills(Authentication authentication) {
        var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());
        boolean isOwner = userIsOwnerApp(authentication);

        var skills = isOwner ?
            skillService.getAllSkill() :
            skillService.getSkillsByOrganization(userDetails.user().getOrganization().getName());
        var converted = skills.stream()
                .map(skill -> new SkillSummaryDto(skill.getId(), skill.getName(), skill.getDescription()))
                .collect(Collectors.toList());
        ApiResponseDto<List<SkillSummaryDto>> response = ApiResponseDto.<List<SkillSummaryDto>>builder()
                .success(true)
                .data(converted)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    public ResponseEntity<ApiResponseDto<SkillDetailDto>> getSkillById(@PathVariable Long id, Authentication authentication) {
        // Check if user can access this skill
        if (!customUsrDetailsService.canAccessSkill(authentication, id)) {
            ApiResponseDto<SkillDetailDto> response = ApiResponseDto.<SkillDetailDto>builder()
                    .success(false)
                    .message("You don't have access to this skill")
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            Skill skill = skillService.getSkillById(id);
            ApiResponseDto<SkillDetailDto> response = ApiResponseDto.<SkillDetailDto>builder()
                    .success(true)
                    .data(toSkillDetailDto(skill))
                    .build();
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            ApiResponseDto<SkillDetailDto> response = ApiResponseDto.<SkillDetailDto>builder()
                    .success(false)
                    .message("Skill not found")
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    public ResponseEntity<ApiResponseDto<Void>> updateSkillVisibility(@PathVariable Long id, @RequestBody UpdateSkillVisibilityDto payload, Authentication authentication) {
        // Check if user can access this skill
        if (!customUsrDetailsService.canAccessSkill(authentication, id)) {
            ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                    .success(false)
                    .message("You don't have access to this skill")
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        skillService.updateSkillVisibility(id, payload.isHidden());
        ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                .success(true)
                .message("Skill visibility updated successfully")
                .build();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    public ResponseEntity<ApiResponseDto<Void>> deleteSkill(@PathVariable Long id, Authentication authentication) {
        // Check if user can access this skill
        if (!customUsrDetailsService.canAccessSkill(authentication, id)) {
            ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                    .success(false)
                    .message("You don't have access to this skill")
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            skillService.deleteSkill(id);
            ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                .success(true)
                .message("Skill archived successfully")
                .build();
            return ResponseEntity.ok(response);
        } catch (UnsupportedOperationException e) {
            ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (EntityNotFoundException e) {
            ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    public ResponseEntity<ApiResponseDto<Void>> restoreSkill(@PathVariable Long id, Authentication authentication) {
        // Check if user can access this skill
        if (!customUsrDetailsService.canAccessSkill(authentication, id)) {
            ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                    .success(false)
                    .message("You don't have access to this skill")
                    .build();
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        try {
            skillService.restoreSkill(id);
            ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                    .success(true)
                    .message("Skill restored successfully")
                    .build();
            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            ApiResponseDto<Void> response = ApiResponseDto.<Void>builder()
                    .success(false)
                    .message(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/archived")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OWNER')")
    public ResponseEntity<ApiResponseDto<List<SkillSummaryDto>>> getArchivedSkills(Authentication authentication) {
        var userDetails = (CustomUsrDetails) customUsrDetailsService.loadUserByUsername(authentication.getName());
        boolean isOwner = userIsOwnerApp(authentication);

        var archivedSkills = isOwner ?
            skillService.getArchivedSkills() :
            skillService.getArchivedSkillsByOrganization(userDetails.user().getOrganization().getName());
        var converted = archivedSkills.stream()
                .map(skill -> new SkillSummaryDto(skill.getId(), skill.getName(), skill.getDescription()))
                .collect(Collectors.toList());
        ApiResponseDto<List<SkillSummaryDto>> response = ApiResponseDto.<List<SkillSummaryDto>>builder()
                .success(true)
                .message("Archived skills retrieved successfully")
                .data(converted)
                .build();
        return ResponseEntity.ok(response);
    }

    private SkillDetailDto toSkillDetailDto(Skill skill) {
        // Get material metadata without loading file content to avoid LOB transaction issues
        var materialMetadata = skillService.getMaterialMetadataBySkillId(skill.getId());

        // Get simulations summary for this skill
        var simulations = skillService.getSimulationsBySkillId(skill.getId());

        return SkillDetailDto.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .type(skill.getType())
                .behavior(skill.getBehavior())
                .simulationCount(skill.getSimulationCount())
                .simulations(simulations)
                .materials(materialMetadata.stream()
                        .map(m -> SkillDetailDto.MaterialDetailDto.builder()
                                .fileName(m.getFileName())
                                .tag(m.getTag())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
