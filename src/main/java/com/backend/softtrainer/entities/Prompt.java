package com.backend.softtrainer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "prompts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Prompt {

  @Id
  //@GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Enumerated(EnumType.STRING)
  private PromptName name;

  @Column(length = 5000)
  private String prompt;

  @Column(name = "is_on")
  private boolean isOn;

  @Column(name = "assistant_id")
  private String assistantId;

}
