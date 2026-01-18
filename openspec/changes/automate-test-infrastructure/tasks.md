# Tasks: Automate Test Infrastructure

- [x] 1. OCI Authentication Enhancements <!-- id: 1 -->
    - [x] 1.1 Update `OciConfig` to handle environment variables <!-- id: 2 -->
        - **Verification**: Unit tests pass for `OciConfigTest` with new env var parsing logic.
        - **Acceptance**: `OciConfig.fromEnvironment()` returns a valid config when `OCI_USER_ID`, `OCI_TENANCY_ID`, `OCI_FINGERPRINT`, `OCI_REGION`, and `OCI_PRIVATE_KEY_CONTENT` are set.
    - [x] 1.2 Implement prioritized auth detection in `OciAuthProviderFactory` <!-- id: 3 -->
        - **Verification**: Unit tests for `OciAuthProviderFactoryTest` cover all priority scenarios.
        - **Acceptance**: Factory detects in order: (1) Resource Principals, (2) Instance Principals, (3) Environment Variables, (4) Config File. Each scenario returns the correct provider type.
    - [x] 1.3 Add support for Instance and Resource Principals <!-- id: 4 -->
        - **Verification**: Integration test (manual or CI on OCI) confirms auth works on OCI Compute.
        - **Acceptance**: When `OCI_RESOURCE_PRINCIPAL_VERSION` is set, `ResourcePrincipalsAuthenticationDetailsProvider` is returned. On OCI Compute without config, `InstancePrincipalsAuthenticationDetailsProvider` is returned.

- [x] 2. Oracle 23ai Container Integration <!-- id: 5 -->
    - [x] 2.1 Add Testcontainers dependencies to `pom.xml` <!-- id: 6 -->
        - **Verification**: `mvn dependency:resolve` succeeds with no conflicts.
        - **Acceptance**: `pom.xml` includes `testcontainers-core`, `testcontainers-oracle-free` (or equivalent), and `ojdbc11` for vector support.
    - [x] 2.2 Create `OracleContainerBase` test class <!-- id: 7 -->
        - **Verification**: A minimal test extending `OracleContainerBase` starts the container and connects via JDBC.
        - **Acceptance**: `OracleContainerBase` provides `@Container` lifecycle, exposes JDBC URL/user/password, and is reusable by all `*IT.java` tests.
    - [x] 2.3 Create centralized schema initialization script for vector store <!-- id: 8 -->
        - **Verification**: After container starts, `VECTOR` column type is available and `RUNBOOK_CHUNKS` table exists.
        - **Acceptance**: SQL script at `src/test/resources/schema/oracle-vector-schema.sql` runs on container startup via `withInitScript()`. This is the **canonical schema** used by all container-based tests (including `add-testcontainers-e2e`). Script includes `CREATE TABLE runbook_chunks` with `VECTOR(768, FLOAT32)` column and vector index.
    - [x] 2.4 Add Ollama container with shared network support <!-- id: 15 -->
        - **Verification**: A minimal test starts Ollama container alongside Oracle on a shared `Network` and can call `/api/tags` endpoint.
        - **Acceptance**: `OllamaContainerBase` or extension to `OracleContainerBase` provides `GenericContainer<?>` for `ollama/ollama:latest`, exposes port 11434, and shares the same Testcontainers `Network` as Oracle. Container waits for health via `Wait.forHttp("/api/tags")`.

- [x] 3. CI/CD Integration <!-- id: 9 -->
    - [x] 3.1 Document environment variable requirements for CI secrets <!-- id: 10 -->
        - **Verification**: `docs/CI_SETUP.md` exists and is readable.
        - **Acceptance**: Doc lists all required env vars (`OCI_*`), their expected formats, and links to OCI docs for key generation.
    - [x] 3.2 Add Maven profile `ci-integration` for non-Docker cloud tests <!-- id: 11 -->
        - **Verification**: `mvn verify -Pci-integration -DskipContainerTests` runs cloud-auth-only tests.
        - **Acceptance**: Profile excludes Testcontainers tests and uses real OCI services with env-var auth.

- [x] 4. Verification <!-- id: 12 -->
    - [x] 4.1 Migrate `VectorStoreIT` to use real Oracle 23ai container <!-- id: 13 -->
        - **Verification**: `mvn failsafe:integration-test -Dit.test=VectorStoreIT` passes with Docker running.
        - **Acceptance**: `VectorStoreIT` extends `OracleContainerBase`, stores/retrieves chunks using `OracleVectorStoreRepository`, and verifies similarity search results.
    - [ ] 4.2 Verify OCI adapter tests with real cloud credentials (optional/manual) <!-- id: 14 -->
        - **Verification**: Manual execution of `OciMonitoringAdapterIT` with valid `~/.oci/config` or env vars.
        - **Acceptance**: Test connects to OCI Monitoring, fetches real metrics (or gracefully handles empty response), and logs success.
