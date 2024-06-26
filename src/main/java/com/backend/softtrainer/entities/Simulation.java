package com.backend.softtrainer.entities;


import com.backend.softtrainer.entities.enums.SimulationComplexity;
import com.backend.softtrainer.entities.flow.FlowNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.LocalDateTime;
import java.util.List;

@Entity(name = "simulations")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"skill", "nodes", "avatar"})
@Data
@EqualsAndHashCode(exclude = {"skill"})
public class Simulation {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String name;

  @OneToMany(cascade = CascadeType.ALL)
  private List<FlowNode> nodes;

  private String avatar;

  @Enumerated(EnumType.STRING)
  private SimulationComplexity complexity;

  @Column(name = "created_at", insertable = false, updatable = false)
  @CreationTimestamp(source = SourceType.DB)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.EAGER)
  private Skill skill;

  @Column(name = "is_open")
  private boolean isOpen;

}
