# Domain Model Template

## DO: Use Records for Value Objects

```java
package com.agentbanking.<service>.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record YourEntityNameRecord(
    UUID id,
    String name,
    BigDecimal amount,
    LocalDateTime createdAt
) {}
```

## DO: Use Plain Java Classes for Entities with JPA

Place in **infrastructure** layer, NOT domain:

```java
package com.agentbanking.<service>.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "your_table_name")
public class YourEntityNameEntity {
    @Id
    private UUID id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "amount")
    private BigDecimal amount;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // getters and setters
}
```

## DON'T: Do This

```java
// ❌ WRONG - JPA in domain layer
package com.agentbanking.<service>.domain.model;

@Entity
@Table(name = "your_table")
public class YourEntity {  // <-- DON'T put JPA here
    @Id
    private UUID id;
}
```

## DO: Use Repository Pattern

```java
// Domain port (interface)
package com.agentbanking.<service>.domain.port.out;

import com.agentbanking.<service>.domain.model.YourEntityNameRecord;
import java.util.Optional;
import java.util.UUID;

public interface YourEntityRepository {
    Optional<YourEntityNameRecord> findById(UUID id);
    YourEntityNameRecord save(YourEntityNameRecord record);
}

// Infrastructure adapter (implementation)
package com.agentbanking.<service>.infrastructure.persistence.repository;

import com.agentbanking.<service>.domain.model.YourEntityNameRecord;
import com.agentbanking.<service>.domain.port.out.YourEntityRepository;
import org.springframework.stereotype.Repository;

@Repository
public class YourEntityRepositoryImpl implements YourEntityRepository {
    // Use JPA repository here, map to/from records
}
```