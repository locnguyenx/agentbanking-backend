package com.agentbanking.rules.integration;

import com.agentbanking.common.test.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class RulesControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void calculateFees_shouldReturnFees() throws Exception {
        String configBody = """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentTier": "STANDARD",
                "feeType": "FIXED",
                "customerFeeValue": "1.00",
                "agentCommissionValue": "0.20",
                "bankShareValue": "0.80",
                "dailyLimitAmount": "10000.00",
                "dailyLimitCount": 10,
                "effectiveFrom": "2024-01-01"
            }
            """;
        mockMvc.perform(post("/internal/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/internal/fees/calculate")
                        .param("transactionType", "CASH_WITHDRAWAL")
                        .param("agentTier", "STANDARD")
                        .param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerFee").exists())
                .andExpect(jsonPath("$.agentCommission").exists())
                .andExpect(jsonPath("$.bankShare").exists());
    }

    @Test
    void calculateFees_withMultipleConfigs_shouldReturnMostRecent() throws Exception {
        String oldConfig = """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentTier": "PREMIER",
                "feeType": "FIXED",
                "customerFeeValue": "1.00",
                "agentCommissionValue": "0.20",
                "bankShareValue": "0.80",
                "dailyLimitAmount": "10000.00",
                "dailyLimitCount": 10,
                "effectiveFrom": "2024-01-01"
            }
            """;
        mockMvc.perform(post("/internal/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(oldConfig))
                .andExpect(status().is2xxSuccessful());

        String newConfig = """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentTier": "PREMIER",
                "feeType": "FIXED",
                "customerFeeValue": "2.00",
                "agentCommissionValue": "0.40",
                "bankShareValue": "1.60",
                "dailyLimitAmount": "10000.00",
                "dailyLimitCount": 10,
                "effectiveFrom": "2026-04-07"
            }
            """;
        mockMvc.perform(post("/internal/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(newConfig))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/internal/fees/calculate")
                        .param("transactionType", "CASH_WITHDRAWAL")
                        .param("agentTier", "PREMIER")
                        .param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerFee").value(2.00))
                .andExpect(jsonPath("$.agentCommission").value(0.40));
    }

    @Test
    void getFeeConfig_shouldReturnConfig() throws Exception {
        mockMvc.perform(get("/internal/fees/{transactionType}/{agentTier}", "CASH_WITHDRAWAL", "STANDARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("CASH_WITHDRAWAL"))
                .andExpect(jsonPath("$.agentTier").value("STANDARD"));
    }

    @Test
    void createFeeConfig_withValidData_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "transactionType": "PIN_PURCHASE",
                "agentTier": "PREMIER",
                "feeType": "FIXED",
                "customerFeeValue": "1.00",
                "agentCommissionValue": "0.20",
                "bankShareValue": "0.80",
                "dailyLimitAmount": "10000.00",
                "dailyLimitCount": 10,
                "effectiveFrom": "2028-01-01"
            }
            """;

        mockMvc.perform(post("/internal/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.feeConfigId").exists());
    }

    @Test
    void createFeeConfig_withInvalidData_shouldReturn400() throws Exception {
        String requestBody = """
            {
                "transactionType": "INVALID_TYPE",
                "agentTier": "STANDARD",
                "feeType": "FIXED"
            }
            """;

        mockMvc.perform(post("/internal/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void checkVelocity_shouldReturnResult() throws Exception {
        String requestBody = """
            {
                "transactionCountToday": 5,
                "amountToday": "1000.00"
            }
            """;

        mockMvc.perform(post("/internal/check-velocity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passed").isBoolean());
    }

    @Test
    void getLimits_shouldReturnLimits() throws Exception {
        mockMvc.perform(get("/internal/limits/{transactionType}/{agentTier}", "CASH_WITHDRAWAL", "STANDARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("CASH_WITHDRAWAL"))
                .andExpect(jsonPath("$.dailyLimitAmount").exists());
    }

    @Test
    void evaluateStp_shouldReturnEvaluation() throws Exception {
        String requestBody = """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "customerMykad": "880101011234",
                "amount": "500.00",
                "agentTier": "STANDARD",
                "transactionCountToday": 1,
                "amountToday": "500.00"
            }
            """;

        mockMvc.perform(post("/internal/stp/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").exists())
                .andExpect(jsonPath("$.approved").isBoolean());
    }

    @Test
    void checkMicroAutoApproval_shouldReturnEligibility() throws Exception {
        mockMvc.perform(get("/internal/stp/micro-auto-approval")
                        .param("agentTier", "MICRO")
                        .param("amount", "500.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").isBoolean());
    }

    @Test
    void getQuote_shouldReturnQuote() throws Exception {
        String configBody = """
            {
                "transactionType": "CASH_WITHDRAWAL",
                "agentTier": "STANDARD",
                "feeType": "FIXED",
                "customerFeeValue": "1.00",
                "agentCommissionValue": "0.20",
                "bankShareValue": "0.80",
                "dailyLimitAmount": "10000.00",
                "dailyLimitCount": 10,
                "effectiveFrom": "2026-01-01"
            }
            """;
        mockMvc.perform(post("/internal/fees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(configBody))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(post("/internal/transactions/quote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "serviceCode": "CASH_WITHDRAWAL",
                                "amount": "100.00",
                                "fundingSource": "CARD_EMV"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quoteId").exists())
                .andExpect(jsonPath("$.amount").value("100.00"))
                .andExpect(jsonPath("$.fee").exists())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.commission").exists());
    }
}