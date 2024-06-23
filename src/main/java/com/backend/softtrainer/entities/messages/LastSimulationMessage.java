package com.backend.softtrainer.entities.messages;

import com.backend.softtrainer.dtos.UserHyperParamResponseDto;
import com.backend.softtrainer.entities.Prompt;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
public class LastSimulationMessage extends Message {

  @Transient
  private List<UserHyperParamResponseDto> hyperParams;

  private String content;

  @Transient
  private String title;

  @ManyToOne
  @JoinColumn(name = "prompt_id")
  private Prompt prompt;

}
