package com.backend.softtrainer.entities.messages;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
public class MultiChoiceTaskQuestionMessage extends Message {

  @JsonIgnore
  @Column(length = 700)
  private String correct;

  @Column(length = 700)
  private String options;

  @JsonProperty("is_voted")
  private boolean isVoted;

  private String answer;

}
