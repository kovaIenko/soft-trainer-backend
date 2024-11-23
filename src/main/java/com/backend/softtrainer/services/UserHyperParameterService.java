package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.SumHyperParamDto;
import com.backend.softtrainer.dtos.UserHyperParamMaxValueDto;
import com.backend.softtrainer.entities.User;
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
    log.info("Update the hyper parameter with key: {} for the chatId: {} to the value: {}", key, chatId, newValue);
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
      log.info("Hyper Parameter with key: {} was found for the chatId: {} and the value is {}", key, chatId, userHyperParamOptional.get().getValue());
      return userHyperParamOptional.get().getValue();
    } else {
      log.info("Hyper Parameter with key: {} wasn't found for the chatId: {}", key, chatId);
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

  public List<UserHyperParamMaxValueDto> findHyperParamsWithMaxValues(final Long chatId) {
    return userHyperParameterRepository.findHyperParamsWithMaxValues(chatId);
  }

  public List<SumHyperParamDto> sumUpByUser(final User user) {
    return userHyperParameterRepository.sumUpByUser(user.getId());
  }

}
