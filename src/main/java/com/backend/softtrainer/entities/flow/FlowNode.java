package com.backend.softtrainer.entities.flow;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

  @ManyToOne
//  @JoinColumn(name = "simulation_id")
  private Simulation simulation;

}
