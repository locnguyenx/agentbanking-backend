package com.agentbanking.auth.domain.port.out;

import com.agentbanking.auth.domain.model.OtpData;

public interface OtpStore {
    void storeOtp(String username, String hashedOtp, int ttlSeconds);
    OtpData retrieveOtp(String username);
    void deleteOtp(String username);
    void incrementAttempts(String username);
}
