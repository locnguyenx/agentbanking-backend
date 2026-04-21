package com.agentbanking.switchadapter.domain.model;

/**
 * ABOUTME: Defines the standard transaction statuses across the switch adapter.
 * ABOUTME: Aligned with OpenAPI specification SUCCESS/FAILED/PENDING/REVERSED.
 */
public enum SwitchStatus {
    SUCCESS,
    FAILED,
    PENDING,
    REVERSED,
    APPROVED, // Deprecated, use SUCCESS
    DECLINED  // Deprecated, use FAILED
}
