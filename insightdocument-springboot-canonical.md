
# Insight Document (Java Edition – UPDATED)
## Configuration-Driven Engineering Health Scoring Framework
## Spring Boot + MongoDB

This document is the authoritative Java/Spring Boot context for implementing
the Engineering Health Scoring framework using Copilot Agent mode.

It supersedes earlier Java drafts and explicitly incorporates:
- Canonical form contracts
- Canonical-form-aware MongoDB configuration
- Code vs configuration change boundaries
- Guardrails to prevent hallucination

Do NOT mix this document with Python/FastAPI context when using Copilot.

---

## 1. Purpose

Ensure Copilot Agent:
- Treats canonical forms as core framework contracts
- Uses MongoDB only for interpretation and tuning
- Keeps Java code stable during discovery

This is an architectural contract.

---

## 2. Core Philosophy

Code remains stable while interpretation evolves.

Java defines:
- Contracts
- Operators
- Boundaries

MongoDB defines:
- Meaning
- Weights
- Priorities

---

## 3. Architecture Flow

External Tools → Parser → Adapter → Canonical Signals → Scoring → Debt → HealthScore → API

---

## 4. Canonical Data Forms (MANDATORY)

Canonical forms define allowed data shapes beyond adapters.

Supported forms:
- COUNTABLE_CATEGORY (Map<Enum, Integer>)
- SCALAR (BigDecimal / Double)
- BOOLEAN (Boolean)
- ENUM (Enum)

Metrics not fitting these must be reduced or excluded.

Canonical forms are code-defined, config-referenced.

---

## 5. Mandatory Package Structure

healthscore-platform/
 └─ src/main/java/com/org/healthscore/
    ├─ api/
    ├─ parser/
    ├─ adapter/
    ├─ core/
    │  ├─ scoring/
    │  ├─ operators/
    │  └─ debt/
    ├─ domain/
    ├─ repository/mongo/
    └─ config/

---

## 6. MongoDB Collections

adapter_signal_definitions  
signals  
signal_scoring_rules  
debt_signal_contributions  
debt_dimension_weights  
scores  

Each configuration references canonical forms but never redefines them.

---

## 7. Scoring Engine Rules

- Strategy pattern
- Operators fixed in code
- Parameters from MongoDB
- Canonical-form compatibility enforced

---

## 8. Adapter Responsibilities

- Config-driven extraction
- Canonical normalization
- No scoring or business meaning

---

## 9. Parser Responsibilities

- Handle tool API syntax only
- Tool-specific

---

## 10. Code vs Config Change Matrix

New metric → Config  
Weight change → Config  
Business priority change → Config  
Payload structure change → Code  
New canonical form → Code  
New operator → Code  

---

## 11. SYSTEM PROMPT FOR COPILOT AGENT (SPRING BOOT)

You are a senior Java backend engineer and software architect.

You are implementing a configuration-driven Engineering Health Scoring
framework using Spring Boot and MongoDB.

Rules:
- Java 17+, Spring Boot, MongoDB only
- Follow the defined package structure
- Canonical forms are framework contracts
- Adapters normalize data using MongoDB
- Scoring executes fixed operators only
- All business meaning lives in MongoDB
- Never hardcode weights or formulas
- Never invent operators or canonical forms

Prefer architectural stability over flexibility.

---

END OF DOCUMENT
