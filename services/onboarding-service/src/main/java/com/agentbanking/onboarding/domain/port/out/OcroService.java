package com.agentbanking.onboarding.domain.port.out;

/**
 * Service for extracting name from MyKad via OCR
 */
public interface OcroService {
    String extractNameFromMyKad(String mykadNumber);
}