package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.SumHyperParamDto;
import com.backend.softtrainer.dtos.UserHyperParamMaxValueDto;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.UserHyperParameter;
import com.backend.softtrainer.events.HyperParameterUpdatedEvent;
import com.backend.softtrainer.repositories.UserHyperParameterRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class UserHyperParameterService {

  private final UserHyperParameterRepository userHyperParameterRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public boolean update(final Long chatId, final String key, final double newValue) {
    log.info("Update the hyper parameter with key: {} for the chatId: {} to the value: {}", key, chatId, newValue);
    var userHyperParamOptional = userHyperParameterRepository.findUserHyperParameterByChatIdAndKey(chatId, key);

    if (userHyperParamOptional.isPresent()) {
      var userHyperParam = userHyperParamOptional.get();
      userHyperParam.setValue(newValue);
      userHyperParameterRepository.save(userHyperParam);

      // Publish event for hyperparameter update
      if (userHyperParam.getOwnerId() != null) {
        String userEmail = userHyperParameterRepository.findUserEmailById(userHyperParam.getOwnerId());
        if (userEmail != null) {
          eventPublisher.publishEvent(new HyperParameterUpdatedEvent(userEmail));
          log.info("Published hyperparameter update event for user: {}", userEmail);
        }
      }
      return true;
    }
    log.warn(String.format("Hyper Parameter with key: %s wasn't found for the chatId: %s", key, chatId));
    return false;
  }

  public Double getOrCreateUserHyperParameter(final Long chatId, final String key, final Long ownerId) {
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
      userHyperParam.setOwnerId(ownerId);
      var savedParam = userHyperParameterRepository.save(userHyperParam);

      // Publish event for new hyperparameter
      if (savedParam.getOwnerId() != null) {
        String userEmail = userHyperParameterRepository.findUserEmailById(savedParam.getOwnerId());
        if (userEmail != null) {
          eventPublisher.publishEvent(new HyperParameterUpdatedEvent(userEmail));
          log.info("Published hyperparameter update event for user: {}", userEmail);
        }
      }
      return savedParam.getValue();
    }
  }

  // Backward compatibility
  public Double getOrCreateUserHyperParameter(final Long chatId, final String key) {
    return getOrCreateUserHyperParameter(chatId, key, null);
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

  public List<UserHyperParameter> findAllByUser(final User user) {
    if (user == null || user.getId() == null) {
      return List.of();
    }
    return userHyperParameterRepository.findAllByOwnerId(user.getId());
  }

}
