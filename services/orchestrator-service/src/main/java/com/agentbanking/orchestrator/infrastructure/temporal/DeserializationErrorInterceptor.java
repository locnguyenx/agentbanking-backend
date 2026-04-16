package com.agentbanking.orchestrator.infrastructure.temporal;

import io.temporal.common.interceptors.ActivityInboundCallsInterceptor;
import io.temporal.common.interceptors.ActivityInboundCallsInterceptorBase;
import io.temporal.common.interceptors.WorkerInterceptorBase;
import io.temporal.failure.ApplicationFailure;
import io.temporal.common.converter.DataConverterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Temporal Worker Interceptor that catches deserialization errors and fails fast.
 * Prevents infinite retry loops when activity input cannot be deserialized.
 */
public class DeserializationErrorInterceptor extends WorkerInterceptorBase {

    private static final Logger log = LoggerFactory.getLogger(DeserializationErrorInterceptor.class);

    @Override
    public ActivityInboundCallsInterceptor interceptActivity(ActivityInboundCallsInterceptor next) {
        return new DeserializationErrorActivityInterceptor(next);
    }

    /**
     * Activity interceptor that catches DataConverterException and wraps it in a non-retryable failure.
     */
    private static class DeserializationErrorActivityInterceptor extends ActivityInboundCallsInterceptorBase {
        
        DeserializationErrorActivityInterceptor(ActivityInboundCallsInterceptor next) {
            super(next);
        }

        @Override
        public ActivityOutput execute(ActivityInput input) {
            try {
                return super.execute(input);
            } catch (DataConverterException e) {
                log.error("Deserialization error in activity: {}. Failing fast to prevent infinite retries.",
                        e.getMessage());

                // Wrap in ApplicationFailure with non-retryable flag
                throw ApplicationFailure.newNonRetryableFailure(
                        "Deserialization error: " + e.getMessage(),
                        "ERR_DESERIALIZATION_FAILED",
                        e
                );
            }
        }
    }
}
