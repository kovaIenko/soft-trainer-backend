
package com.backend.softtrainer.entities;

import com.backend.softtrainer.dtos.StaticRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity(name = "roles")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Role {

  @Id
  private Long id;

  @Enumerated(EnumType.STRING)
  private StaticRole name;

}
