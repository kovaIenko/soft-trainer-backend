package com.backend.softtrainer.entities;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Entity(name = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @NotNull
  @Email
  @Size(max = 100)
  private String email;

  @NotNull
  @Size(min = 3, max = 50)
  private String username;

  private String avatar;

  private String password;

  //todo add org entity
  private String company;

//  public void setPassword(String password) {
//    this.password = new BCryptPasswordEncoder().encode(password);
//  }

}
