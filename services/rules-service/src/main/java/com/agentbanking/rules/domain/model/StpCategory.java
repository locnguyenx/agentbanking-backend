package com.agentbanking.rules.domain.model;

/**
 * STP category classification
 */
public enum StpCategory {
    FULL_STP,        // 100% automated
    CONDITIONAL_STP, // Rules engine, fallback to manual
    NON_STP          // Manual maker-checker required
}
