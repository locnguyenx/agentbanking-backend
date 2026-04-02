package com.agentbanking.gateway.filter;

import com.agentbanking.gateway.transform.Transformers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gateway filter factory for transforming external API requests/responses
 * to internal service format. Referenced in application.yaml as:
 *   filters:
 *     - RequestTransform=withdrawal
 *     - RequestTransform=deposit
 */
@Component
public class RequestTransformGatewayFilterFactory 
        extends AbstractGatewayFilterFactory<RequestTransformGatewayFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(RequestTransformGatewayFilterFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public RequestTransformGatewayFilterFactory() {
        super(Config.class);
        log.info("RequestTransformGatewayFilterFactory initialized");
    }

    @Override
    public GatewayFilter apply(Config config) {
        String type = config.getType();
        log.info("RequestTransform filter created for type: {}", type);
        
        GatewayFilter delegate = new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
                ServerHttpRequest request = exchange.getRequest();
                String agentId = request.getHeaders().getFirst("X-Agent-Id");
                
                if (agentId == null || agentId.isBlank()) {
                    return errorResponse(exchange, "Missing X-Agent-Id header", "ERR_AUTH_MISSING_AGENT");
                }
                
                log.info("Transform [{}]: agentId={}, path={}", type, agentId, request.getURI().getPath());
                
                // Handle request body transformation
                return DataBufferUtils.join(request.getBody())
                    .flatMap(dataBuffer -> {
                        String body;
                        Map<String, Object> input;
                        try {
                            ByteBuffer buffer = dataBuffer.toByteBuffer();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            DataBufferUtils.release(dataBuffer);
                            
                            body = new String(bytes, StandardCharsets.UTF_8);
                            log.debug("Transform [{}]: input body: {}", type, body);
                            
                            input = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                        } catch (Exception e) {
                            log.error("Transform [{}]: Failed to parse request body: {}", type, e.getMessage());
                            DataBufferUtils.release(dataBuffer);
                            return errorResponse(exchange, "Invalid JSON: " + e.getMessage(), "ERR_VAL_INVALID_JSON");
                        }
                        
                        // Transform request
                        Map<String, Object> transformed;
                        try {
                            transformed = transformRequest(input, type, agentId);
                        } catch (Exception e) {
                            log.error("Transform [{}]: Transformation failed: {}", type, e.getMessage());
                            return errorResponse(exchange, "Transformation failed: " + e.getMessage(), "ERR_VAL_INVALID_REQUEST");
                        }
                        
                        String transformedBody;
                        try {
                            transformedBody = mapper.writeValueAsString(transformed);
                        } catch (Exception e) {
                            return errorResponse(exchange, "Failed to serialize transformed request", "ERR_SYS_INTERNAL");
                        }
                        
                        log.debug("Transform [{}]: output body: {}", type, transformedBody);
                        
                        byte[] outputBytes = transformedBody.getBytes(StandardCharsets.UTF_8);
                        DataBuffer outputBuffer = exchange.getResponse().bufferFactory().wrap(outputBytes);
                        
                        // Log the URI before creating the decorator
                        log.info("Transform [{}]: original URI: {}", type, request.getURI());
                        
                        ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.just(outputBuffer);
                            }
                            
                            @Override
                            public HttpHeaders getHeaders() {
                                // Update Content-Length header
                                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                                headers.putAll(super.getHeaders());
                                headers.setContentLength(outputBytes.length);
                                headers.setContentType(MediaType.APPLICATION_JSON);
                                return headers;
                            }
                        };
                        
                        // Create response decorator for response transformation
                        ServerWebExchange mutatedExchange = exchange.mutate().request(decorator).build();
                        
                        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(
                            mutatedExchange.getResponse()) {
                            @Override
                            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                                return DataBufferUtils.join(Flux.from(body))
                                    .flatMap(dataBuffer -> {
                                        try {
                                            ByteBuffer buffer = dataBuffer.toByteBuffer();
                                            byte[] bytes = new byte[buffer.remaining()];
                                            buffer.get(bytes);
                                            DataBufferUtils.release(dataBuffer);
                                            
                                            String responseBody = new String(bytes, StandardCharsets.UTF_8);
                                            log.debug("Response [{}]: internal response: {}", type, responseBody);
                                            
                                            Map<String, Object> internalResponse;
                                            try {
                                                internalResponse = mapper.readValue(responseBody, 
                                                    new TypeReference<Map<String, Object>>() {});
                                            } catch (Exception e) {
                                                // If response is not JSON, pass through
                                                DataBuffer original = exchange.getResponse().bufferFactory()
                                                    .wrap(bytes);
                                                return super.writeWith(Flux.just(original));
                                            }
                                            
                                            // Transform response
                                            Map<String, Object> transformedResponse = transformResponse(
                                                internalResponse, type, agentId);
                                            
                                            String transformedResponseBody = mapper.writeValueAsString(transformedResponse);
                                            log.debug("Response [{}]: external response: {}", type, transformedResponseBody);
                                            
                                            byte[] transformedBytes = transformedResponseBody.getBytes(StandardCharsets.UTF_8);
                                            DataBuffer transformedBuffer = exchange.getResponse().bufferFactory()
                                                .wrap(transformedBytes);
                                            
                                            return super.writeWith(Flux.just(transformedBuffer));
                                        } catch (Exception e) {
                                            log.error("Response transformation failed: {}", e.getMessage());
                                            // Pass through original response on error
                                            return super.writeWith(body);
                                        }
                                    });
                            }
                        };
                        
                        return chain.filter(mutatedExchange.mutate().response(responseDecorator).build());
                    });
            }
        };
        
        return new OrderedGatewayFilter(delegate, Ordered.HIGHEST_PRECEDENCE + 1);
    }

    /**
     * Transform external API request to internal format
     */
    private Map<String, Object> transformRequest(Map<String, Object> input, String type, String agentId) {
        return switch (type) {
            case "withdrawal" -> transformWithdrawalRequest(input, agentId);
            case "deposit" -> transformDepositRequest(input, agentId);
            case "topup" -> transformTopupRequest(input, agentId);
            case "ewallet" -> transformEWalletRequest(input, agentId);
            case "jompay" -> transformJomPayRequest(input, agentId);
            case "onboarding" -> transformOnboardingRequest(input, agentId);
            case "retail-sale", "retail-pin-purchase", "retail-cashback" -> transformRetailRequest(input, agentId);
            case "bill-pay" -> transformBillPayRequest(input, agentId);
            case "essp" -> transformEsspRequest(input, agentId);
            case "duitnow" -> transformDuitnowRequest(input, agentId);
            case "balance-inquiry" -> transformBalanceInquiryRequest(input, agentId);
            case "kyc-verify" -> transformKycVerifyRequest(input, agentId);
            case "kyc-biometric" -> transformKycBiometricRequest(input, agentId);
            case "rules-fees" -> transformRulesFeesRequest(input, agentId);
            default -> input;
        };
    }

    /**
     * Transform internal API response to external format
     */
    private Map<String, Object> transformResponse(Map<String, Object> input, String type, String agentId) {
        return switch (type) {
            case "withdrawal" -> transformWithdrawalResponse(input);
            case "deposit" -> transformDepositResponse(input);
            case "topup" -> transformTopupResponse(input);
            case "ewallet" -> transformEWalletResponse(input);
            case "jompay" -> transformJomPayResponse(input);
            case "onboarding" -> transformOnboardingResponse(input);
            case "retail-sale", "retail-pin-purchase", "retail-cashback" -> transformRetailResponse(input);
            case "bill-pay" -> transformBillPayResponse(input);
            case "essp" -> transformEsspResponse(input);
            case "duitnow" -> transformDuitnowResponse(input);
            case "balance-inquiry" -> transformBalanceInquiryResponse(input);
            case "kyc-verify", "kyc-biometric" -> transformKycResponse(input);
            case "rules-fees" -> transformRulesFeesResponse(input);
            default -> input;
        };
    }

    // ==================== REQUEST TRANSFORMERS ====================

    private Map<String, Object> transformWithdrawalRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("agentId", agentId);
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("customerFee", null);
        output.put("agentCommission", null);
        output.put("bankShare", null);
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));
        
        Object customerCard = input.get("customerCard");
        if (customerCard != null) {
            output.put("customerCardMasked", Transformers.maskPan(customerCard.toString()));
        }
        
        Object location = input.get("location");
        if (location instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> loc = (Map<String, Object>) location;
            output.put("geofenceLat", Transformers.toDouble(loc.get("latitude")));
            output.put("geofenceLng", Transformers.toDouble(loc.get("longitude")));
        }
        return output;
    }

    private Map<String, Object> transformDepositRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("agentId", agentId);
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("customerFee", null);
        output.put("agentCommission", null);
        output.put("bankShare", null);
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));
        output.put("destinationAccount", Transformers.toString(input.get("customerAccount")));
        return output;
    }

    private Map<String, Object> transformTopupRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("telco", Transformers.toString(input.get("telco")));
        output.put("phoneNumber", Transformers.toString(input.get("phoneNumber")));
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        String idempotencyKey = Transformers.toString(input.get("idempotencyKey"));
        output.put("idempotencyKey", Transformers.toUUID(idempotencyKey).toString());
        return output;
    }

    private Map<String, Object> transformEWalletRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("walletProvider", Transformers.toString(input.get("walletProvider")));
        Object walletAccountId = input.get("walletAccountId");
        if (walletAccountId != null) {
            output.put("walletId", walletAccountId.toString());
        }
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        String idempotencyKey = Transformers.toString(input.get("idempotencyKey"));
        output.put("internalTransactionId", Transformers.toUUID(idempotencyKey).toString());
        output.put("isWithdrawal", true);
        return output;
    }

    private Map<String, Object> transformJomPayRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("billerCode", Transformers.toString(input.get("billerCode")));
        output.put("ref1", Transformers.toString(input.get("ref1")));
        output.put("ref2", Transformers.toString(input.get("ref2")));
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("currency", "MYR");
        String idempotencyKey = Transformers.toString(input.get("idempotencyKey"));
        output.put("idempotencyKey", Transformers.toUUID(idempotencyKey).toString());
        return output;
    }

    private Map<String, Object> transformOnboardingRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("mykadNumber", Transformers.toString(input.get("mykadNumber")));
        output.put("extractedName", null);
        output.put("ssmBusinessName", Transformers.toString(input.get("businessName")));
        output.put("ssmOwnerName", null);
        
        String tier = Transformers.toString(input.get("tier"));
        String agentTier = tier == null ? "STANDARD" : switch (tier.toUpperCase()) {
            case "MICRO" -> "MICRO";
            case "PREMIUM" -> "PREMIUM";
            default -> "STANDARD";
        };
        output.put("agentTier", agentTier);
        
        output.put("merchantGpsLat", Transformers.toDouble(input.get("merchantGpsLat")));
        output.put("merchantGpsLng", Transformers.toDouble(input.get("merchantGpsLng")));
        output.put("phoneNumber", Transformers.toString(input.get("phoneNumber")));
        return output;
    }

    private Map<String, Object> transformRetailSaleRequest(Map<String, Object> input, String agentId) {
        return transformRetailRequest(input, agentId);
    }

    private Map<String, Object> transformRetailPinPurchaseRequest(Map<String, Object> input, String agentId) {
        return transformRetailRequest(input, agentId);
    }

    private Map<String, Object> transformRetailCashbackRequest(Map<String, Object> input, String agentId) {
        return transformRetailRequest(input, agentId);
    }

    private Map<String, Object> transformRetailRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("agentId", agentId);
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("merchantId", Transformers.toString(input.get("merchantId")));
        output.put("terminalId", Transformers.toString(input.get("terminalId")));
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));
        return output;
    }

    private Map<String, Object> transformDuitnowRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("agentId", agentId);
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("destinationBank", Transformers.toString(input.get("destinationBank")));
        output.put("destinationAccount", Transformers.toString(input.get("destinationAccount")));
        output.put("recipientName", Transformers.toString(input.get("recipientName")));
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));
        return output;
    }

    private Map<String, Object> transformBalanceInquiryRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("agentId", agentId);
        return output;
    }

    private Map<String, Object> transformKycVerifyRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("mykadNumber", Transformers.toString(input.get("mykadNumber")));
        output.put("fullName", Transformers.toString(input.get("fullName")));
        return output;
    }

    private Map<String, Object> transformKycBiometricRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("mykadNumber", Transformers.toString(input.get("mykadNumber")));
        output.put("biometricData", input.get("biometricData"));
        output.put("verificationType", Transformers.toString(input.get("verificationType")));
        return output;
    }

    private Map<String, Object> transformRulesFeesRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("agentId", agentId);
        output.put("transactionType", Transformers.toString(input.get("transactionType")));
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("agentTier", Transformers.toString(input.get("agentTier")));
        return output;
    }

    private Map<String, Object> transformBillPayRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("billerCode", Transformers.toString(input.get("billerCode")));
        output.put("ref1", Transformers.toString(input.get("ref1")));
        output.put("ref2", Transformers.toString(input.get("ref2")));
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));
        return output;
    }

    private Map<String, Object> transformEsspRequest(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new HashMap<>();
        output.put("agentId", agentId);
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("productId", Transformers.toString(input.get("productId")));
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));
        return output;
    }

    // ==================== RESPONSE TRANSFORMERS ====================

    private Map<String, Object> transformWithdrawalResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("transactionId", input.get("transactionId"));
        output.put("referenceId", input.get("referenceId"));
        output.put("amount", input.get("amount"));
        output.put("balance", input.get("balance"));
        output.put("currency", "MYR");
        output.put("timestamp", input.get("createdAt") != null ? input.get("createdAt") : java.time.Instant.now().toString());
        
        // Preserve error if present
        if (input.containsKey("error")) {
            output.put("error", input.get("error"));
        }
        return output;
    }

    private Map<String, Object> transformDepositResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("transactionId", input.get("transactionId"));
        output.put("referenceId", input.get("referenceId"));
        output.put("amount", input.get("amount"));
        output.put("balance", input.get("balance"));
        output.put("currency", "MYR");
        output.put("timestamp", input.get("createdAt") != null ? input.get("createdAt") : java.time.Instant.now().toString());
        
        if (input.containsKey("error")) {
            output.put("error", input.get("error"));
        }
        return output;
    }

    private Map<String, Object> transformTopupResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "COMPLETED"));
        output.put("transactionId", input.get("transactionId"));
        output.put("telco", input.get("telco"));
        output.put("phoneNumber", input.get("phoneNumber"));
        output.put("amount", input.get("amount"));
        return output;
    }

    private Map<String, Object> transformEWalletResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("transactionId", input.get("transactionId"));
        output.put("walletProvider", input.get("walletProvider"));
        output.put("amount", input.get("amount"));
        return output;
    }

    private Map<String, Object> transformJomPayResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("transactionId", input.get("transactionId"));
        output.put("billerCode", input.get("billerCode"));
        output.put("ref1", input.get("ref1"));
        output.put("amount", input.get("amount"));
        return output;
    }

    private Map<String, Object> transformOnboardingResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUBMITTED"));
        output.put("applicationId", input.get("applicationId"));
        output.put("message", input.get("message"));
        return output;
    }

    private Map<String, Object> transformRetailSaleResponse(Map<String, Object> input) {
        return transformRetailResponse(input);
    }

    private Map<String, Object> transformRetailPinPurchaseResponse(Map<String, Object> input) {
        return transformRetailResponse(input);
    }

    private Map<String, Object> transformRetailCashbackResponse(Map<String, Object> input) {
        return transformRetailResponse(input);
    }

    private Map<String, Object> transformRetailResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("transactionId", input.get("transactionId"));
        output.put("amount", input.get("amount"));
        output.put("currency", "MYR");
        return output;
    }

    private Map<String, Object> transformDuitnowResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("transactionId", input.get("transactionId"));
        output.put("amount", input.get("amount"));
        output.put("destinationBank", input.get("destinationBank"));
        output.put("destinationAccount", input.get("destinationAccount"));
        return output;
    }

    private Map<String, Object> transformBalanceInquiryResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("agentId", input.get("agentId"));
        output.put("balance", input.get("balance"));
        output.put("availableBalance", input.get("availableBalance"));
        output.put("currency", "MYR");
        return output;
    }

    private Map<String, Object> transformKycResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("verified", input.get("verified"));
        output.put("verificationId", input.get("verificationId"));
        output.put("message", input.get("message"));
        return output;
    }

    private Map<String, Object> transformRulesFeesResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("customerFee", input.get("customerFee"));
        output.put("agentCommission", input.get("agentCommission"));
        output.put("bankShare", input.get("bankShare"));
        output.put("feeConfig", input.get("feeConfig"));
        return output;
    }

    private Map<String, Object> transformBillPayResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("transactionId", input.get("transactionId"));
        output.put("billerCode", input.get("billerCode"));
        output.put("amount", input.get("amount"));
        return output;
    }

    private Map<String, Object> transformEsspResponse(Map<String, Object> input) {
        Map<String, Object> output = new HashMap<>();
        output.put("status", input.getOrDefault("status", "SUCCESS"));
        output.put("transactionId", input.get("transactionId"));
        output.put("amount", input.get("amount"));
        output.put("currency", "MYR");
        return output;
    }

    // ==================== ERROR HANDLING ====================

    private Mono<Void> errorResponse(ServerWebExchange exchange, String message, String errorCode) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        
        String errorBody = String.format(
            "{\"status\":\"FAILED\",\"error\":{\"code\":\"%s\",\"message\":\"%s\",\"action_code\":\"DECLINE\",\"trace_id\":\"%s\",\"timestamp\":\"%s\"}}",
            errorCode, message, UUID.randomUUID(), java.time.Instant.now()
        );
        
        DataBuffer buffer = exchange.getResponse().bufferFactory()
            .wrap(errorBody.getBytes(StandardCharsets.UTF_8));
        
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    // ==================== CONFIG ====================

    /**
     * Defines the order of shortcut arguments for the YAML compact notation.
     * Example: RequestTransform=withdrawal → type="withdrawal"
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("type");
    }

    public static class Config {
        private String type;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
