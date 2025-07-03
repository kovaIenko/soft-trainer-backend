package com.backend.softtrainer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "hyperparams")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HyperParameter {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(name = "\"key\"") // Escape reserved keyword for H2 compatibility
  private String key;

  private String description;

  @Column(name = "simulation_id")
  private Long simulationId;

  private Double maxValue;

}
