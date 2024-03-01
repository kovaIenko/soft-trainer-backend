package com.backend.softtrainer.repositories;

import com.backend.softtrainer.interpreter.entity.PredicateMessage;
import com.backend.softtrainer.interpreter.runnerValues.MessageProvider;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MessageProviderImpl implements MessageProvider {

  @NotNull
  @Override
  public List<PredicateMessage> getMessages() {
    return null;
  }
}
