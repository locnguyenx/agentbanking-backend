package com.agentbanking.common.messaging;

public final class KafkaTopics {

    public static final String LEDGER_TRANSACTIONS = "ledger.transactions";
    public static final String LEDGER_REVERSALS = "ledger.reversals";
    public static final String SMS_NOTIFICATIONS = "sms.notifications";
    public static final String EFM_EVENTS = "efm.events";
    public static final String COMMISSION_EVENTS = "commission.events";
    public static final String AUDIT_LOGS = "audit-logs";

    private KafkaTopics() {
    }
}
