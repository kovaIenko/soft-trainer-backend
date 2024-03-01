package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Entity;
import lombok.Data;

@Entity
@Data
public class MultiChoiceQuestionMessage extends Message {

  private String question;

  private String options;

}
