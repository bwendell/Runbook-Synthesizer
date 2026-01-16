<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

---

## Build Environment

**This project uses WSL (Windows Subsystem for Linux) for all build and test commands.**

### Running Commands

Use `wsl` prefix for all Maven/Java commands:

```powershell
# Run tests
wsl mvn test -Dtest=TestClassName

# Build project
wsl mvn clean compile

# Run all tests
wsl mvn test
```

### Why WSL?

- Java 25 and Maven are installed in WSL
- Consistent Linux-based build environment
- Matches CI/CD environment