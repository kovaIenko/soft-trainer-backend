package com.backend.softtrainer.exceptions;

public class SendMessageConditionException extends Exception {

  private String error;

  public SendMessageConditionException(final String error){
    super(error);
  }
}
