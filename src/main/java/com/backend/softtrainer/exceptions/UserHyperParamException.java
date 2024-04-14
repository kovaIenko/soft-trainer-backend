package com.backend.softtrainer.exceptions;

public class UserHyperParamException extends Exception {

  private String error;

  public UserHyperParamException(String error){
    super(error);
  }

}
