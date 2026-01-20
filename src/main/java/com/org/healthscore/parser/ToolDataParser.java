package com.org.healthscore.parser;

import java.util.Map;

/**
 * Contract for external tool data parsers.
 * 
 * Parsers handle tool API syntax only - they are tool-specific.
 * Each external tool (SonarQube, Jira, GitHub, etc.) has its own parser.
 */
public interface ToolDataParser {
    
    /**
     * The source type this parser handles.
     */
    String getSourceType();
    
    /**
     * Parse raw API response from the external tool.
     * 
     * @param rawResponse Raw response from the tool's API
     * @return Normalized map structure for adapter processing
     */
    Map<String, Object> parse(String rawResponse);
    
    /**
     * Parse raw API response from the external tool.
     * 
     * @param rawResponse Raw response as a map (already parsed JSON)
     * @return Normalized map structure for adapter processing
     */
    Map<String, Object> parse(Map<String, Object> rawResponse);
}
