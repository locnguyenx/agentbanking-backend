package com.agentbanking.switchadapter.application.usecase;

import com.agentbanking.common.security.ErrorCodes;
import com.agentbanking.switchadapter.domain.port.in.ProxyEnquiryUseCase.ProxyEnquiryResult;
import com.agentbanking.switchadapter.domain.port.out.DuitNowProxyGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyEnquiryUseCaseTest {

    @Mock
    private DuitNowProxyGateway duitNowProxyGateway;

    private ProxyEnquiryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProxyEnquiryUseCaseImpl(duitNowProxyGateway);
    }

    @Test
    void enquiryProxy_shouldReturnNameForValidProxy() {
        String proxyId = "60123456789";
        String proxyType = "MOBILE";
        when(duitNowProxyGateway.resolveProxy(proxyId, proxyType))
            .thenReturn("AHMAD BIN ABDULLAH");

        ProxyEnquiryResult result = useCase.enquiryProxy(proxyId, proxyType);

        assertEquals("AHMAD BIN ABDULLAH", result.name());
        assertEquals(proxyType, result.proxyType());
    }

    @Test
    void enquiryProxy_shouldThrowWhenProxyNotFound() {
        when(duitNowProxyGateway.resolveProxy("invalid", "MOBILE"))
            .thenThrow(new IllegalArgumentException(ErrorCodes.ERR_BIZ_PROXY_NOT_FOUND));

        assertThrows(IllegalArgumentException.class, () ->
            useCase.enquiryProxy("invalid", "MOBILE")
        );
    }

    @Test
    void enquiryProxy_shouldThrowWhenDownstreamFails() {
        when(duitNowProxyGateway.resolveProxy(any(), any()))
            .thenThrow(new RuntimeException("Switch unavailable"));

        assertThrows(IllegalStateException.class, () ->
            useCase.enquiryProxy("60123456789", "MOBILE")
        );
    }
}
