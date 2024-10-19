package com.backend.softtrainer.entities.flow;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.core.annotation.Order;

@Entity(name = "nodes")
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"character", "showPredicate", "simulation"})
public class FlowNode {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Order
  @Column(name = "order_number", nullable = false)
  private Long orderNumber;

  @Column(name = "previous_order_number", nullable = false)
  private Long previousOrderNumber = 0L;

  @Column(name = "message_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private MessageType messageType;

  @Column(name = "show_predicate", nullable = false)
  private String showPredicate;

  @ManyToOne
  private Character character;

  @ManyToOne(fetch = FetchType.EAGER)
  private Simulation simulation;

  private boolean hasHint;

  @JsonProperty("response_time_limit")
  @Column(name = "response_time_limit")
  private Long responseTimeLimit;

}
