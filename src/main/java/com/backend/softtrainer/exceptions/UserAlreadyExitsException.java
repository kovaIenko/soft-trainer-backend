package com.backend.softtrainer.exceptions;

public class UserAlreadyExitsException extends Exception {

  public UserAlreadyExitsException(String error) {
    super(error);
  }

}
