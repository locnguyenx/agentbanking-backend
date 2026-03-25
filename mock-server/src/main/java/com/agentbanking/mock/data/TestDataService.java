package com.agentbanking.mock.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;

@Service
public class TestDataService {

    private List<Citizen> citizens = new ArrayList<>();
    private List<Agent> agents = new ArrayList<>();
    private List<Biller> billers = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    public void load() throws IOException {
        citizens = mapper.readValue(new ClassPathResource("mock-data/citizens.json").getInputStream(),
            new TypeReference<>() {});
        agents = mapper.readValue(new ClassPathResource("mock-data/agents.json").getInputStream(),
            new TypeReference<>() {});
        billers = mapper.readValue(new ClassPathResource("mock-data/billers.json").getInputStream(),
            new TypeReference<>() {});
    }

    public Citizen findCitizenByMykad(String mykad) {
        return citizens.stream().filter(c -> c.mykad().equals(mykad)).findFirst().orElse(null);
    }

    public Agent findAgentByCode(String code) {
        return agents.stream().filter(a -> a.agentCode().equals(code)).findFirst().orElse(null);
    }

    public boolean isValidBillerRef(String billerCode, String ref) {
        return billers.stream()
            .filter(b -> b.billerCode().equals(billerCode))
            .findFirst()
            .map(b -> b.validRefs().contains(ref))
            .orElse(false);
    }

    public record Citizen(String mykad, String fullName, String dateOfBirth, String amlStatus) {}
    public record Agent(String agentCode, String tier, String status, double gpsLat, double gpsLng) {}
    public record Biller(String billerCode, String name, List<String> validRefs) {}
}