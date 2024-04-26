package com.backend.softtrainer.entities;

import com.backend.softtrainer.entities.flow.FlowNode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  private String avatar;

  private String name;

  @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
  @JoinTable(name = "organizations_simulations",
    joinColumns = @JoinColumn(name = "organization_id"),
    inverseJoinColumns = @JoinColumn(name = "flow_id"))
  private Set<FlowNode> availableSimulations;

  @OneToMany(fetch = FetchType.LAZY)
  private List<User> employees;

}
