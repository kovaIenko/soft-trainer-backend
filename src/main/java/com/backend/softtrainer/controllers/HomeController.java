package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.SignUpUserResponseDto;
import com.backend.softtrainer.dtos.auth.LoginRequest;
import com.backend.softtrainer.dtos.auth.LoginResponse;
import com.backend.softtrainer.dtos.auth.RefreshTokenResponse;
import com.backend.softtrainer.dtos.auth.SignupRequestDto;
import com.backend.softtrainer.exceptions.UserAlreadyExitsException;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import com.backend.softtrainer.services.auth.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
@AllArgsConstructor
@Slf4j
public class HomeController {

  private final TokenService tokenService;
  private final AuthenticationManager authManager;
  private final CustomUsrDetailsService usrDetailsService;

  @GetMapping("/health")
  //@PreAuthorize("hasAnyRole('ROLE_OWNER')")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Welcome, Miha");
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody final LoginRequest request) {
    log.info(String.format("The user %s is trying to login by pass %s", request.email(), request.password()));
    UsernamePasswordAuthenticationToken authenticationToken =
      new UsernamePasswordAuthenticationToken(request.email(), request.password());
    Authentication auth = authManager.authenticate(authenticationToken);

    CustomUsrDetails user = (CustomUsrDetails) usrDetailsService.loadUserByUsername(request.email());
    usrDetailsService.createAuthRecord(user.user());
    String access_token = tokenService.generateAccessToken(user);
    String refresh_token = tokenService.generateRefreshToken(user);

    return ResponseEntity.ok(new LoginResponse(
      "User with email = " + request.email() + " successfully logined!",
      access_token,
      refresh_token,
      true,
      "success"
    ));
  }

  @GetMapping("/token/refresh")
  public ResponseEntity<RefreshTokenResponse> refreshToken(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");
    String refreshToken = headerAuth.substring(7);

    String email = tokenService.parseToken(refreshToken);
    CustomUsrDetails user = (CustomUsrDetails) usrDetailsService.loadUserByUsername(email);
    String access_token = tokenService.generateAccessToken(user);
    String refresh_token = tokenService.generateRefreshToken(user);

    return ResponseEntity.ok(new RefreshTokenResponse(
      access_token,
      refresh_token,
      true,
      "success"
    ));
  }

  @PostMapping("/signup")
  public ResponseEntity<SignUpUserResponseDto> signUp(@RequestBody final SignupRequestDto request) {
    try {
      usrDetailsService.createUser(request.email(), request.password());
      return ResponseEntity.ok(new SignUpUserResponseDto(request.email(), true, "success"));
    } catch (UserAlreadyExitsException e) {
      return ResponseEntity.ok(new SignUpUserResponseDto(request.email(), false, e.getMessage()));
    } catch (Exception e) {
      return ResponseEntity.ok(new SignUpUserResponseDto(request.email(), false, "unknown"));
    }
  }

}
