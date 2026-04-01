package com.agentbanking.gateway.integration.setup;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Shared state across E2E test phases.
 * Populated by Phase 1 (AuthSetupTest) and Phase 2 (AgentOnboardingTest).
 * Consumed by Phase 5+ (Transaction tests).
 * 
 * This class is intentionally static - tests run in order and populate it sequentially.
 */
public class TestContext {

    // Gateway URL
    public static final String GATEWAY_URL = System.getenv().getOrDefault(
            "GATEWAY_BASE_URL", "http://localhost:8080");

    // Phase 1: Auth tokens
    public static String adminToken;
    public static String agentToken;
    public static String operatorToken;
    public static String makerToken;
    public static String checkerToken;
    public static String complianceToken;
    public static String tellerToken;
    public static String supervisorToken;

    // Phase 1: User IDs (from auth service)
    public static UUID adminUserId;
    public static UUID agentUserId;
    public static UUID operatorUserId;
    public static UUID makerUserId;
    public static UUID checkerUserId;

    // Phase 2: Agent IDs (from onboarding service)
    public static UUID agentId;
    public static String agentCode;

    // Phase 4: Float balance
    public static BigDecimal agentFloatBalance;

    // Test credentials
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "AdminPass123!";

    public static final String AGENT_USERNAME = "agent001";
    public static final String AGENT_PASSWORD = "AgentPass123!";

    public static final String OPERATOR_USERNAME = "operator001";
    public static final String OPERATOR_PASSWORD = "OperatorPass123!";

    public static final String MAKER_USERNAME = "maker001";
    public static final String MAKER_PASSWORD = "MakerPass123!";

    public static final String CHECKER_USERNAME = "checker001";
    public static final String CHECKER_PASSWORD = "CheckerPass123!";

    public static final String COMPLIANCE_USERNAME = "compliance001";
    public static final String COMPLIANCE_PASSWORD = "CompliancePass123!";

    public static final String TELLER_USERNAME = "teller001";
    public static final String TELLER_PASSWORD = "TellerPass123!";

    public static final String SUPERVISOR_USERNAME = "supervisor001";
    public static final String SUPERVISOR_PASSWORD = "SupervisorPass123!";

    // Agent configuration
    public static final String TEST_AGENT_CODE = "AGT-E2E-001";
    public static final String TEST_AGENT_BUSINESS = "E2E Test Business";
    public static final String TEST_AGENT_TIER = "STANDARD";
    public static final String TEST_AGENT_MYKAD = "900101011234";
    public static final String TEST_AGENT_PHONE = "0123456789";
    public static final double TEST_AGENT_GPS_LAT = 3.1390;
    public static final double TEST_AGENT_GPS_LNG = 101.6869;

    // Transaction test constants
    public static final String MYR_CURRENCY = "MYR";
    public static final String GPS_LAT = "3.1390";
    public static final String GPS_LNG = "101.6869";
    public static final String POS_TERMINAL_ID = "POS-E2E-001";

    /**
     * Check if Phase 1 has been completed (tokens available)
     */
    public static boolean isAuthSetupComplete() {
        return adminToken != null && agentToken != null;
    }

    /**
     * Check if Phase 2 has been completed (agent created)
     */
    public static boolean isAgentOnboardingComplete() {
        return agentId != null;
    }

    /**
     * Reset all context (for cleanup)
     */
    public static void reset() {
        adminToken = null;
        agentToken = null;
        operatorToken = null;
        makerToken = null;
        checkerToken = null;
        complianceToken = null;
        tellerToken = null;
        supervisorToken = null;
        adminUserId = null;
        agentUserId = null;
        operatorUserId = null;
        makerUserId = null;
        checkerUserId = null;
        agentId = null;
        agentCode = null;
        agentFloatBalance = null;
    }
}
