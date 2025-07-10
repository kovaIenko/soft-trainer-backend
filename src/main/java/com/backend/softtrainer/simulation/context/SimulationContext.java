package com.backend.softtrainer.simulation.context;

import com.backend.softtrainer.entities.Chat;
import com.backend.softtrainer.entities.Simulation;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.User;
import com.backend.softtrainer.entities.messages.Message;
import com.backend.softtrainer.entities.enums.SimulationMode;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 🎯 Enhanced Simulation Context - Complete execution context for simulations
 *
 * This context contains all the information needed to execute simulations:
 * - Chat and user information
 * - Message history and current state
 * - Hyperparameters and learning objectives
 * - Simulation mode and configuration
 * - Completion and state tracking
 */
@Data
@Builder
@Slf4j
public class SimulationContext {

    // Core entities
    @Getter
    private final Long chatId;
    private final Chat chat;
    private final User user;
    private final Simulation simulation;
    private final Skill skill;

    // Simulation configuration
    @Builder.Default
    private SimulationMode simulationMode = SimulationMode.PREDEFINED;

    @Builder.Default
    private Integer maxMessages = 100;

    // Message management
    @Builder.Default
    private final List<Message> messageHistory = new CopyOnWriteArrayList<>();

    // Hyperparameters and learning state
    @Builder.Default
    private final Map<String, Double> hyperParameters = new ConcurrentHashMap<>();

    @Builder.Default
    private final List<String> learningObjectives = new CopyOnWriteArrayList<>();

    // State tracking
    @Builder.Default
    private Double hearts = 5.0;

    @Builder.Default
    private boolean markedAsCompleted = false;

    @Builder.Default
    private long startTime = System.currentTimeMillis();

    /**
     * 📝 Add a message to the conversation history
     */
    public void addMessage(Message message) {
        if (message != null) {
            messageHistory.add(message);
            log.debug("📝 Added message {} to context. Total messages: {}",
                message.getId(), messageHistory.size());
        }
    }

    /**
     * 📊 Get current message count
     */
    public int getMessageCount() {
        return messageHistory.size();
    }

    /**
     * 🎯 Get learning objectives for this simulation
     */
    public List<String> getSimulationLearningObjectives() {
        if (!learningObjectives.isEmpty()) {
            return learningObjectives;
        }

        // Extract from skill or simulation metadata if available
        // For now, return common soft skills
        return List.of(
            "active_listening",
            "empathy",
            "engagement",
            "collaboration",
            "feedback_delivery"
        );
    }

    /**
     * 📈 Get hyperparameter value
     */
    public Double getHyperParameter(String key) {
        return hyperParameters.getOrDefault(key, 0.0);
    }

    /**
     * 📈 Set hyperparameter value
     */
    public void setHyperParameter(String key, Double value) {
        hyperParameters.put(key, value);
        log.debug("📈 Updated hyperparameter {}: {}", key, value);
    }

    /**
     * 📈 Increment hyperparameter value
     */
    public void incrementHyperParameter(String key, Double increment) {
        Double currentValue = getHyperParameter(key);
        setHyperParameter(key, currentValue + increment);
    }

    /**
     * 🏁 Mark simulation as completed
     */
    public void markAsCompleted() {
        this.markedAsCompleted = true;
        log.info("🏁 Simulation {} marked as completed", chatId);
    }

    /**
     * ❤️ Update hearts count
     */
    public void updateHearts(Double newHearts) {
        this.hearts = newHearts;
        log.debug("❤️ Hearts updated to: {}", hearts);
    }

    /**
     * ⏱️ Get simulation duration in seconds
     */
    public long getDurationSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    /**
     * 📋 Get the last message in the conversation
     */
    public Message getLastMessage() {
        return messageHistory.isEmpty() ? null :
            messageHistory.get(messageHistory.size() - 1);
    }
    /**
     * 👤 Get user selections for a specific message (placeholder)
     */
    public List<Integer> getUserSelections(Long messageId) {
        return List.of(); // TODO: Implement proper user response tracking
    }

    /**
     * 💬 Get user text input for a specific message (placeholder)
     */
    public String getUserTextInput(Long messageId) {
        return null; // TODO: Implement proper user text tracking
    }

    /**
     * 🎯 Check if this is a legacy simulation
     */
    public boolean isLegacySimulation() {
        return simulation != null &&
               simulation.getNodes() != null &&
               !simulation.getNodes().isEmpty();
    }

    /**
     * 🔍 Get simulation metadata
     */
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new ConcurrentHashMap<>();
        metadata.put("chatId", chatId);
        metadata.put("simulationMode", simulationMode);
        metadata.put("messageCount", getMessageCount());
        metadata.put("hearts", hearts);
        metadata.put("durationSeconds", getDurationSeconds());
        metadata.put("isCompleted", markedAsCompleted);
        return metadata;
    }

    /**
     * 🎯 Create a copy of this context
     */
    public SimulationContext copy() {
        return SimulationContext.builder()
            .chatId(this.chatId)
            .chat(this.chat)
            .user(this.user)
            .simulation(this.simulation)
            .skill(this.skill)
            .simulationMode(this.simulationMode)
            .maxMessages(this.maxMessages)
            .hearts(this.hearts)
            .markedAsCompleted(this.markedAsCompleted)
            .startTime(this.startTime)
            .build()
            .withCopiedData(this);
    }

    private SimulationContext withCopiedData(SimulationContext source) {
        this.messageHistory.addAll(source.messageHistory);
        this.hyperParameters.putAll(source.hyperParameters);
        this.learningObjectives.addAll(source.learningObjectives);
        return this;
    }

  public Long getSimulationId() { return simulation != null ? simulation.getId() : null; }
    public Long getSkillId() { return skill != null ? skill.getId() : null; }
    public String getSkillName() { return skill != null ? skill.getName() : "Unknown"; }
    public String getOrganizationLocalization() {
        return user != null && user.getOrganization() != null ?
            user.getOrganization().getLocalization() : "en";
    }
}
