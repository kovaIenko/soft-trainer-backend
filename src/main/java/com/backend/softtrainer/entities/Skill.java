package com.backend.softtrainer.entities;

import com.backend.softtrainer.entities.enums.BehaviorType;
import com.backend.softtrainer.entities.enums.SkillType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
  @Builder.Default
  private Map<Simulation, Long> simulations = new HashMap<>();

  @OneToMany(mappedBy = "skill", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Material> materials = new ArrayList<>();

  @Column(length = 1000)
  private String avatar;

  @Column(length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  private SkillType type;

  @Enumerated(EnumType.STRING)
  private BehaviorType behavior;

  private Integer simulationCount;

  @Builder.Default
  private boolean isHidden = false;

  @Builder.Default
  private boolean isProtected = false;

  @Builder.Default
  private boolean isAdminHidden = false;

  @Column(name = "timestamp", insertable = false, updatable = false)
  @CreationTimestamp(source = SourceType.DB)
  private LocalDateTime timestamp;

  @Column(length = 1000)
  private String description;

}
