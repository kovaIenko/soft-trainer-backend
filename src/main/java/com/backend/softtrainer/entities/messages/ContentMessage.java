package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class ContentMessage extends Message {

  @Column(length = 1000)
  private String url;

}
