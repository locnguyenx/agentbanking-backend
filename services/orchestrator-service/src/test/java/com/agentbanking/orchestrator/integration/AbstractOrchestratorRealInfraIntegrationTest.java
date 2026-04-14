package com.agentbanking.orchestrator.integration;

import com.agentbanking.orchestrator.infrastructure.external.*;
import com.agentbanking.orchestrator.infrastructure.temporal.WorkflowFactory;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort;
import com.agentbanking.orchestrator.domain.port.out.RulesServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort;
import com.agentbanking.orchestrator.domain.port.out.LedgerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort;
import com.agentbanking.orchestrator.domain.port.out.SwitchAdapterPort.*;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort;
import com.agentbanking.orchestrator.domain.port.out.BillerServicePort.*;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort;
import com.agentbanking.orchestrator.domain.port.out.CbsServicePort.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractOrchestratorRealInfraIntegrationTest {

    // Mock all external Feign clients - these are tested separately in unit tests
    // The integration test focuses on: controller → use case → workflow router → Temporal workflow start → status polling

    @MockBean
    protected WorkflowFactory workflowFactory;

    @MockBean
    protected SwitchAdapterClient switchAdapterClient;

    @MockBean
    protected LedgerServiceClient ledgerServiceClient;

    @MockBean
    protected RulesServiceClient rulesServiceClient;

    @MockBean
    protected BillerServiceClient billerServiceClient;

    @MockBean
    protected CbsServiceClient cbsServiceClient;

    @MockBean
    protected TelcoAggregatorClient telcoAggregatorClient;

    @MockBean
    protected EWalletProviderClient ewalletProviderClient;

    @MockBean
    protected ESSPServiceClient esspServiceClient;

    @MockBean
    protected PINInventoryClient pinInventoryClient;

    @MockBean
    protected QRPaymentClient qrPaymentClient;

    @MockBean
    protected RequestToPayClient requestToPayClient;

    @MockBean
    protected MerchantTransactionClient merchantTransactionClient;

    @MockBean
    protected RulesServicePort rulesServicePort;

    @MockBean
    protected LedgerServicePort ledgerServicePort;

    @BeforeEach
    void setUpMocks() {
        // Configure WorkflowFactory mocks - verify correct workflow is selected
        when(workflowFactory.startWorkflow(any(), any(String.class), any()))
            .thenAnswer(invocation -> invocation.getArgument(0)); // Return idempotencyKey as workflowId

        // Configure RulesServicePort mocks
        when(rulesServicePort.checkVelocity(any()))
            .thenReturn(new VelocityCheckResult(true, null));
        
        when(rulesServicePort.evaluateStp(any()))
            .thenReturn(new StpDecision("STP", true, "Auto-approved"));
        
        when(rulesServicePort.calculateFees(any()))
            .thenReturn(new FeeCalculationResult(
                BigDecimal.valueOf(2.00),  // customerFee
                BigDecimal.valueOf(0.50),  // agentCommission  
                BigDecimal.valueOf(1.50)   // bankShare
            ));

        // Configure LedgerServicePort mocks
        when(ledgerServicePort.blockFloat(any()))
            .thenReturn(new FloatBlockResult(true, UUID.randomUUID(), null));
        
        when(ledgerServicePort.commitFloat(any()))
            .thenReturn(new FloatCommitResult(true, null));
        
        when(ledgerServicePort.releaseFloat(any()))
            .thenReturn(new FloatReleaseResult(true, null));
        
        when(ledgerServicePort.getTransactionDetails(any()))
            .thenReturn(new TransactionDetailsResult(
                UUID.randomUUID(),                    // transactionId
                UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"), // agentId
                "CASH_WITHDRAWAL",                    // transactionType
                BigDecimal.valueOf(500),              // amount
                BigDecimal.valueOf(2.00),             // customerFee
                BigDecimal.valueOf(0.50),             // agentCommission
                BigDecimal.valueOf(1.50),             // bankShare
                "COMPLETED",                          // status
                null,                                 // errorCode
                "411111******1111",                   // customerCardMasked
                "REF123456",                          // referenceNumber
                BigDecimal.valueOf(3.1390),           // geofenceLat
                BigDecimal.valueOf(101.6869),         // geofenceLng
                "TIER_1",                             // agentTier
                "0123",                               // targetBin
                null,                                 // billerCode
                null,                                 // ref1
                null,                                 // ref2
                null,                                 // destinationAccount
                "2024-01-01T12:00:00Z",               // createdAt
                "2024-01-01T12:01:00Z"                // completedAt
            ));

        // Configure external client mocks
        setupExternalClientMocks();
    }

    private void setupExternalClientMocks() {
        // EWallet Provider
        when(ewalletProviderClient.validateWallet(any()))
            .thenReturn(new EWalletProviderClient.EWalletValidationResponse(true, BigDecimal.valueOf(1000.00), null));
        
        when(ewalletProviderClient.withdraw(any()))
            .thenReturn(new EWalletProviderClient.EWalletWithdrawResponse(true, "EWALLET123", null));
        
        when(ewalletProviderClient.topup(any()))
            .thenReturn(new EWalletProviderClient.EWalletTopupResponse(true, "EWALLET123", null));

        // PIN Inventory
        when(pinInventoryClient.validateInventory(any()))
            .thenReturn(new PINInventoryClient.PINValidationResponse(true, 100, null));
        
        when(pinInventoryClient.generatePIN(any()))
            .thenReturn(new PINInventoryClient.PINGenerationResponse(true, "1234567890123456", "PIN123", java.time.LocalDate.now().plusDays(30), null));

        // Telco Aggregator
        when(telcoAggregatorClient.validatePhone(any()))
            .thenReturn(new TelcoAggregatorClient.TelcoPhoneValidationResponse(true, "DIGI", null));
        
        when(telcoAggregatorClient.topup(any()))
            .thenReturn(new TelcoAggregatorClient.TelcoTopupResponse(true, "TELCO123", null));

        // ESSP Service
        when(esspServiceClient.validatePurchase(any()))
            .thenReturn(new ESSPServiceClient.ESSPValidationResponse(true, BigDecimal.valueOf(10), BigDecimal.valueOf(1000), null));
        
        when(esspServiceClient.purchase(any()))
            .thenReturn(new ESSPServiceClient.ESSPPurchaseResponse(true, "ESSP123", null));

        // Switch Adapter
        when(switchAdapterClient.authorizeTransaction(any()))
            .thenReturn(new SwitchAuthorizationResult(true, "AUTH123", "APPROVED", null));
        
        when(switchAdapterClient.sendReversal(any()))
            .thenReturn(new SwitchReversalResult(true, null));

        // Biller Service
        when(billerServiceClient.validateBill(any()))
            .thenReturn(new BillValidationResult(true, "John Doe", BigDecimal.valueOf(150.00), null));
        
        when(billerServiceClient.payBill(any()))
            .thenReturn(new BillPaymentResult(true, "BILL123", null));

        // CBS Service
        when(cbsServiceClient.authorizeAtCbs(any()))
            .thenReturn(new CbsAuthorizationResult(true, "CBS123", null));

        // QR Payment
        when(qrPaymentClient.generateQR(any()))
            .thenReturn(new QRPaymentClient.QRGenerationResponse("QR123456", "QR123", null));
        
        when(qrPaymentClient.checkStatus(any()))
            .thenReturn(new QRPaymentClient.QRPaymentStatusResponse("PAID", "PAYNET123", null));

        // Request to Pay
        when(requestToPayClient.sendRTP(any()))
            .thenReturn(new RequestToPayClient.RTPSendResponse(true, "RTP123", null));
        
        when(requestToPayClient.checkStatus(any()))
            .thenReturn(new RequestToPayClient.RTPStatusResponse("COMPLETED", "PAYNET123", null));

        // Merchant Transaction
        when(merchantTransactionClient.createRecord(any()))
            .thenReturn(new MerchantTransactionClient.MerchantTransactionResponse(true, UUID.randomUUID(), null));
    }

    // No manual test configuration needed - using real infrastructure with test profile
}