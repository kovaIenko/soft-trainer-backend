package com.backend.softtrainer.services.auth;

import com.backend.softtrainer.entities.Role;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.RoleRepository;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@RequiredArgsConstructor
@Data
public class CustomUsrDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private final RoleRepository roleRepository;

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
      .orElseThrow(() -> new UsernameNotFoundException("User with email = " + email + " not exist!"));
    return new CustomUsrDetails(user);
  }

  public void createUser(String email, String password) {
    var optLowestRole = roleRepository.findByName("User");
    if (optLowestRole.isPresent()) {
      User user = new User();
      user.setUsername(email);
      user.setPassword(passwordEncoder.encode(password));
      user.setEmail(email);
      var roles = new HashSet<Role>();
      roles.add(optLowestRole.get());
      user.setRoles(roles);
      userRepository.save(user);
    } else {
      throw new RuntimeException("There is no such role in the db");
    }
  }

}
