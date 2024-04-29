package com.backend.softtrainer.configs;


import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

@Component
public class JwtCustomAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Collection<GrantedAuthority> grantedAuthorities = new ArrayList<>();
    String[] splitScopes = ((String)jwt.getClaim("scope")).split(" ");
    for (String authority : splitScopes) {
      grantedAuthorities.add(new SimpleGrantedAuthority(authority));
    }
    return grantedAuthorities;
  }
}
