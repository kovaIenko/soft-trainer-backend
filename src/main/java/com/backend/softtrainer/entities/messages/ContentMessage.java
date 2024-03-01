package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class ContentMessage extends Message {

  private String url;

}
