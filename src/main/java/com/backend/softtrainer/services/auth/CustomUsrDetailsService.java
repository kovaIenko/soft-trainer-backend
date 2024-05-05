package com.backend.softtrainer.services.auth;

import com.backend.softtrainer.dtos.StaticRole;
import com.backend.softtrainer.entities.Auth;
import com.backend.softtrainer.entities.enums.AuthType;
import com.backend.softtrainer.entities.enums.AuthWay;
import com.backend.softtrainer.entities.enums.PlatformType;
import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.exceptions.UserAlreadyExitsException;
import com.backend.softtrainer.repositories.AuthRepository;
import com.backend.softtrainer.repositories.RoleRepository;
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

import static com.backend.softtrainer.services.auth.AuthUtils.isOwnerApp;

@Service
@RequiredArgsConstructor
@Data
@Slf4j
public class CustomUsrDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;
  private final AuthRepository authRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new UsernameNotFoundException("User with email = " + email + " not exist!"));
    return new CustomUsrDetails(user);
  }

  public void createAuthRecord(final Long userId) {
    var login = Auth.builder()
      .platform(PlatformType.WEB)
      .userId(userId)
      .authType(AuthType.LOGIN)
      .authWay(AuthWay.BASIC)
      .build();
    authRepository.save(login);
  }

  public void createUser(String email, String password) throws UserAlreadyExitsException {
    var userByEmail = userRepository.findByEmail(email);
    if(userByEmail.isPresent())
      throw new UserAlreadyExitsException(String.format("The user with email %s already exists", email));
    var optLowestRole = roleRepository.findByName(StaticRole.ROLE_USER);
    if (optLowestRole.isPresent()) {
      User user = new User();
      user.setUsername(email);
      user.setPassword(passwordEncoder.encode(password));
      user.setEmail(email);
      var roles = new HashSet<Role>();
      roles.add(optLowestRole.get());
      user.setRoles(roles);
      var userEnt = userRepository.save(user);

      var signUp = Auth.builder()
        .platform(PlatformType.WEB)
        .userId(userEnt.getId())
        .authType(AuthType.SIGNUP)
        .authWay(AuthWay.BASIC)
        .build();

      authRepository.save(signUp);
    } else {
      log.error(String.format("We have no %s role ", StaticRole.ROLE_USER));
      throw new RuntimeException("There is no such role in the db");
    }
  }

  public boolean isResourceOwner(Authentication authentication, Long ownerId) {
    Optional<User> optUser = userRepository.findByEmail(authentication.getName());
    return optUser.map(user -> user.getId().equals(ownerId)).orElse(false);
  }

  public boolean isSkillAvailable(Authentication authentication, Long skillId) {
    Optional<User> optUser = userRepository.findByEmail(authentication.getName());
    if (isOwnerApp(authentication)) {
      return true;
    }
    if (optUser.isPresent()) {
      for (Skill skill : optUser.get().getOrganization().getAvailableSkills()) {
        if (Objects.equals(skill.getId(), skillId)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean orgHasEmployee(Authentication authentication, String org) {
    Optional<User> optUser = userRepository.findByEmailWithOrg(authentication.getName());

    if (optUser.isPresent()) {
      var user = optUser.get();

      if (Objects.nonNull(user.getOrganization())) {
        return user.getOrganization().getName().equalsIgnoreCase(org);
      } else {
        return false;
      }
    }
    return false;
  }

}
