package com.agentbanking.gateway.filter;

import com.agentbanking.gateway.transform.Transformers;
import com.agentbanking.gateway.transform.WithdrawalTransformer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Component
public class RequestTransformFilterFactory 
        extends AbstractGatewayFilterFactory<RequestTransformFilterFactory.Config> {

    private static final Logger log = LoggerFactory.getLogger(RequestTransformGatewayFilterFactory.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public RequestTransformFilterFactory() {
        super(Config.class);
        System.out.println("=== REQUEST TRANSFORM FILTER FACTORY CREATED ===");
        System.out.println("=== REQUEST TRANSFORM FILTER FACTORY CLASS LOADED FROM: " + getClass().getProtectionDomain().getCodeSource().getLocation() + " ===");
    }

    @Override
    public GatewayFilter apply(Config config) {
        String type = config.getType();
        System.out.println("=== REQUEST TRANSFORM FILTER APPLY CALLED WITH TYPE: " + type + " ===");
        
        org.springframework.cloud.gateway.filter.GatewayFilter delegate = new org.springframework.cloud.gateway.filter.GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
                ServerHttpRequest request = exchange.getRequest();
                String agentId = request.getHeaders().getFirst("X-Agent-Id");
                
                if (agentId == null || agentId.isBlank()) {
                    return errorResponse(exchange, "Missing X-Agent-Id header", "ERR_AUTH_MISSING_AGENT");
                }
                
                System.out.println("=== REQUEST TRANSFORM FILTER PROCESSING REQUEST ===");
                System.out.println("=== REQUEST TRANSFORM FILTER: agentId from header: " + agentId + " ===");
                
                return DataBufferUtils.join(request.getBody())
                    .flatMap(dataBuffer -> {
                        try {
                            ByteBuffer buffer = dataBuffer.toByteBuffer();
                            byte[] bytes = new byte[buffer.remaining()];
                            buffer.get(bytes);
                            DataBufferUtils.release(dataBuffer);
                            
                            String body = new String(bytes, StandardCharsets.UTF_8);
                            Map<String, Object> input = mapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                            
                            System.out.println("=== REQUEST TRANSFORM FILTER: input body: " + body + " ===");
                            
                            Map<String, Object> transformed = transform(input, type, agentId);
                            String transformedBody = mapper.writeValueAsString(transformed);
                            
                            System.out.println("=== REQUEST TRANSFORM FILTER: transformed body: " + transformedBody + " ===");
                            
                            byte[] outputBytes = transformedBody.getBytes(StandardCharsets.UTF_8);
                            DataBuffer outputBuffer = exchange.getResponse().bufferFactory().wrap(outputBytes);
                            
                            ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {
                                @Override
                                public Flux<DataBuffer> getBody() {
                                    return Flux.just(outputBuffer);
                                }
                            };
                            
                            return chain.filter(exchange.mutate().request(decorator).build());
                        } catch (Exception e) {
                            log.error("Transformation failed: {}", e.getMessage());
                            DataBufferUtils.release(dataBuffer);
                            return errorResponse(exchange, "Invalid JSON: " + e.getMessage(), "ERR_VAL_INVALID_JSON");
                        }
                    });
            }
        };
        
        return new OrderedGatewayFilter(delegate, Ordered.HIGHEST_PRECEDENCE + 1);
    }

     private Map<String, Object> transform(Map<String, Object> input, String type, String agentId) {
         log.info("TRANSFORM DEBUG: input={}, type={}, agentId={}", input, type, agentId);
         Map<String, Object> result = switch (type) {
             case "withdrawal" -> transformWithdrawal(input, agentId);
             case "deposit" -> transformDeposit(input, agentId);
             case "topup" -> transformTopup(input, agentId);
             case "ewallet" -> transformEWallet(input, agentId);
             case "jompay" -> transformJomPay(input, agentId);
             case "onboarding" -> transformOnboarding(input, agentId);
             default -> input;
         };
         log.info("TRANSFORM DEBUG: result={}", result);
         return result;
     }

    private Map<String, Object> transformWithdrawal(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new java.util.HashMap<>();
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

    private Map<String, Object> transformDeposit(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new java.util.HashMap<>();
        output.put("agentId", agentId);
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("customerFee", null);
        output.put("agentCommission", null);
        output.put("bankShare", null);
        output.put("idempotencyKey", Transformers.toString(input.get("idempotencyKey")));
        output.put("destinationAccount", Transformers.toString(input.get("customerAccount")));
        return output;
    }

    private Map<String, Object> transformTopup(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new java.util.HashMap<>();
        output.put("telco", Transformers.toString(input.get("telco")));
        output.put("phoneNumber", Transformers.toString(input.get("phoneNumber")));
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        String idempotencyKey = Transformers.toString(input.get("idempotencyKey"));
        output.put("idempotencyKey", Transformers.toUUID(idempotencyKey).toString());
        return output;
    }

    private Map<String, Object> transformEWallet(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new java.util.HashMap<>();
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

    private Map<String, Object> transformJomPay(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new java.util.HashMap<>();
        output.put("billerCode", Transformers.toString(input.get("billerCode")));
        output.put("ref1", Transformers.toString(input.get("ref1")));
        output.put("ref2", Transformers.toString(input.get("ref2")));
        output.put("amount", Transformers.toBigDecimal(input.get("amount")));
        output.put("currency", "MYR");
        String idempotencyKey = Transformers.toString(input.get("idempotencyKey"));
        output.put("idempotencyKey", Transformers.toUUID(idempotencyKey).toString());
        return output;
    }

    private Map<String, Object> transformOnboarding(Map<String, Object> input, String agentId) {
        Map<String, Object> output = new java.util.HashMap<>();
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

    public static class Config {
        private String type;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
