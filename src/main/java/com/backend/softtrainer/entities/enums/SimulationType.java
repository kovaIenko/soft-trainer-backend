package com.backend.softtrainer.entities.enums;

/**
 * Enumeration for simulation types
 * 
 * - PREDEFINED: Traditional simulations with predefined nodes (legacy or modern format)
 * - AI_GENERATED: Real-time AI-generated simulations without predefined nodes
 */
public enum SimulationType {
    PREDEFINED,     // Legacy + Modern predefined simulations with nodes
    AI_GENERATED    // Real-time AI-generated simulations
} 