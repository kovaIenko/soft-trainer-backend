package com.backend.softtrainer.services.auth;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class TokenService {

  private final JwtEncoder jwtEncoder;

  public TokenService(JwtEncoder jwtEncoder) {
    super();
    this.jwtEncoder = jwtEncoder;
  }

  public String generateAccessToken(CustomUsrDetails usrDetails) {
    Instant now = Instant.now();
    String scope = usrDetails.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.joining(" "));

    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer("self")
      .issuedAt(now)
      .expiresAt(now.plus(7, ChronoUnit.DAYS))
      //there it equals to the email
      .subject(usrDetails.getUsername())
      .claim("scope", scope)
      .build();
    return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  public String generateRefreshToken(CustomUsrDetails usrDetails) {
    Instant now = Instant.now();
    String scope = usrDetails.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .collect(Collectors.joining(" "));

    JwtClaimsSet claims = JwtClaimsSet.builder()
      .issuer("self")
      .issuedAt(now)
      .expiresAt(now.plus(10, ChronoUnit.MINUTES))
      .subject(usrDetails.getUsername())
      .claim("scope", scope)
      .build();
    return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
  }

  public String parseToken(String token) {
    try {
      SignedJWT decodedJWT = SignedJWT.parse(token);
      return decodedJWT.getJWTClaimsSet().getSubject();
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return null;
  }
}
