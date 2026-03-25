package com.agentbanking.mock.model;
public record PaynetAuthRequest(String pan, Double amount, String merchantCode) {}