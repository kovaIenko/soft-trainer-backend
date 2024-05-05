package com.backend.softtrainer.configs;

import com.backend.softtrainer.services.auth.CustomUsrDetailsService;
import com.backend.softtrainer.services.auth.TokenService;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.text.ParseException;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final RsaProperties rsaKeys;

  @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
  private String jwkSetUri;

  @Value("${security.enabled}")
  private boolean isSecurityEnabled;

  private final JwtCustomAuthoritiesConverter jwtCustomAuthoritiesConverter;

  private final CustomUsrDetailsService customUsrDetailsService;

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public UserDetailsService customUserDetailsService() {
    customUsrDetailsService.setPasswordEncoder(passwordEncoder());
    return customUsrDetailsService;
  }

  @Bean
  public AuthenticationManager authManager() {
    var authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(customUserDetailsService());
    authProvider.setPasswordEncoder(passwordEncoder());
    return new ProviderManager(authProvider);
  }

  @Bean
  JwtEncoder jwtEncoder() {
    JWK jwk = new RSAKey.Builder(rsaKeys.publicKey()).privateKey(rsaKeys.privateKey()).build();
    JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
    return new NimbusJwtEncoder(jwkSource);
  }

  @Bean
  TokenService tokenService() {
    return new TokenService(jwtEncoder());
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    if (isSecurityEnabled) {
      http
        .cors()
        .and()
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(authorize -> authorize
          .requestMatchers("/login").permitAll()
          .requestMatchers("/signup").permitAll()
          .requestMatchers("/health").permitAll()
          .requestMatchers("/token/refresh").permitAll()
          .anyRequest().authenticated())
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .oauth2ResourceServer((auth) -> auth.jwt((jwt) -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    } else {
      http
        .cors().and()
        .csrf(AbstractHttpConfigurer::disable);
    }
    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return token -> {
      try {
        if (JWTParser.parse(token).getJWTClaimsSet().getIssuer().equals("self")) {
          System.err.println("Self!");
          JwtDecoder jwtDecoder = NimbusJwtDecoder.withPublicKey(rsaKeys.publicKey()).build();
          return jwtDecoder.decode(token);
        } else if (JWTParser.parse(token).getJWTClaimsSet().getIssuer().equals("accounts.google.com")) {
          System.err.println("Google");
          JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
          return jwtDecoder.decode(token);
        }
      } catch (ParseException e) {
        e.printStackTrace();
      }
      return null;
    };
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtCustomAuthoritiesConverter);
    return jwtAuthenticationConverter;
  }

  /**
   * control @EnableGlobalMethodSecurity，to  solve AuthenticationCredentialsNotFoundException
   */
  @ConditionalOnProperty(prefix = "security",
    name = "enabled",
    havingValue = "true")
  @EnableMethodSecurity
  static class Dummy {
  }

}
