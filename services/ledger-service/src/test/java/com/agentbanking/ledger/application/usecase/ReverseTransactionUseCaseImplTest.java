package com.agentbanking.ledger.application.usecase;

import com.agentbanking.ledger.infrastructure.messaging.ReversalEvent;
import com.agentbanking.ledger.infrastructure.messaging.ReversalEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReverseTransactionUseCaseImplTest {

    @Mock
    private ReversalEventPublisher reversalEventPublisher;

    private ReverseTransactionUseCaseImpl useCase;

    private UUID transactionId;

    @BeforeEach
    void setUp() {
        useCase = new ReverseTransactionUseCaseImpl(reversalEventPublisher);
        transactionId = UUID.randomUUID();
    }

    @Test
    void reverseTransaction_publishesReversalEventAfterSuccess() {
        Map<String, Object> result = useCase.reverseTransaction(transactionId);

        assertEquals("REVERSED", result.get("status"));
        assertEquals(transactionId.toString(), result.get("transactionId"));

        ArgumentCaptor<ReversalEvent> eventCaptor = ArgumentCaptor.forClass(ReversalEvent.class);
        verify(reversalEventPublisher).publish(eventCaptor.capture());

        ReversalEvent publishedEvent = eventCaptor.getValue();
        assertEquals("TRANSACTION_REVERSAL", publishedEvent.eventType());
        assertEquals(transactionId, publishedEvent.originalTransactionId());
        assertNotNull(publishedEvent.reversalTransactionId());
        assertNotNull(publishedEvent.timestamp());
    }
}
