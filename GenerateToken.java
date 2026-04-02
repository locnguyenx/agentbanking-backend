import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class GenerateToken {
    private static final String SECRET = "your-super-secret-jwt-key-change-in-production-minimum-32-chars-long";
    private static final String AGENT_ID = "a0000000-0000-0000-0000-000000000001";
    private static final String SUBJECT = "be581cb8-62b0-414f-9826-b539ee40dc4e";

    public static void main(String[] args) {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

        String token = Jwts.builder()
                .subject(SUBJECT)
                .claim("agent_id", AGENT_ID)
                .claim("permissions", new String[]{})
                .claim("fullName", "Test Agent")
                .claim("email", "agent001@bank.com")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .id(UUID.randomUUID().toString())
                .signWith(key)
                .compact();

        System.out.println(token);
    }
}
