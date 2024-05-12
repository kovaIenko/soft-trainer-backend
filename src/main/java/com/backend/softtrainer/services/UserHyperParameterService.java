package com.backend.softtrainer.services;

import com.backend.softtrainer.entities.UserHyperParameter;
import com.backend.softtrainer.exceptions.UserHyperParamException;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class UserHyperParameterService {

  private final UserHyperParameterRepository userHyperParameterRepository;

  @Transactional
  public UserHyperParameter update(final Long chatId, final String key, final double newValue) throws UserHyperParamException {
    var userHyperParamOptional = userHyperParameterRepository.findUserHyperParameterByChatIdAndKey(chatId, key);

    if (userHyperParamOptional.isPresent()) {
      var userHyperParam = userHyperParamOptional.get();
      userHyperParam.setValue(newValue);
      return userHyperParameterRepository.save(userHyperParam);
    }
    throw new UserHyperParamException(String.format("Hyper Parameter with key: %s wasn't found for the chatId: %s", key, chatId));
  }


  public UserHyperParameter getUserHyperParam(final Long chatId, final String key) throws UserHyperParamException {
    var userHyperParamOptional = userHyperParameterRepository.findUserHyperParameterByChatIdAndKey(chatId, key);

    if (userHyperParamOptional.isPresent()) {
      return userHyperParamOptional.get();
    }
    throw new UserHyperParamException(String.format("Hyper Parameter with key: %s wasn't found for the chatId: %s", key, chatId));
  }

}
