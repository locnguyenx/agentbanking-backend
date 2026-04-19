package com.agentbanking.rules.domain.port.out;

import com.agentbanking.rules.domain.model.VelocityRuleRecord;
import java.util.List;

public interface VelocityRuleRepository {
    List<VelocityRuleRecord> findActiveRules();
    VelocityRuleRecord save(VelocityRuleRecord rule);
}
