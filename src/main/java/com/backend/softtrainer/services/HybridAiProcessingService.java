package com.backend.softtrainer.services;

import com.backend.softtrainer.dtos.aiagent.AiGeneratePlanResponseDto;
import com.backend.softtrainer.entities.Skill;
import com.backend.softtrainer.entities.enums.SkillGenerationStatus;
import com.backend.softtrainer.repositories.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 🔄 Hybrid AI Processing Service - Smart Migration Controller
 * 
 * Intelligently chooses between legacy and enhanced AI processing based on:
 * - Feature flags
 * - Skill characteristics
 * - Organization preferences
 * - Gradual rollout strategy
 * 
 * Maintains 100% backward compatibility while enabling smooth migration
 * to enhanced message types and modern flow execution.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridAiProcessingService {
    
    private final AiPlanProcessingService legacyProcessingService;
    // private final EnhancedAiImportService enhancedProcessingService;  // Temporarily disabled
    private final SkillRepository skillRepository;
    
    @Value("${app.ai-agent.enhanced-processing.enabled:true}")
    private boolean enhancedProcessingEnabled;
    
    @Value("${app.ai-agent.enhanced-processing.rollout-percentage:50}")
    private int enhancedRolloutPercentage;
    
    @Value("${app.ai-agent.enhanced-processing.force-enhanced:false}")
    private boolean forceEnhancedMode;
    
    @Value("${app.ai-agent.enhanced-processing.organization-whitelist:}")
    private String organizationWhitelist;
    
    /**
     * 🎯 Main Processing Method - Smart Route Selection
     */
    @Async("aiAgentTaskExecutor")
    @Transactional
    public CompletableFuture<Void> processAiPlanWithSmartRouting(
            Long skillId, AiGeneratePlanResponseDto aiResponse) {
        
        log.info("🔄 Processing AI plan for skill ID: {} with smart routing", skillId);
        
        try {
            Skill skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new RuntimeException("Skill not found: " + skillId));
            
            boolean useEnhancedProcessing = shouldUseEnhancedProcessing(skill, aiResponse);
            
            if (useEnhancedProcessing) {
                log.info("🚀 Enhanced processing selected but temporarily disabled - using LEGACY for skill: {} (ID: {})", skill.getName(), skillId);
                // return enhancedProcessingService.processEnhancedAiPlan(skillId, aiResponse);  // Temporarily disabled
                return legacyProcessingService.processAiPlanAndCreateSimulations(skillId, aiResponse);
            } else {
                log.info("🔧 Using LEGACY processing for skill: {} (ID: {})", skill.getName(), skillId);
                return legacyProcessingService.processAiPlanAndCreateSimulations(skillId, aiResponse);
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to process AI plan for skill ID: {} - falling back to legacy", skillId, e);
            
            // Fallback to legacy processing on any error
            try {
                return legacyProcessingService.processAiPlanAndCreateSimulations(skillId, aiResponse);
            } catch (Exception fallbackError) {
                log.error("❌ Legacy fallback also failed for skill ID: {}", skillId, fallbackError);
                markSkillAsFailed(skillId);
                throw fallbackError;
            }
        }
    }
    
    /**
     * 🧠 Smart Decision Logic - Enhanced vs Legacy Processing
     */
    private boolean shouldUseEnhancedProcessing(Skill skill, AiGeneratePlanResponseDto aiResponse) {
        
        // 1. Force enhanced mode (for testing/debugging)
        if (forceEnhancedMode) {
            log.debug("🔧 Force enhanced mode enabled");
            return true;
        }
        
        // 2. Enhanced processing globally disabled
        if (!enhancedProcessingEnabled) {
            log.debug("🔧 Enhanced processing globally disabled");
            return false;
        }
        
        // 3. Organization whitelist check (currently disabled - no organization field on Skill)
        // if (isOrganizationWhitelisted(skill)) {
        //     log.debug("🏢 Organization is whitelisted for enhanced processing");
        //     return true;
        // }
        
        // 4. Skill characteristics check
        if (skillHasEnhancedCharacteristics(skill, aiResponse)) {
            log.debug("📊 Skill has characteristics suitable for enhanced processing");
            return true;
        }
        
        // 5. Gradual rollout percentage
        if (isInRolloutPercentage(skill)) {
            log.debug("📈 Skill selected for enhanced processing via rollout percentage ({}%)", 
                    enhancedRolloutPercentage);
            return true;
        }
        
        // 6. Default to legacy
        log.debug("🔧 Defaulting to legacy processing");
        return false;
    }
    
    /**
     * 🏢 Check if organization is whitelisted for enhanced processing
     * Currently disabled since Skill entity doesn't have organization field
     */
    private boolean isOrganizationWhitelisted(Skill skill) {
        // TODO: Implement organization whitelist when organization field is added to Skill
        // For now, return false to use other criteria
        return false;
    }
    
    /**
     * 📊 Check if skill has characteristics suitable for enhanced processing
     */
    private boolean skillHasEnhancedCharacteristics(Skill skill, AiGeneratePlanResponseDto aiResponse) {
        
        // Complex simulations with multiple variables benefit from enhanced processing
        boolean hasComplexSimulations = aiResponse.getSimulations().stream()
                .anyMatch(sim -> sim.getVariables() != null && sim.getVariables().size() > 2);
        
        if (hasComplexSimulations) {
            log.debug("✅ Skill has complex simulations with multiple variables");
            return true;
        }
        
        // Skills with materials benefit from enhanced AI integration
        boolean hasRichMaterials = skill.getMaterials() != null && !skill.getMaterials().isEmpty();
        
        if (hasRichMaterials) {
            log.debug("✅ Skill has materials that can benefit from enhanced processing");
            return true;
        }
        
        // Large number of simulations benefit from enhanced flow management
        boolean hasManySims = aiResponse.getSimulations().size() > 3;
        
        if (hasManySims) {
            log.debug("✅ Skill has many simulations that benefit from enhanced flow management");
            return true;
        }
        
        // Skills with specific keywords benefit from enhanced features
        String skillName = skill.getName().toLowerCase();
        boolean hasEnhancedKeywords = skillName.contains("interactive") || 
                                    skillName.contains("advanced") ||
                                    skillName.contains("ai") ||
                                    skillName.contains("dynamic");
        
        if (hasEnhancedKeywords) {
            log.debug("✅ Skill name suggests enhanced processing would be beneficial");
            return true;
        }
        
        return false;
    }
    
    /**
     * 📈 Check if skill is selected for gradual rollout
     */
    private boolean isInRolloutPercentage(Skill skill) {
        if (enhancedRolloutPercentage <= 0) {
            return false;
        }
        
        if (enhancedRolloutPercentage >= 100) {
            return true;
        }
        
        // Use skill ID for consistent rollout (same skill always gets same result)
        int skillHash = Math.abs(skill.getId().hashCode()) % 100;
        return skillHash < enhancedRolloutPercentage;
    }
    
    /**
     * ❌ Mark skill as failed
     */
    private void markSkillAsFailed(Long skillId) {
        try {
            Skill skill = skillRepository.findById(skillId).orElse(null);
            if (skill != null) {
                skill.setGenerationStatus(SkillGenerationStatus.FAILED);
                skill.setHidden(true);
                skillRepository.save(skill);
                log.info("Marked skill as failed: {} (ID: {})", skill.getName(), skillId);
            }
        } catch (Exception e) {
            log.error("Failed to mark skill as failed: {}", skillId, e);
        }
    }
    
    /**
     * 📊 Get Processing Statistics
     */
    public ProcessingStats getProcessingStats() {
        // Count skills processed with each method in the last 24 hours
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        
        // This would require additional tracking in the database
        // For now, return basic stats
        return ProcessingStats.builder()
                .enhancedProcessingEnabled(enhancedProcessingEnabled)
                .rolloutPercentage(enhancedRolloutPercentage)
                .forceEnhancedMode(forceEnhancedMode)
                .organizationWhitelist(organizationWhitelist)
                .build();
    }
    
    /**
     * 🔧 Update Processing Configuration
     */
    public void updateProcessingConfig(ProcessingConfig config) {
        // This would typically update configuration in database or config service
        log.info("🔧 Processing configuration update requested: {}", config);
        // Implementation would depend on your configuration management strategy
    }
    
    /**
     * 📊 Processing Statistics Data
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingStats {
        private boolean enhancedProcessingEnabled;
        private int rolloutPercentage;
        private boolean forceEnhancedMode;
        private String organizationWhitelist;
        private int enhancedProcessedLast24h;
        private int legacyProcessedLast24h;
        private double enhancedSuccessRate;
        private double legacySuccessRate;
    }
    
    /**
     * ⚙️ Processing Configuration
     */
    @lombok.Builder
    @lombok.Data
    public static class ProcessingConfig {
        private Boolean enableEnhancedProcessing;
        private Integer rolloutPercentage;
        private Boolean forceEnhancedMode;
        private String organizationWhitelist;
    }
} 