package com.backend.softtrainer.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

  //todo change it to the User Entity
  private Long ownerId;

  //id from the table hyperparams
  private String key;

  private Double value;

}
