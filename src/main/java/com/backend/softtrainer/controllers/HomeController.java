package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.auth.LoginRequest;
import com.backend.softtrainer.dtos.auth.LoginResponse;
import com.backend.softtrainer.dtos.auth.RefreshTokenResponse;
import com.backend.softtrainer.dtos.auth.SignupRequestDto;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import com.backend.softtrainer.services.auth.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

  @PreAuthorize("hasAnyRole('OWNER')")
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("Hello, Misha");
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@RequestBody final LoginRequest request) {

    UsernamePasswordAuthenticationToken authenticationToken =
      new UsernamePasswordAuthenticationToken(request.email(), request.password());
    Authentication auth = authManager.authenticate(authenticationToken);

    CustomUsrDetails user = (CustomUsrDetails) usrDetailsService.loadUserByUsername(request.email());
    String access_token = tokenService.generateAccessToken(user);
    String refresh_token = tokenService.generateRefreshToken(user);

    return ResponseEntity.ok(new LoginResponse("User with email = "+ request.email() + " successfully logined!",access_token, refresh_token));
  }

  @GetMapping("/token/refresh")
  public ResponseEntity<RefreshTokenResponse> refreshToken(HttpServletRequest request) {
    String headerAuth = request.getHeader("Authorization");
    String refreshToken = headerAuth.substring(7);

    String email = tokenService.parseToken(refreshToken);
    CustomUsrDetails user = (CustomUsrDetails) usrDetailsService.loadUserByUsername(email);
    String access_token = tokenService.generateAccessToken(user);
    String refresh_token = tokenService.generateRefreshToken(user);

    return ResponseEntity.ok(new RefreshTokenResponse(access_token, refresh_token));
  }

  @PostMapping("/signup")
  public ResponseEntity<String> signUp(@RequestBody final SignupRequestDto request) {
    usrDetailsService.createUser(request.email(), request.password());
    return ResponseEntity.ok("User registered successfully.");
  }

}
