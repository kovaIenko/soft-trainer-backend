package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.Organization;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.flow.FlowNode;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.SkillRepository;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SkillService {

  private final SkillRepository skillRepository;

  private final OrganizationRepository organizationRepository;

  private final UserRepository userRepository;

  public Set<Skill> getAvailableSkillByOrg(final String organization) {

    var optOrg = organizationRepository.getFirstByName(organization);
    return optOrg.map(Organization::getAvailableSkills)
      .orElse(Collections.emptySet());
  }

  public Set<Skill> getAvailableSkill(final String username) {

    var userOpt = userRepository.findByEmail(username);
    if (userOpt.isPresent()) {
      var optOrg = Optional.ofNullable(userOpt.get().getOrganization());
      return optOrg.map(Organization::getAvailableSkills)
        .orElse(Collections.emptySet());
    } else {
      var errorMessage = String.format("The user %s doesn't exist", username);
      log.error(errorMessage);
      return new HashSet<>();
    }
  }

  public Set<Skill> getAllSkill() {
    return new HashSet<>(skillRepository.findAll());
  }

  public Set<Simulation> findSimulationsBySkill(final Long skillId) {
    var optSkill = skillRepository.findById(skillId);
    if (optSkill.isPresent()) {
      return optSkill.get().getSimulations().keySet();
    }
    throw new NoSuchElementException(String.format("There is no skills with id %s", skillId));
  }

}
