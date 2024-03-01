package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@SuperBuilder
@NoArgsConstructor
public class EnterTextQuestionMessage extends Message {

  private String content;

}
