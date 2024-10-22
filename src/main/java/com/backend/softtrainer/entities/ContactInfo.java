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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.LocalDateTime;

@Entity(name = "contact_info")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String contact;

  private String name;

  @Column(length = 1000)
  private String request;

  @Column(insertable = false, updatable = false)
  @CreationTimestamp(source = SourceType.DB)
  private LocalDateTime timestamp;

}
