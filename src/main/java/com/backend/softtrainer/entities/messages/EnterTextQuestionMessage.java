package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@EqualsAndHashCode(callSuper = true)
@Entity
@SuperBuilder
@NoArgsConstructor
@Data
public class EnterTextQuestionMessage extends Message {

  @Column(length = 700)
  private String content;

  private String answer;

  private String openAnswer;

  @Column(length = 700)
  private String correct;

  @Column(length = 700)
  private String options;

  @JdbcTypeCode(SqlTypes.JSON)
  @JsonProperty("selected")
  private Integer selected;

}
