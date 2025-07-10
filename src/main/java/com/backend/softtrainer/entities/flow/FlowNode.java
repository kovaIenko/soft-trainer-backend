package com.backend.softtrainer.entities.flow;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.List;

@Entity(name = "nodes")
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"character", "showPredicate", "simulation"})
public class FlowNode {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Order
  @Column(name = "order_number", nullable = false)
  private Long orderNumber;

  @Builder.Default
  @Column(name = "previous_order_number", nullable = false)
  private Long previousOrderNumber = 0L;

  @Column(name = "message_type", nullable = false)
  @Enumerated(EnumType.STRING)
  private MessageType messageType;

  @Column(name = "show_predicate", nullable = false)
  private String showPredicate;

  @ManyToOne
  private Character character;

  @ManyToOne(fetch = FetchType.EAGER)
  private Simulation simulation;

  private boolean hasHint;

  @JsonProperty("response_time_limit")
  @Column(name = "response_time_limit")
  private Long responseTimeLimit;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "flow_rules", columnDefinition = "TEXT")
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private JsonNode flowRulesJson;

  // Custom getter and setter for flowRules to handle JsonNode conversion
  public List<FlowRule> getFlowRules() {
    if (flowRulesJson == null || flowRulesJson.isNull()) {
      return new ArrayList<>();
    }
    
    try {
      ObjectMapper mapper = new ObjectMapper();
      
      // Check if the JsonNode contains a string (which means Hibernate loaded JSON as text)
      if (flowRulesJson.isTextual()) {
        // Parse the string as JSON first, then convert to List<FlowRule>
        String jsonString = flowRulesJson.asText();
        return mapper.readValue(jsonString, new TypeReference<List<FlowRule>>() {});
      } else {
        // Handle normal JsonNode case
        return mapper.convertValue(flowRulesJson, new TypeReference<List<FlowRule>>() {});
      }
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }
  
  public void setFlowRules(List<FlowRule> flowRules) {
    if (flowRules == null || flowRules.isEmpty()) {
      this.flowRulesJson = null;
    } else {
      try {
        ObjectMapper mapper = new ObjectMapper();
        this.flowRulesJson = mapper.valueToTree(flowRules);
      } catch (Exception e) {
        this.flowRulesJson = null;
      }
    }
  }

}
