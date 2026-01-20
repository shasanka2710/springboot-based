package com.org.healthscore.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Map;

/**
 * API request for ingesting signal data from external tools.
 */
@Data
public class SignalIngestionRequest {
    
    @NotBlank(message = "Source type is required")
    private String sourceType;
    
    @NotBlank(message = "Source ID is required")
    private String sourceId;
    
    @NotBlank(message = "Entity type is required")
    private String entityType;
    
    @NotBlank(message = "Entity ID is required")
    private String entityId;
    
    /**
     * Raw data from the external tool.
     * Structure depends on the source type.
     */
    private Map<String, Object> data;
}
