package com.org.healthscore.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;

/**
 * HTTP client for SonarQube REST API.
 * 
 * Responsibility: Invoke SonarQube API and return raw response.
 * Boundary: MUST NOT parse, aggregate, or interpret data.
 * 
 * This is part of the parser layer - handles tool API invocation only.
 */
@Slf4j
@Component
public class SonarApiClient {
    
    private final RestClient restClient;
    
    public SonarApiClient(
            @Value("${sonarqube.base-url:http://localhost:9000}") String baseUrl,
            @Value("${sonarqube.token:}") String token) {
        
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        
        // Add authentication if token is provided
        if (token != null && !token.isBlank()) {
            String auth = Base64.getEncoder().encodeToString((token + ":").getBytes());
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth);
        }
        
        this.restClient = builder.build();
    }
    
    /**
     * Fetch issues from SonarQube for a component.
     * 
     * Invokes: GET /api/issues/search?componentKeys={componentKey}
     * 
     * @param componentKey The SonarQube project/component key
     * @return Raw API response as a map (unparsed)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchIssues(String componentKey) {
        log.debug("Fetching issues from SonarQube for component: {}", componentKey);
        
        Map<String, Object> response = restClient.get()
                .uri("/api/issues/search?componentKeys={componentKey}&ps=500", componentKey)
                .retrieve()
                .body(Map.class);
        
        log.debug("Received response from SonarQube with {} issues", 
                response != null && response.containsKey("issues") 
                        ? ((java.util.List<?>) response.get("issues")).size() 
                        : 0);
        
        return response;
    }
}
