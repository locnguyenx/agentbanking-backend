package com.agentbanking.mock.model;
public record JpnVerifyResponse(String status, String fullName, String dateOfBirth, int age, String amlStatus) {}
