# Change: Scaffold Project Foundation for Runbook-Synthesizer

## Why

The Runbook-Synthesizer project requires a properly configured Helidon SE foundation with Maven build system, Java 25, and the correct directory structure before any domain or business logic can be implemented. This phase establishes the project scaffolding that all subsequent phases depend on.

## What Changes

- Create Maven `pom.xml` with:
  - Java 25 compiler settings
  - Helidon SE BOM for dependency management
  - OCI SDK BOM for future OCI integrations
  - LangChain4j dependencies (version-managed)
  - JUnit 5 + Mockito for TDD
  - Maven plugins (compiler, surefire, exec, jib)

- Create Helidon SE application entry point:
  - `RunbookSynthesizerApp.java` with main method
  - Basic health endpoint (`/health`)
  - Graceful server lifecycle

- Create `application.yaml` configuration:
  - Server port and host
  - Logging configuration
  - Placeholder sections for future OCI/LLM config

- Establish directory structure per DESIGN.md:
  - `src/main/java/com/oracle/runbook/` (domain, ingestion, enrichment, rag, api, output, config)
  - `src/main/resources/` (configuration files)
  - `src/test/java/` (test mirror structure)
  - `src/test/resources/sample-runbooks/`
  - `examples/runbooks/` (sample runbook templates)
  - `docs/` (architecture documentation)

- Create placeholder package-info.java files to establish packages

## Impact

- **Affected specs**: New `project-foundation` capability
- **Affected code**: All new files (greenfield project scaffolding)
- **Dependencies**: None (this is the foundation layer)
- **Downstream phases**: All subsequent phases (2-8) depend on this scaffolding
