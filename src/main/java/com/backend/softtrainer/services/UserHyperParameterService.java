package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.UserHyperParameter;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class UserHyperParameterService {

  private final UserHyperParameterRepository userHyperParameterRepository;

  @Transactional
  public boolean update(final Long chatId, final String key, final double newValue) {
    var userHyperParamOptional = userHyperParameterRepository.findUserHyperParameterByChatIdAndKey(chatId, key);

    if (userHyperParamOptional.isPresent()) {
      var userHyperParam = userHyperParamOptional.get();
      userHyperParam.setValue(newValue);
      userHyperParameterRepository.save(userHyperParam);
      return true;
    }
    log.warn(String.format("Hyper Parameter with key: %s wasn't found for the chatId: %s", key, chatId));
    return false;
  }

  public Double getOrCreateUserHyperParameter(final Long chatId, final String key) {
    var userHyperParamOptional = userHyperParameterRepository.findUserHyperParameterByChatIdAndKey(chatId, key);

    if (userHyperParamOptional.isPresent()) {
      return userHyperParamOptional.get().getValue();
    } else {
      log.warn("Hyper Parameter with key: {} wasn't found for the chatId: {}", key, chatId);
      var userHyperParam = new UserHyperParameter();
      userHyperParam.setChatId(chatId);
      userHyperParam.setKey(key);
      userHyperParam.setValue((double) 0);
      return userHyperParameterRepository.save(userHyperParam).getValue();
    }
  }

  public List<UserHyperParameter> findAllByChatId(final Long chatId) {
    return userHyperParameterRepository.findAllByChatId(chatId);
  }

}
