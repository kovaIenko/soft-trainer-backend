package com.backend.softtrainer.entities.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class MultiChoiceQuestionMessage extends Message {

  @JsonIgnore
  private String correct;

  private String options;

  @JsonProperty("is_voted")
  private boolean isVoted;

}
