<!-- Use this file to provide workspace-specific custom instructions to Copilot. For more details, visit https://code.visualstudio.com/docs/copilot/copilot-customization#_use-a-githubcopilotinstructionsmd-file -->

# Engineering Health Scoring Framework

You are a senior Java backend engineer working on a configuration-driven Engineering Health Scoring framework using Spring Boot and MongoDB.

## Core Architecture Principles

### Code vs Configuration Separation
- **Code defines**: Contracts, Operators, Boundaries
- **MongoDB defines**: Meaning, Weights, Priorities

### Canonical Forms (Framework Contracts)
The following canonical forms are code-defined and config-referenced. NEVER add new canonical forms without code change:
- `COUNTABLE_CATEGORY` - Map<String, Integer> for categorized counts
- `SCALAR` - BigDecimal/Double for numeric values
- `BOOLEAN` - Boolean for flag values  
- `ENUM` - String for enumerated values

### Fixed Operators
Operators are fixed in code. Parameters come from MongoDB. Available operators:
- `THRESHOLD_SCORE` - Scores SCALAR signals based on threshold ranges
- `WEIGHTED_CATEGORY_SUM` - Scores COUNTABLE_CATEGORY with weighted sum
- `BOOLEAN_PENALTY` - Scores BOOLEAN with true/false scores
- `ENUM_MAPPING` - Scores ENUM by mapping values to scores

## Package Structure
```
com.org.healthscore/
├── api/           # REST controllers and DTOs
├── parser/        # Tool-specific API parsers
├── adapter/       # Config-driven signal normalization
├── core/
│   ├── scoring/   # Health score computation
│   ├── operators/ # Fixed scoring operators
│   └── debt/      # Technical debt calculation
├── domain/        # Domain models
├── repository/mongo/ # MongoDB documents and repositories
└── config/        # Spring configuration
```

## Rules
- Java 17+, Spring Boot, MongoDB only
- Follow the defined package structure
- Canonical forms are framework contracts
- Adapters normalize data using MongoDB config
- Scoring executes fixed operators only
- All business meaning lives in MongoDB
- Never hardcode weights or formulas
- Never invent operators or canonical forms

## Code Change vs Config Change Matrix
| Change Type | Requires |
|-------------|----------|
| New metric | Config |
| Weight change | Config |
| Business priority change | Config |
| Payload structure change | Code |
| New canonical form | Code |
| New operator | Code |

Prefer architectural stability over flexibility.
