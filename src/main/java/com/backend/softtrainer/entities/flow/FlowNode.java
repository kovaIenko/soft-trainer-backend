package com.backend.softtrainer.entities.flow;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.enums.SimulationComplexity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;

@Entity(name = "flows")
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"character", "showPredicate", "name"})
public class FlowNode {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Order
  @Column(name = "order_number", nullable = false)
  private Long orderNumber;

  //pointer to the parent orderNumber
  @Column(name = "previous_order_number", nullable = false)
  private Long previousOrderNumber = 0L;

  @Column(name = "message_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private MessageType messageType;

  //for example 'giving feedback'
  private String name;

  @Column(name = "show_predicate", nullable = false)
  private String showPredicate;

  @ManyToOne
  private Character character;

  @Enumerated(EnumType.STRING)
  private SimulationComplexity complexity;

  @Column(name = "timestamp", insertable = false, updatable = false)
  @CreationTimestamp(source = SourceType.DB)
  private LocalDateTime timestamp;

}
