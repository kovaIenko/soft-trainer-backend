package com.backend.softtrainer.exceptions;

public class InsufficientUserPrivilegesException extends Exception {

  public InsufficientUserPrivilegesException(String message){
    super(message);
  }

}
