package com.agentbanking.mock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@ConfigurationProperties(prefix = "mock")
public class MockConfig {
    private PaynetConfig paynet = new PaynetConfig();
    private JpnConfig jpn = new JpnConfig();
    private HsmConfig hsm = new HsmConfig();
    private BillerConfig billers = new BillerConfig();

    public PaynetConfig getPaynet() { return paynet; }
    public void setPaynet(PaynetConfig paynet) { this.paynet = paynet; }
    public JpnConfig getJpn() { return jpn; }
    public void setJpn(JpnConfig jpn) { this.jpn = jpn; }
    public HsmConfig getHsm() { return hsm; }
    public void setHsm(HsmConfig hsm) { this.hsm = hsm; }
    public BillerConfig getBillers() { return billers; }
    public void setBillers(BillerConfig billers) { this.billers = billers; }

    public static class PaynetConfig {
        private String defaultResponse = "APPROVE";
        private List<String> declineCodes = List.of();
        private int latencyMs = 200;

        public String getDefaultResponse() { return defaultResponse; }
        public void setDefaultResponse(String defaultResponse) { this.defaultResponse = defaultResponse; }
        public List<String> getDeclineCodes() { return declineCodes; }
        public void setDeclineCodes(List<String> declineCodes) { this.declineCodes = declineCodes; }
        public int getLatencyMs() { return latencyMs; }
        public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    }

    public static class JpnConfig {
        private String defaultMatch = "MATCH";
        private String amlDefault = "CLEAN";

        public String getDefaultMatch() { return defaultMatch; }
        public void setDefaultMatch(String defaultMatch) { this.defaultMatch = defaultMatch; }
        public String getAmlDefault() { return amlDefault; }
        public void setAmlDefault(String amlDefault) { this.amlDefault = amlDefault; }
    }

    public static class HsmConfig {
        private String pinValidation = "VALID";

        public String getPinValidation() { return pinValidation; }
        public void setPinValidation(String pinValidation) { this.pinValidation = pinValidation; }
    }

    public static class BillerConfig {
        private String defaultValidation = "VALID";
        private int latencyMs = 500;

        public String getDefaultValidation() { return defaultValidation; }
        public void setDefaultValidation(String defaultValidation) { this.defaultValidation = defaultValidation; }
        public int getLatencyMs() { return latencyMs; }
        public void setLatencyMs(int latencyMs) { this.latencyMs = latencyMs; }
    }
}