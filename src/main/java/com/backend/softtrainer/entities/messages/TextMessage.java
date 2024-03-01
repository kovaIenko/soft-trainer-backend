package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class TextMessage extends Message {

  private String content;

}
