package com.org.healthscore.api;

import com.org.healthscore.adapter.SignalAdapterService;
import com.org.healthscore.core.debt.DebtService;
import com.org.healthscore.core.scoring.HealthScoreEngine;
import com.org.healthscore.domain.DebtContribution;
import com.org.healthscore.domain.HealthScore;
import com.org.healthscore.domain.Signal;
import com.org.healthscore.parser.SonarApiClient;
import com.org.healthscore.parser.SonarQubeParser;
import com.org.healthscore.repository.mongo.ScoreDocument;
import com.org.healthscore.repository.mongo.ScoreRepository;
import com.org.healthscore.repository.mongo.SignalDocument;
import com.org.healthscore.repository.mongo.SignalRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API for the Health Score platform.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HealthScoreController {
    
    private final SignalAdapterService adapterService;
    private final HealthScoreEngine healthScoreEngine;
    private final DebtService debtService;
    private final SignalRepository signalRepository;
    private final ScoreRepository scoreRepository;
    
    // Tool integrations
    private final SonarApiClient sonarApiClient;
    private final SonarQubeParser sonarQubeParser;
    
    /**
     * Ingest signal data from an external tool.
     */
    @PostMapping("/signals")
    public ResponseEntity<Map<String, Object>> ingestSignals(@Valid @RequestBody SignalIngestionRequest request) {
        log.info("Ingesting signals from {} for {}/{}", 
                request.getSourceType(), request.getEntityType(), request.getEntityId());
        
        List<Signal> signals = adapterService.adaptToSignals(
                request.getSourceType(),
                request.getSourceId(),
                request.getEntityType(),
                request.getEntityId(),
                request.getData()
        );
        
        // Persist signals
        for (Signal signal : signals) {
            SignalDocument doc = toSignalDocument(signal, request.getEntityType(), request.getEntityId());
            signalRepository.save(doc);
        }
        
        log.info("Ingested {} signals from {}", signals.size(), request.getSourceType());
        
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "signalsIngested", signals.size(),
                "signals", signals.stream().map(Signal::getMetricKey).collect(Collectors.toList())
        ));
    }
    
    /**
     * Compute health score for an entity.
     */
    @PostMapping("/scores/{entityType}/{entityId}/compute")
    public ResponseEntity<HealthScoreResponse> computeHealthScore(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        
        log.info("Computing health score for {}/{}", entityType, entityId);
        
        // Get all signals for the entity
        List<SignalDocument> signalDocs = signalRepository.findByEntityTypeAndEntityId(entityType, entityId);
        List<Signal> signals = signalDocs.stream()
                .map(this::toSignal)
                .collect(Collectors.toList());
        
        if (signals.isEmpty()) {
            log.warn("No signals found for {}/{}", entityType, entityId);
            return ResponseEntity.notFound().build();
        }
        
        // Compute health score
        HealthScore healthScore = healthScoreEngine.computeHealthScore(entityType, entityId, signals);
        
        // Compute debt contributions
        List<DebtContribution> debtContributions = debtService.computeDebtContributions(signals);
        
        // Build response
        HealthScoreResponse response = toResponse(healthScore, debtContributions);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get the latest health score for an entity.
     */
    @GetMapping("/scores/{entityType}/{entityId}")
    public ResponseEntity<HealthScoreResponse> getHealthScore(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        
        return scoreRepository.findTopByEntityTypeAndEntityIdOrderByComputedAtDesc(entityType, entityId)
                .map(doc -> ResponseEntity.ok(toResponse(doc)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get all signals for an entity.
     */
    @GetMapping("/signals/{entityType}/{entityId}")
    public ResponseEntity<List<SignalDocument>> getSignals(
            @PathVariable String entityType,
            @PathVariable String entityId) {
        
        List<SignalDocument> signals = signalRepository.findByEntityTypeAndEntityId(entityType, entityId);
        return ResponseEntity.ok(signals);
    }
    
    /**
     * Integrate external tools to fetch signals and optionally compute health score.
     * 
     * Flow: Tool API → Parser → Adapter → Signal persistence
     * 
     * The controller orchestrates only - no parsing, normalization, or scoring logic here.
     */
    @PostMapping("/tools/integrate")
    public ResponseEntity<Map<String, Object>> integrateTools(@Valid @RequestBody ToolIntegrationRequest request) {
        log.info("Integrating tools {} for {}/{}", 
                request.getTools(), request.getEntityType(), request.getEntityId());
        
        List<Signal> allSignals = new ArrayList<>();
        Map<String, Object> toolResults = new java.util.LinkedHashMap<>();
        
        for (String tool : request.getTools()) {
            try {
                List<Signal> toolSignals = integrateToolSignals(
                        tool,
                        request.getEntityType(),
                        request.getEntityId(),
                        request.getToolConfig() != null ? request.getToolConfig().get(tool) : null
                );
                
                // Persist signals
                for (Signal signal : toolSignals) {
                    SignalDocument doc = toSignalDocument(signal, request.getEntityType(), request.getEntityId());
                    signalRepository.save(doc);
                }
                
                allSignals.addAll(toolSignals);
                toolResults.put(tool, Map.of(
                        "status", "success",
                        "signalsCount", toolSignals.size(),
                        "signals", toolSignals.stream().map(Signal::getMetricKey).collect(Collectors.toList())
                ));
                
                log.info("Tool {} produced {} signals", tool, toolSignals.size());
            } catch (Exception e) {
                log.error("Error integrating tool {}: {}", tool, e.getMessage(), e);
                toolResults.put(tool, Map.of(
                        "status", "error",
                        "message", e.getMessage()
                ));
            }
        }
        
        return ResponseEntity.ok(Map.of(
                "entityType", request.getEntityType(),
                "entityId", request.getEntityId(),
                "totalSignals", allSignals.size(),
                "toolResults", toolResults
        ));
    }
    
    /**
     * Orchestrates: Tool API call → Parser → Adapter
     * 
     * Controller responsibility: Orchestration only.
     * No parsing, normalization, or business logic here.
     */
    private List<Signal> integrateToolSignals(String tool, String entityType, String entityId, 
                                               Map<String, String> config) {
        return switch (tool.toLowerCase()) {
            case "sonarqube" -> integrateSonarQube(entityType, entityId, config);
            default -> {
                log.warn("Unknown tool: {}", tool);
                yield List.of();
            }
        };
    }
    
    /**
     * SonarQube integration: API → Parser → Adapter
     * 
     * This is pure orchestration - the controller invokes components in order
     * but does NOT implement any parsing, normalization, or scoring.
     */
    private List<Signal> integrateSonarQube(String entityType, String entityId, Map<String, String> config) {
        // Get component key from config or use entityId as fallback
        String componentKey = (config != null && config.containsKey("componentKey")) 
                ? config.get("componentKey") 
                : entityId;
        
        // Step 1: Fetch raw data from SonarQube API (SonarApiClient responsibility)
        Map<String, Object> rawResponse = sonarApiClient.fetchIssues(componentKey);
        if (rawResponse == null || rawResponse.isEmpty()) {
            log.warn("Empty response from SonarQube for component: {}", componentKey);
            return List.of();
        }
        
        // Step 2: Parse raw response into normalized structure (Parser responsibility)
        Map<String, Object> parsedData = sonarQubeParser.parse(rawResponse);
        
        // Step 3: Adapt parsed data into canonical signals (Adapter responsibility)
        List<Signal> signals = adapterService.adaptToSignals(
                "sonarqube",
                componentKey,
                entityType,
                entityId,
                parsedData
        );
        
        return signals;
    }
    
    private SignalDocument toSignalDocument(Signal signal, String entityType, String entityId) {
        SignalDocument doc = new SignalDocument();
        doc.setId(signal.getId());
        doc.setSourceType(signal.getSourceType());
        doc.setSourceId(signal.getSourceId());
        doc.setMetricKey(signal.getMetricKey());
        doc.setCanonicalForm(signal.getCanonicalForm().name());
        doc.setTimestamp(signal.getTimestamp());
        doc.setEntityType(entityType);
        doc.setEntityId(entityId);
        
        // Store value based on canonical form
        doc.setValue(switch (signal.getCanonicalForm()) {
            case COUNTABLE_CATEGORY -> Map.of("categories", signal.getCountableValue());
            case SCALAR -> Map.of("value", signal.getScalarValue());
            case BOOLEAN -> Map.of("value", signal.getBooleanValue());
            case ENUM -> Map.of("value", signal.getEnumValue());
        });
        
        return doc;
    }
    
    private Signal toSignal(SignalDocument doc) {
        var form = com.org.healthscore.domain.CanonicalForm.valueOf(doc.getCanonicalForm());
        
        Signal.SignalBuilder builder = Signal.builder()
                .id(doc.getId())
                .sourceType(doc.getSourceType())
                .sourceId(doc.getSourceId())
                .metricKey(doc.getMetricKey())
                .canonicalForm(form)
                .timestamp(doc.getTimestamp())
                .metadata(doc.getMetadata());
        
        Map<String, Object> value = doc.getValue();
        if (value != null) {
            switch (form) {
                case COUNTABLE_CATEGORY -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> categories = (Map<String, Integer>) value.get("categories");
                    builder.countableValue(categories);
                }
                case SCALAR -> builder.scalarValue(
                        new java.math.BigDecimal(value.get("value").toString()));
                case BOOLEAN -> builder.booleanValue((Boolean) value.get("value"));
                case ENUM -> builder.enumValue((String) value.get("value"));
            }
        }
        
        return builder.build();
    }
    
    private HealthScoreResponse toResponse(HealthScore healthScore, List<DebtContribution> debtContributions) {
        HealthScoreResponse response = new HealthScoreResponse();
        response.setEntityType(healthScore.getEntityType());
        response.setEntityId(healthScore.getEntityId());
        response.setOverallScore(healthScore.getOverallScore());
        response.setDimensionScores(healthScore.getDimensionScores());
        response.setComputedAt(healthScore.getComputedAt());
        
        response.setDebtContributions(debtContributions.stream()
                .map(dc -> {
                    HealthScoreResponse.DebtContributionDto dto = new HealthScoreResponse.DebtContributionDto();
                    dto.setMetricKey(dc.getMetricKey());
                    dto.setDimension(dc.getDimension());
                    dto.setContribution(dc.getContribution());
                    dto.setSeverity(dc.getSeverity());
                    dto.setDescription(dc.getDescription());
                    return dto;
                })
                .collect(Collectors.toList()));
        
        return response;
    }
    
    private HealthScoreResponse toResponse(ScoreDocument doc) {
        HealthScoreResponse response = new HealthScoreResponse();
        response.setEntityType(doc.getEntityType());
        response.setEntityId(doc.getEntityId());
        response.setOverallScore(doc.getOverallScore());
        response.setDimensionScores(doc.getDimensionScores());
        response.setComputedAt(doc.getComputedAt());
        
        if (doc.getDebtContributions() != null) {
            response.setDebtContributions(doc.getDebtContributions().stream()
                    .map(dc -> {
                        HealthScoreResponse.DebtContributionDto dto = new HealthScoreResponse.DebtContributionDto();
                        dto.setMetricKey(dc.getMetricKey());
                        dto.setDimension(dc.getDimension());
                        dto.setContribution(dc.getContribution());
                        dto.setSeverity(dc.getSeverity());
                        dto.setDescription(dc.getDescription());
                        return dto;
                    })
                    .collect(Collectors.toList()));
        }
        
        return response;
    }
}
