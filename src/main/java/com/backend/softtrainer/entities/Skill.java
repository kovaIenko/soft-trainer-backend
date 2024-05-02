package com.backend.softtrainer.entities;

import com.backend.softtrainer.entities.flow.FlowNode;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity(name = "skills")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Skill {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  //integer is an order of the simulation in that skill
  @ElementCollection(fetch = FetchType.EAGER)
  //reference to the first node of simulations
  private Map<FlowNode, Long> simulations = new HashMap<>();

  @Column(length = 1000)
  private String avatar;

  @Column(length = 100)
  private String name;

  @Column(name = "timestamp", insertable = false, updatable = false)
  @CreationTimestamp(source = SourceType.DB)
  private LocalDateTime timestamp;

}
