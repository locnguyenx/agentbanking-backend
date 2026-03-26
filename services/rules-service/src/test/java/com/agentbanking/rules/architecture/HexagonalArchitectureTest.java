package com.agentbanking.rules.architecture;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

public class HexagonalArchitectureTest {

    @Test
    void domainLayerMustNotContainJpaAnnotations() throws Exception {
        Path domainPath = Path.of("src/main/java/com/agentbanking/rules/domain");
        
        try (Stream<Path> paths = Files.walk(domainPath)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                .forEach(javaFile -> {
                    try {
                        String content = Files.readString(javaFile);
                        boolean hasEntity = content.contains("@Entity");
                        boolean hasTable = content.contains("@Table");
                        boolean hasColumn = content.contains("@Column");
                        boolean hasId = content.contains("@Id");
                        
                        // Check if it's in domain/model or domain/service
                        boolean isInDomain = javaFile.toString().contains("/domain/model/") || 
                                            javaFile.toString().contains("/domain/service/") ||
                                            javaFile.toString().contains("/domain/port/");
                        
                        if (isInDomain && (hasEntity || hasTable || hasColumn || hasId)) {
                            fail("JPA annotation found in domain layer: " + javaFile);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }

    @Test
    void domainLayerMustNotContainSpringServiceAnnotations() throws Exception {
        Path domainPath = Path.of("src/main/java/com/agentbanking/rules/domain");
        
        try (Stream<Path> paths = Files.walk(domainPath)) {
            paths.filter(p -> p.toString().endsWith(".java"))
                .forEach(javaFile -> {
                    try {
                        String content = Files.readString(javaFile);
                        boolean hasService = content.contains("@Service");
                        boolean hasComponent = content.contains("@Component");
                        boolean hasRepository = content.contains("@Repository");
                        boolean hasTransactional = content.contains("@Transactional");
                        
                        // Check if it's in domain layer (not infrastructure)
                        boolean isInDomain = javaFile.toString().contains("/domain/model/") || 
                                            javaFile.toString().contains("/domain/service/") ||
                                            javaFile.toString().contains("/domain/port/");
                        
                        if (isInDomain && (hasService || hasComponent || hasRepository || hasTransactional)) {
                            fail("Spring annotation found in domain layer: " + javaFile);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
        }
    }
}