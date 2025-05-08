package com.backend.softtrainer.services.auth;

import com.backend.softtrainer.dtos.StaticRole;
import com.backend.softtrainer.entities.Auth;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.enums.AuthType;
import com.backend.softtrainer.entities.enums.AuthWay;
import com.backend.softtrainer.entities.enums.PlatformType;
import com.backend.softtrainer.exceptions.UserAlreadyExitsException;
import com.backend.softtrainer.repositories.AuthRepository;
import com.backend.softtrainer.repositories.ChatRepository;
import com.backend.softtrainer.repositories.OrganizationRepository;
import com.backend.softtrainer.repositories.RoleRepository;
import com.backend.softtrainer.repositories.SimulationRepository;
import com.backend.softtrainer.repositories.SkillRepository;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Data
@Slf4j
public class CustomUsrDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final SkillRepository skillRepository;
  private final SimulationRepository simulationRepository;
  private PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final AuthRepository authRepository;

  private final ChatRepository chatRepository;
  private final OrganizationRepository organizationRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByEmailWithRoles(username)
      .orElseThrow(() -> new UsernameNotFoundException("User with username = " + username + " not exist!"));
    return new CustomUsrDetails(user);
  }

  public void createAuthRecord(final User user) {
    var login = Auth.builder()
      .platform(PlatformType.WEB)
      .user(user)
      .authType(AuthType.LOGIN)
      .authWay(AuthWay.BASIC)
      .build();
    authRepository.save(login);
  }

  public void createUser(String email, String password) throws UserAlreadyExitsException {
    var userByEmail = userRepository.findByEmail(email);
    if (userByEmail.isPresent()) {
      throw new UserAlreadyExitsException(String.format("The user with email %s already exists", email));
    }
    var optLowestRole = roleRepository.findByName(StaticRole.ROLE_USER);

    var optOrg = organizationRepository.findById(1L);

    if (optLowestRole.isPresent()) {
      User user = new User();
      user.setUsername(email);
      user.setPassword(passwordEncoder.encode(password));
      user.setEmail(email);

      optOrg.ifPresent(user::setOrganization);

      var roles = new HashSet<Role>();
      roles.add(optLowestRole.get());
      user.setRoles(roles);

      var userEnt = userRepository.save(user);

      var signUp = Auth.builder()
        .platform(PlatformType.WEB)
        .user(userEnt)
        .authType(AuthType.SIGNUP)
        .authWay(AuthWay.BASIC)
        .build();

      authRepository.save(signUp);

      //update org with new employee
      if (optOrg.isPresent()) {
        var org = optOrg.get();
        var userOfOrg = org.getEmployees();
        userOfOrg.add(user);

        org.setEmployees(userOfOrg);
        organizationRepository.save(org);
      }
    } else {
      log.error(String.format("We have no %s role ", StaticRole.ROLE_USER));
      throw new RuntimeException("There is no such role in the db");
    }
  }

//  public boolean areSkillAndSimulationAvailable(Authentication authentication, Long skillId, Long simulationId) {
//    Optional<User> optUser = userRepository.findByEmail(authentication.getName());
//
//    //todo 1 org to 1 user when onboarding is a org
//    if(isSkillOnboarding(skillId)){
//      return true;
//    }
//
//    return optUser.map(user -> {
//        var skillOpt = user.getOrganization()
//          .getAvailableSkills()
//          .stream()
//          .filter(skillFilter -> skillFilter.getId().equals(skillId))
//          .findAny();
//        if (skillOpt.isPresent()) {
//          return skillOpt.get().getSimulations().keySet().stream().anyMatch(simulation -> simulation.getId().equals
//          (simulationId));
//        } else {
//          log.error(String.format(
//            "The user %s does not have access to these resources skill_id %s and simulation_id %s",
//            authentication.getName(),
//            skillId,
//            simulationId
//          ));
//          return false;
//        }
//      })
//      .orElseGet(() -> {
//        log.error(String.format("The user %s doesn't exit", authentication.getName()));
//        return false;
//      });
//  }

  private boolean isSkillOnboarding(final Long skillId) {
    //todo 1 org to 1 user when onboarding is a org
    var isOnboarding = skillRepository.findById(skillId);
    return isOnboarding
      .filter(skill -> Objects.nonNull(skill.getName()))
      .map(skill -> skill.getName().startsWith("<Onboarding>")).orElse(false);
  }

  private boolean isSimulationOnboarding(final Long simulationId) {
    //todo 1 org to 1 user when onboarding is a org
    var isOnboarding = simulationRepository.findById(simulationId);
    log.info("Found simulation {} for id {}", isOnboarding.isPresent(), simulationId);
    return isOnboarding
      .filter(simulation -> Objects.nonNull(simulation.getSkill()))
      .map(Simulation::getSkill)
      .filter(skill -> Objects.nonNull(skill.getName()))
      .map(skill -> skill.getName().startsWith("Onboarding"))
      .orElse(false);
  }

  public boolean isSkillAvailable(Authentication authentication, Long skillId) {
    Optional<User> optUser = userRepository.findByEmail(authentication.getName());

    //todo 1 org to 1 user when onboarding is a org
    if (isSkillOnboarding(skillId)) {
      return true;
    }

    return optUser.map(user -> {
        if (user.getOrganization()
          .getAvailableSkills()
          .stream()
          .anyMatch(skillFilter -> skillFilter.getId().equals(skillId))) {
          return true;
        } else {
          log.error(String.format(
            "The user %s does not have access to these resources skill %s ",
            authentication.getName(),
            skillId
          ));
          return false;
        }
      })
      .orElseGet(() -> {
        log.error(String.format("The user %s doesn't exit", authentication.getName()));
        return false;
      });
  }

  public boolean isSimulationAvailable(Authentication authentication, Long simulationId) {
    Optional<User> optUser = userRepository.findByEmail(authentication.getName());

    //todo 1 org to 1 user when onboarding is a org
    if (isSimulationOnboarding(simulationId)) {
      return true;
    }

    return optUser.map(user -> {
        if (user.getOrganization().getAvailableSkills()
          .stream().flatMap(a -> a.getSimulations().keySet().stream())
          .anyMatch(simulation -> simulation.getId().equals(simulationId))) {
          return true;
        } else {
          log.error(String.format(
            "The user %s does not have access to this simulation %s",
            authentication.getName(),
            simulationId
          ));
          return false;
        }
      })
      .orElseGet(() -> {
        log.error(String.format("The user %s doesn't exit", authentication.getName()));
        return false;
      });
  }

  public boolean orgHasEmployee(Authentication authentication, String org) {
    Optional<User> optUser = userRepository.findByEmailWithOrg(authentication.getName());

    if (optUser.isPresent()) {
      var user = optUser.get();

      if (Objects.nonNull(user.getOrganization())) {
        return user.getOrganization().getName().equalsIgnoreCase(org);
      } else {
        log.error("The organization cannot be empty");
        return false;
      }
    }

    return false;
  }

  public boolean isChatOfUser(Authentication authentication, Long chatId) {
    var optUser = userRepository.findByEmailWithOrg(authentication.getName());

    if (optUser.isPresent()) {
      var user = optUser.get();
      var existsByChatIdAndUser = chatRepository.existsByIdAndUser(chatId, user);
      if (existsByChatIdAndUser) {
        return true;
      } else {
        log.error(String.format("The user %s doesn't have chat %s ", user.getUsername(), chatId));
        return false;
      }
    }

    return false;
  }
}
