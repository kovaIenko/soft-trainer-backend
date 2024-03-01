package com.backend.softtrainer.entities.flow;

import com.backend.softtrainer.entities.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity(name = "flows")
@Data
@SuperBuilder
@NoArgsConstructor
public class FlowQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(name = "order_number", nullable = false)
  private long orderNumber;

  //pointer to the parent orderNumber
  @Column(name = "previous_order_number", nullable = false)
  private long previousOrderNumber = 0L;

  @Column(name="message_type", nullable = false)
  private MessageType messageType;

  //for example 'giving feedback'
  private String name;

  @Column(name = "show_predicate", nullable = false)
  private String showPredicate;

}
