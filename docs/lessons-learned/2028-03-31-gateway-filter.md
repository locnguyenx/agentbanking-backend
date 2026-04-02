# Lessons Learned: Gateway Filter & Transform

## 1. Spring Cloud Gateway Naming Convention

### Issue
`AbstractGatewayFilterFactory` subclasses must follow a specific naming convention for Spring to auto-discover them.

### Root Cause
- Class named `RequestTransformFilterFactory` → YAML reference `RequestTransform=withdrawal` **FAILS**
- Spring strips `GatewayFilterFactory` suffix, not `FilterFactory`
- `RequestTransform` in YAML looks for bean `RequestTransformGatewayFilterFactory`

### Lesson
```
Class Name                              → YAML Reference
JwtAuthGatewayFilterFactory             → JwtAuth
RequestTransformGatewayFilterFactory    → RequestTransform
LoggingGatewayFilterFactory             → Logging
```

### Rule
**Always** end filter factory class names with `GatewayFilterFactory`.

---

## 2. Compact YAML Notation Requires `shortcutFieldOrder()`

### Issue
`RequestTransform=withdrawal` didn't pass the `type` argument to the Config class.

### Root Cause
- Compact notation `- FilterName=value1,value2` requires `shortcutFieldOrder()` override
- Without it, Spring doesn't know which Config field corresponds to the value

### Fix
```java
@Override
public List<String> shortcutFieldOrder() {
    return Arrays.asList("type");
}
```

### Lesson
When using compact YAML notation, always implement `shortcutFieldOrder()`.

---

## 3. JwtAuthFilter vs JwtAuthGatewayFilterFactory

### Issue
`JwtAuthFilter` (without `GatewayFilterFactory` suffix) worked in YAML as `- JwtAuthFilter` because:
- The class name matched exactly (case-insensitive)
- Spring found the `@Component` bean by name

### But
This breaks the convention and causes confusion. When we renamed to `RequestTransformGatewayFilterFactory`, we needed to update YAML to `RequestTransform` (without suffix).

### Lesson
Follow the convention consistently:
- Class: `XxxGatewayFilterFactory`
- YAML: `Xxx`

---

## 4. Request Decorator Doesn't Preserve Path for RewritePath

### Issue
`ServerHttpRequestDecorator` in the RequestTransform filter interfered with `RewritePath` filter.

### Root Cause
- RequestTransform creates a decorator that overrides `getBody()` and `getHeaders()`
- The decorator doesn't override `getURI()` or `getPath()`
- RewritePath filter may not work correctly with decorated requests

### Investigation Needed
The `RewritePath` filter may not be compatible with custom request decorators. Consider doing path rewriting inside the transform filter itself.

---

## 5. System.out.println vs Logger in Docker

### Issue
`System.out.println` debug statements didn't appear in Docker logs.

### Root Cause
- Docker buffers stdout/stderr
- Spring Boot's logging framework properly outputs to Docker logs
- `System.out.println` may be lost or buffered

### Lesson
Always use SLF4J Logger instead of `System.out.println`:
```java
private static final Logger log = LoggerFactory.getLogger(MyFilter.class);
log.info("message");
log.debug("details");
```

---

## 6. Filter Order Matters

### Issue
JwtAuth filter must run before RequestTransform filter.

### Solution
Use `OrderedGatewayFilter` with explicit order:
```java
return new OrderedGatewayFilter(delegate, Ordered.HIGHEST_PRECEDENCE);
```

### Lesson
- JwtAuth: `Ordered.HIGHEST_PRECEDENCE` (runs first)
- RequestTransform: `Ordered.HIGHEST_PRECEDENCE + 1` (runs second)
- RewritePath: Runs last (after transforms)

---

## 7. JWT Token Generation for Testing

### Issue
Auth service Kafka dependency blocked token generation for E2E tests.

### Solution
Created `generate-token.sh` that generates valid JWT tokens using the same secret:
```bash
SECRET="your-super-secret-jwt-key-change-in-production-minimum-32-chars-long"
# Use openssl to create HMAC-SHA512 signature
```

### Lesson
For E2E testing, have a fallback token generation method independent of other services.

---

## 8. Response Transformation Pattern

### Issue
Need to transform both requests AND responses.

### Solution
Use `ServerHttpResponseDecorator` with `writeWith()`:
```java
ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(response) {
    @Override
    public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        // Read response body, transform, write back
    }
};
```

### Lesson
Response transformation requires intercepting `writeWith()` to modify the response body before it's sent to the client.

---

## 9. Body Reading Consumes DataBuffer

### Issue
Once `DataBuffer` is read, it can't be read again.

### Solution
Always release the original buffer and create new ones:
```java
DataBufferUtils.release(dataBuffer);  // Release original
DataBuffer newBuffer = exchange.getResponse().bufferFactory().wrap(newBytes);
```

### Lesson
- `DataBufferUtils.join()` consumes the stream
- Always release buffers after reading
- Create new buffers for transformed content

---

## 10. Unit Testing Gateway Filters

### Approach
Use `MockServerWebExchange` and `MockServerHttpRequest`:
```java
MockServerHttpRequest request = MockServerHttpRequest.post("/api/test")
    .header("X-Agent-Id", TEST_AGENT_ID)
    .contentType(MediaType.APPLICATION_JSON)
    .body("{\"amount\": 100.00}");

MockServerWebExchange exchange = MockServerWebExchange.from(request);
```

### Lesson
Gateway filters can be unit tested without starting the full Spring context using mock classes from `spring-test`.

---

## Summary

| Lesson | Category |
|--------|----------|
| Naming convention | Discovery |
| shortcutFieldOrder() | Configuration |
| Consistent naming | Convention |
| Request decorator path | Compatibility |
| Logger vs println | Debugging |
| Filter ordering | Architecture |
| JWT token generation | Testing |
| Response transformation | Implementation |
| DataBuffer handling | Implementation |
| Unit testing approach | Testing |
