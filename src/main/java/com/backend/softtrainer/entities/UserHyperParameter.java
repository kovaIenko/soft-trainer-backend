package com.backend.softtrainer.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity(name = "user_hyperparams")
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserHyperParameter {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private Long chatId;

  private Long ownerId;

  private Long simulationId;

  //id from the table hyperparams
  private String key;

  private Double value;

  @Column(name = "updated_at")
  @UpdateTimestamp
  private LocalDateTime updatedAt;
}
