# Engineering Health Scoring Platform

A configuration-driven Engineering Health Scoring framework built with Spring Boot and MongoDB.

## Overview

This platform computes health scores for engineering entities (projects, teams, etc.) by:
1. Ingesting data from external tools (SonarQube, Jira, GitHub, etc.)
2. Normalizing data into canonical signal forms
3. Scoring signals using configuration-driven rules
4. Computing weighted health scores across dimensions

## Architecture

```
External Tools → Parser → Adapter → Canonical Signals → Scoring → Debt → HealthScore → API
```

### Key Principles

- **Code defines contracts**: Canonical forms, operators, and boundaries are fixed in code
- **MongoDB defines meaning**: Weights, thresholds, and business rules live in configuration
- **Configuration-driven**: New metrics can be added without code changes

### Canonical Forms

All signals must conform to one of these data shapes:

| Form | Type | Example |
|------|------|---------|
| COUNTABLE_CATEGORY | Map<String, Integer> | `{"CRITICAL": 5, "HIGH": 10}` |
| SCALAR | BigDecimal | `85.5` (coverage %) |
| BOOLEAN | Boolean | `true` (has CI/CD) |
| ENUM | String | `"HIGH"` (risk level) |

### Fixed Operators

| Operator | Canonical Form | Description |
|----------|---------------|-------------|
| THRESHOLD_SCORE | SCALAR | Score based on threshold ranges |
| WEIGHTED_CATEGORY_SUM | COUNTABLE_CATEGORY | Weighted sum of category counts |
| BOOLEAN_PENALTY | BOOLEAN | Score for true/false values |
| ENUM_MAPPING | ENUM | Map enum values to scores |

## Prerequisites

- Java 17+
- Maven 3.8+
- MongoDB 6.0+

## Getting Started

### 1. Start MongoDB

```bash
# Using Docker
docker run -d --name mongodb -p 27017:27017 mongo:6.0

# Or use local installation
mongod --dbpath /path/to/data
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The API will be available at `http://localhost:8080`

## API Endpoints

### Ingest Signals

```bash
POST /api/v1/signals
Content-Type: application/json

{
  "sourceType": "sonarqube",
  "sourceId": "project-123",
  "entityType": "project",
  "entityId": "my-project",
  "data": {
    "metrics": {
      "coverage": 75.5,
      "code_smells": 120
    },
    "issues": {
      "CRITICAL": 2,
      "HIGH": 5,
      "MEDIUM": 15
    }
  }
}
```

### Compute Health Score

```bash
POST /api/v1/scores/{entityType}/{entityId}/compute
```

### Get Health Score

```bash
GET /api/v1/scores/{entityType}/{entityId}
```

### Get Signals

```bash
GET /api/v1/signals/{entityType}/{entityId}
```

## MongoDB Collections

| Collection | Purpose |
|------------|---------|
| adapter_signal_definitions | Signal extraction and normalization rules |
| signals | Normalized signal storage |
| signal_scoring_rules | Scoring rules with operator parameters |
| debt_signal_contributions | Technical debt rules |
| debt_dimension_weights | Dimension weights for overall score |
| scores | Computed health scores |

## Configuration Examples

### Signal Definition (adapter_signal_definitions)

```json
{
  "sourceType": "sonarqube",
  "metricKey": "code_coverage",
  "canonicalForm": "SCALAR",
  "extractionPath": "metrics.coverage",
  "enabled": true
}
```

### Scoring Rule (signal_scoring_rules)

```json
{
  "metricKey": "code_coverage",
  "requiredCanonicalForm": "SCALAR",
  "operator": "THRESHOLD_SCORE",
  "parameters": {
    "thresholds": [
      {"min": 80, "max": 100, "score": 100},
      {"min": 60, "max": 79.99, "score": 75},
      {"min": 40, "max": 59.99, "score": 50},
      {"min": 0, "max": 39.99, "score": 25}
    ]
  },
  "weight": 0.3,
  "dimension": "code_quality",
  "enabled": true
}
```

## Project Structure

```
healthscore-platform/
└─ src/main/java/com/org/healthscore/
   ├─ api/                    # REST controllers
   ├─ parser/                 # Tool-specific parsers
   ├─ adapter/                # Signal normalization
   ├─ core/
   │  ├─ scoring/            # Health score engine
   │  ├─ operators/          # Fixed scoring operators
   │  └─ debt/               # Debt calculation
   ├─ domain/                 # Domain models
   ├─ repository/mongo/       # MongoDB integration
   └─ config/                 # Spring config
```

## License

MIT
