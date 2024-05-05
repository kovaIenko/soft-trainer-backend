package com.backend.softtrainer.services.auth;

import com.backend.softtrainer.dtos.StaticRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public class AuthUtils {

  public static boolean userIsOwnerApp(final Authentication authentication) {
    return authentication.getAuthorities()
      .stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(a -> a.equals(StaticRole.ROLE_OWNER.name()));
  }

}
