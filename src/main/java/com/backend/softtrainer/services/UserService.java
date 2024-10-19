package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UserService {

  private final UserRepository userRepository;

  public List<User> findAllCollegues(final User user){
    return userRepository.findAllByOrganizations(user.getOrganization());
  }

  public void updateName(final User user, final String name){
    userRepository.updateName(user, name);
  }

}
