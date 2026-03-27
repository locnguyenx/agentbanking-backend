package com.agentbanking.onboarding.infrastructure.web;

import com.agentbanking.onboarding.domain.model.AgentRecord;
import com.agentbanking.onboarding.domain.model.AgentTier;
import com.agentbanking.onboarding.domain.port.in.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/backoffice/agents")
public class AgentController {

    private final CreateAgentUseCase createAgentUseCase;
    private final UpdateAgentUseCase updateAgentUseCase;
    private final DeactivateAgentUseCase deactivateAgentUseCase;
    private final ListAgentsUseCase listAgentsUseCase;

    public AgentController(CreateAgentUseCase createAgentUseCase,
                          UpdateAgentUseCase updateAgentUseCase,
                          DeactivateAgentUseCase deactivateAgentUseCase,
                          ListAgentsUseCase listAgentsUseCase) {
        this.createAgentUseCase = createAgentUseCase;
        this.updateAgentUseCase = updateAgentUseCase;
        this.deactivateAgentUseCase = deactivateAgentUseCase;
        this.listAgentsUseCase = listAgentsUseCase;
    }

    @PostMapping
    public ResponseEntity<AgentResponse> createAgent(@Valid @RequestBody CreateAgentRequest request) {
        CreateAgentUseCase.CreateAgentCommand command = new CreateAgentUseCase.CreateAgentCommand(
            request.agentCode(),
            request.businessName(),
            request.tier(),
            request.merchantGpsLat(),
            request.merchantGpsLng(),
            request.mykadNumber(),
            request.phoneNumber()
        );
        AgentRecord result = createAgentUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(AgentResponse.from(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentResponse> updateAgent(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdateAgentRequest request) {
        UpdateAgentUseCase.UpdateAgentCommand command = new UpdateAgentUseCase.UpdateAgentCommand(
            request.businessName(),
            request.tier(),
            request.merchantGpsLat(),
            request.merchantGpsLng(),
            request.phoneNumber()
        );
        AgentRecord result = updateAgentUseCase.update(id, command);
        return ResponseEntity.ok(AgentResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateAgent(@PathVariable UUID id) {
        deactivateAgentUseCase.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AgentResponse>> listAgents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AgentRecord> agents = listAgentsUseCase.list(page, size);
        return ResponseEntity.ok(agents.stream().map(AgentResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentResponse> getAgent(@PathVariable UUID id) {
        return listAgentsUseCase.findById(id)
            .map(agent -> ResponseEntity.ok(AgentResponse.from(agent)))
            .orElse(ResponseEntity.notFound().build());
    }

    public record CreateAgentRequest(
        @NotBlank String agentCode,
        @NotBlank String businessName,
        @NotNull AgentTier tier,
        @NotNull @Positive BigDecimal merchantGpsLat,
        @NotNull @Positive BigDecimal merchantGpsLng,
        @NotBlank String mykadNumber,
        @NotBlank String phoneNumber
    ) {}

    public record UpdateAgentRequest(
        @NotBlank String businessName,
        @NotNull AgentTier tier,
        @NotNull @Positive BigDecimal merchantGpsLat,
        @NotNull @Positive BigDecimal merchantGpsLng,
        @NotBlank String phoneNumber
    ) {}

    public record AgentResponse(
        UUID agentId,
        String agentCode,
        String businessName,
        AgentTier tier,
        String status,
        BigDecimal merchantGpsLat,
        BigDecimal merchantGpsLng,
        String phoneNumber,
        String createdAt,
        String updatedAt
    ) {
        public static AgentResponse from(AgentRecord record) {
            return new AgentResponse(
                record.agentId(),
                record.agentCode(),
                record.businessName(),
                record.tier(),
                record.status().name(),
                record.merchantGpsLat(),
                record.merchantGpsLng(),
                record.phoneNumber(),
                record.createdAt().toString(),
                record.updatedAt().toString()
            );
        }
    }
}
