import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class DebugPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "test123";
        String hashedFromDb = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        
        System.out.println("Raw password: " + rawPassword);
        System.out.println("Hash from DB: " + hashedFromDb);
        System.out.println("Matches: " + encoder.matches(rawPassword, hashedFromDb));
        
        // Also test encoding the raw password to see what it produces
        String encoded = encoder.encode(rawPassword);
        System.out.println("New hash: " + encoded);
        System.out.println("New hash matches raw: " + encoder.matches(rawPassword, encoded));
        System.out.println("New hash matches DB hash: " + encoder.matches(hashedFromDb, encoded));
    }
}