package com.backend.softtrainer.controllers;

import com.backend.softtrainer.dtos.ContactInfoRequestDto;
import com.backend.softtrainer.dtos.ContactInfoResponse;
import com.backend.softtrainer.dtos.SignUpUserResponseDto;
import com.backend.softtrainer.dtos.auth.LoginRequest;
import com.backend.softtrainer.dtos.auth.LoginResponse;
import com.backend.softtrainer.dtos.auth.RefreshTokenResponse;
import com.backend.softtrainer.dtos.auth.SignupRequestDto;
import com.backend.softtrainer.entities.ContactInfo;
import com.backend.softtrainer.exceptions.UserAlreadyExitsException;
import com.backend.softtrainer.repositories.ContactInfoRepository;
import com.backend.softtrainer.services.UserDataExtractor;
import com.backend.softtrainer.services.auth.CustomUsrDetails;
import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import com.backend.softtrainer.services.auth.TokenService;
import com.backend.softtrainer.services.notifications.EmailService;
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
  private final ContactInfoRepository contactInfoRepository;
  private final UserDataExtractor userDataExtractor;
  private final EmailService emailService;

  @GetMapping("/health")
  //@PreAuthorize("hasAnyRole('ROLE_OWNER')")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok(String.format("Welcome, Miha. Here is current version of the app: %s", getManifestInfo()));
  }

  public String getManifestInfo() {
    return getClass().getPackage().getImplementationVersion();
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

    var isOnboarded = userDataExtractor.getFirstChatOfOnboarding(user.user()).isPresent();

    var org = user.user().getOrganization();
    return ResponseEntity.ok(new LoginResponse(
      "User with email = " + request.email() + " successfully logined!",
      access_token,
      refresh_token,
      true,
      "success",
      user.user().getId(),
      isOnboarded,
      org.getLocalization()
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

  private  static String EMAIL_SUBJECT = "Contact information from thesofttrainer.com";
  private static String EMAIL_BODY = """
    + 1 one contact information was left on thesofttrainer.com:
    Name: %s
    Contact: %s
    Request: %s
    """;

  @PostMapping("/contact/info")
  public ResponseEntity<ContactInfoResponse> addContactInfo(@RequestBody final ContactInfoRequestDto request) {
    try {
      var entity = ContactInfo.builder()
        .contact(request.contact())
        .name(request.name())
        .request(request.request())
        .build();
      emailService.sendEmail(String.format(EMAIL_SUBJECT), String.format(EMAIL_BODY, request.name(), request.contact(), request.request()));
      contactInfoRepository.save(entity);
      return ResponseEntity.ok(new ContactInfoResponse(true, "success"));
    } catch (Exception e) {
      return ResponseEntity.ok(new ContactInfoResponse(false, "unknown"));
    }
  }

}
