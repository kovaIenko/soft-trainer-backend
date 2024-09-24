package com.backend.softtrainer.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Entity(name = "organizations")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Organization {

  @Id
//  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String avatar;

  private String name;

  @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
  @JoinTable(name = "organizations_skills",
    joinColumns = @JoinColumn(name = "organization_id"),
    inverseJoinColumns = @JoinColumn(name = "skill_id"))
  //todo make simulation names unique withing the skill
  private Set<Skill> availableSkills;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "organization", cascade = CascadeType.ALL)
  private List<User> employees;

  @Column(name = "localization")
  private String localization;

}
