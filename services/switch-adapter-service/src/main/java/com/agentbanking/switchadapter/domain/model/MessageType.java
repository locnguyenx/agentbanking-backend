package com.agentbanking.switchadapter.domain.model;

public enum MessageType {
    MT0100,   // Authorization Request
    MT0110,   // Authorization Response
    MT0400,   // Reversal Request
    MT0410,   // Reversal Response
    ISO20022  // DuitNow transfer
}
