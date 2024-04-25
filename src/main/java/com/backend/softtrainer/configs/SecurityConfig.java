package com.backend.softtrainer.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

//  @Bean
//  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
//    System.out.println("web security works");
//    http
//      .cors()
//      .and()
//      .authorizeRequests()
////            .antMatchers(HttpMethod.GET, "/user/info", "/api/foos/**")
////            .hasAuthority("SCOPE_read")
////            .antMatchers(HttpMethod.POST, "/api/foos")
////            .hasAuthority("SCOPE_write")
//      .anyRequest()
//      .authenticated()
//      .and()
//      //.cors(cors->cors.configurationSource(corsConfiguration()))
//      .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
//
//    return http.build();
//  }


  @Bean
  SecurityFilterChain resourceServerSecurityFilterChain(
    HttpSecurity http)
    throws Exception {
    http.oauth2ResourceServer(oauth2 -> oauth2.jwt());
   // http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    //http.csrf(csrf -> csrf.disable());
//    http.exceptionHandling(handeling -> handeling.authenticationEntryPoint((request, response, authException) -> {
//      response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Restricted Content\"");
//      response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
//    }));
    http.authorizeHttpRequests().anyRequest().authenticated();
//    http.cors(cors -> {
//      if (true) {
//        cors.disable();
//      } else {
//       // cors.configurationSource(corsConfig(allowedOrigins));
//      }
//    });
    return http.build();
  }

//  CorsConfigurationSource corsConfig(List<String> allowedOrigins) {
//    final var source = new UrlBasedCorsConfigurationSource();
//
//    final var configuration = new CorsConfiguration();
//    configuration.setAllowedOrigins(allowedOrigins);
//    configuration.setAllowedMethods(List.of("*"));
//    configuration.setAllowedHeaders(List.of("*"));
//    configuration.setExposedHeaders(List.of("*"));
//
//    source.registerCorsConfiguration("/**", configuration);
//    return source;
//  }

    private CorsConfigurationSource corsConfiguration() {
      var a = new UrlBasedCorsConfigurationSource();
        //  registry.addMapping("/**").allowedOrigins("https://test-web-flutter-427fd.web.app/");
        return a;

    }


}
