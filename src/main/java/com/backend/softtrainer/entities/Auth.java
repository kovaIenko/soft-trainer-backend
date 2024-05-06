package com.backend.softtrainer.entities;

import com.backend.softtrainer.entities.enums.AuthType;
import com.backend.softtrainer.entities.enums.AuthWay;
import com.backend.softtrainer.entities.enums.PlatformType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.LocalDateTime;

@Entity(name = "logins")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Auth {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column(name = "timestamp", insertable = false, updatable = false)
  @CreationTimestamp(source = SourceType.DB)
  private LocalDateTime timestamp;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_type")
  private AuthType authType;

  @Enumerated(EnumType.STRING)
  private PlatformType platform;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_way")
  private AuthWay authWay;

}
