package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.AllColleaguesResponseDto;
import com.backend.softtrainer.dtos.SumHyperParamDto;
import com.backend.softtrainer.dtos.UserDto;
import com.backend.softtrainer.services.UserDataExtractor;
import com.backend.softtrainer.services.UserHyperParameterService;
import com.backend.softtrainer.services.UserService;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/leaderboard")
@AllArgsConstructor
@Slf4j
public class LeaderboardController {

  private final CustomUsrDetailsService usrDetailsService;
  private final UserService userService;
  private final UserDataExtractor userDataExtractor;
  private final UserHyperParameterService userHyperParameterService;


  @GetMapping
  public ResponseEntity<AllColleaguesResponseDto> getLeaderboard(final Authentication authentication) {
    try {
      var userDetails = (CustomUsrDetails) usrDetailsService.loadUserByUsername(authentication.getName());
      var user = userDetails.user();

      var colleaguesDto = userService.findAllCollegues(user)
        .stream().map(usr -> {

          var collectedData = userHyperParameterService.sumUpByUser(usr);

          var sumUp = collectedData.stream()
            .mapToDouble(SumHyperParamDto::value)
            .sum();

          //todo high load - bottleneck
          var userName = Optional.ofNullable(usr.getName()).filter(name -> !name.isBlank())
            .orElseGet(() -> userDataExtractor.extractUserName(usr)
              .map(name -> {
                log.info("Extracted name from the onboarding {} for the user.email {}", name, usr.getEmail());
                return name;
              }).orElse(usr.getEmail()));

          return new UserDto(usr.getId(), usr.getDepartment(), userName, usr.getAvatar(), sumUp);
        })

        .sorted(Comparator.comparingDouble(UserDto::exp).reversed())
        .collect(Collectors.toList());

      return ResponseEntity.ok(new AllColleaguesResponseDto(colleaguesDto, true, "success"));
    } catch (Exception e) {
      String errorMessage = String.format("Error while getting leaderboard for the user %s", authentication.getName());
      log.error(errorMessage, e);
      return ResponseEntity.ok(new AllColleaguesResponseDto(new ArrayList<>(), false, "Error while getting leaderboard"));
    }
  }

}
