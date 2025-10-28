package com.backend.softtrainer.entities.messages;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Entity
@SuperBuilder
@NoArgsConstructor
@Data
public class ContentMessage extends Message {

  @Column(length = 3000)
  private String content;

  @Column(length = 500)
  private String preview;

}
