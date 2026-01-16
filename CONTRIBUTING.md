# Contributing to Runbook-Synthesizer

Thank you for your interest in contributing to Runbook-Synthesizer!

## Development Setup

### Prerequisites

- Java 25+
- Maven 3.9+
- Git

### Building

```bash
# Clone and build
git clone https://github.com/oracle/runbook-synthesizer.git
cd runbook-synthesizer
mvn clean verify
```

### Running Tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=HealthEndpointTest

# With coverage
mvn test jacoco:report
```

## Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Add Javadoc to public APIs
- Keep methods small and focused

## Pull Request Process

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make your changes with tests
4. Run `mvn verify` to ensure all tests pass
5. Commit with conventional commit messages: `feat:`, `fix:`, `docs:`
6. Push and create a Pull Request

## Commit Messages

Use conventional commits:

```
feat: add Prometheus adapter
fix: handle null alerts gracefully
docs: update README with examples
test: add integration tests for webhook
```

## Questions?

Open an issue with your question or suggestion.
