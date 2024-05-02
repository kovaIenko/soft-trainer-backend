package com.backend.softtrainer.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "hyperparams")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HyperParameter {

  @Id
  private String key;

  @Column(name = "simulation_name")
  private String simulationName;

}
