package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.AllSimulationsResponseDto;
import com.backend.softtrainer.dtos.AllSkillsResponseDto;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.services.SkillService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.backend.softtrainer.services.auth.AuthUtils.userIsOwnerApp;
import static com.backend.softtrainer.utils.Converter.convertSimulations;
import static com.backend.softtrainer.utils.Converter.convertSkills;

@RestController
@RequestMapping("/skills")
@AllArgsConstructor
@Slf4j
public class SkillController {

  private final SkillService skillService;

  @GetMapping
  @PreAuthorize("@customUsrDetailsService.orgHasEmployee(authentication, #organization)")
  public ResponseEntity<AllSkillsResponseDto> getAllSkillNames(@RequestParam(name = "org") String organization,
                                                               final Authentication authentication) {
    Set<Skill> skills = new HashSet<>();
    if ((Objects.isNull(organization) || organization.isEmpty())) {
      if (userIsOwnerApp(authentication)) {
        skills = skillService.getAllSkill();
      } else {
        ResponseEntity.ok(new AllSkillsResponseDto(
          new HashSet<>(),
          false,
          "The organization should be specified for that user"
        ));
      }
    } else {
      skills = skillService.getAvailableSkill(organization);
    }

    return ResponseEntity.ok(new AllSkillsResponseDto(convertSkills(skills), true, "success"));
  }

  @GetMapping("/simulations")
  @PreAuthorize("@customUsrDetailsService.isSkillAvailable(authentication, #skillId)")
  public ResponseEntity<AllSimulationsResponseDto> getAllSimulations(@RequestParam(name = "skillId") Long skillId,
                                                                     final Authentication authentication) {
    var simulations = skillService.findSimulationsBySkill(skillId);
    return ResponseEntity.ok(new AllSimulationsResponseDto(skillId, convertSimulations(simulations), true, "success"));
  }

}
