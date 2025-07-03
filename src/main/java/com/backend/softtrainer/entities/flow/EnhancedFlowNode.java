package com.backend.softtrainer.entities.flow;

import com.backend.softtrainer.entities.Character;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.enums.MessageType;
import com.backend.softtrainer.entities.enums.InteractionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.ArrayList;

/**
 * Enhanced FlowNode that can handle complex predefined simulation structures
 * Supports both legacy show_predicate and new structured rules
 */
@Entity(name = "enhanced_flow_nodes")
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(exclude = {"character", "simulation"})
public class EnhancedFlowNode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // Core identification
    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @ElementCollection
    @CollectionTable(name = "flow_node_dependencies")
    @Column(name = "previous_message_id")
    private List<Long> previousMessageIds;

    // Content & Type
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type")
    private InteractionType interactionType;

    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    // Character & Simulation relationships
    @ManyToOne
    @JoinColumn(name = "character_id")
    private Character character;

    @Column(name = "character_id_raw") // For character_id: -1 (user)
    private Integer characterIdRaw;

    @ManyToOne(fetch = FetchType.LAZY)
    private Simulation simulation;

    // Options & Answers (for questions)
    @Column(name = "options", columnDefinition = "TEXT") // Use TEXT for H2 compatibility
    private JsonNode options; // Store as JSON array

    @Column(name = "correct_answer_position")
    private Integer correctAnswerPosition;

    @Builder.Default
    @Column(name = "has_hint")
    private boolean hasHint = false;

    @Column(name = "response_time_limit")
    private Long responseTimeLimit;

    // Legacy predicate system (for migration)
    @Deprecated
    @Column(name = "show_predicate", columnDefinition = "TEXT")
    private String showPredicate;

    // New structured rule system
    @Column(name = "flow_rules", columnDefinition = "TEXT") // Use TEXT for H2 compatibility
    private JsonNode flowRules;

    @Column(name = "hyperparameter_actions", columnDefinition = "TEXT") // Use TEXT for H2 compatibility
    private JsonNode hyperparameterActions;

    // Node ordering and flow
    @Column(name = "order_number")
    private Long orderNumber;

    // Helper methods
    public boolean isUserMessage() {
        return characterIdRaw != null && characterIdRaw == -1;
    }

    public boolean isActionable() {
        return interactionType != null && interactionType.isActionable();
    }

    public boolean hasComplexPredicate() {
        return showPredicate != null && 
               (showPredicate.contains("saveChatValue") || 
                showPredicate.contains("readChatValue"));
    }

    public List<String> getOptionsAsList() {
        if (options == null) return List.of();
        
        try {
            List<String> result = new ArrayList<>();
            if (options.isArray()) {
                for (JsonNode option : options) {
                    result.add(option.asText());
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }
} 