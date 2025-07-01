package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;

    public List<User> findAllCollegues(User user) {
        return userRepository.findAllByOrganization(user.getOrganization());
  }

    public void updateName(User user, String name) {
    userRepository.updateName(user, name);
  }
}
