# RAG Pipeline Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the core RAG pipeline for semantic runbook retrieval and dynamic checklist generation.

**Architecture:** The RAG pipeline follows a standard retrieve-then-generate pattern. An `EmbeddingService` generates vector representations, `VectorStoreRepository` handles persistence in Oracle 23ai, `RunbookRetriever` combines vector search with metadata re-ranking, and `ChecklistGenerator` synthesizes the final troubleshooting checklist using the `LlmProvider`.

**Tech Stack:** Java 25, LangChain4j, Oracle 23ai Vector Search, JUnit 5, Mockito, AssertJ

---

## Task 1: Add LangChain4j Oracle Dependency [S]

**Files:**
- Modify: `pom.xml`

**Hints:**
- Add `langchain4j-oracle` dependency for Oracle 23ai vector store integration
- Add `langchain4j` core dependency if not already present
- Version should align with existing LangChain4j dependencies in pom.xml
- Reference: `docs/DESIGN.md` lines 567-575

**Step 1: Check existing LangChain4j dependencies**
```powershell
Select-String -Path pom.xml -Pattern "langchain4j"
```

**Step 2: Add required dependencies**
- Add `langchain4j-oracle` artifact
- Ensure version consistency with existing `langchain4j` dependency

**Step 3: Verify build compiles**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn compile -q"
```
Expected: BUILD SUCCESS

**Step 4: Commit**
```powershell
git add pom.xml
git commit -m "build: add langchain4j-oracle dependency for vector store"
```

---

## Task 2: EmbeddingService Interface and Implementation [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/EmbeddingService.java`
- Test: `src/test/java/com/oracle/runbook/rag/EmbeddingServiceTest.java`

**Hints:**
- Service wraps `LlmProvider.generateEmbedding()` and `generateEmbeddings()` methods
- Provides convenient methods for embedding single text and batch texts
- Inject `LlmProvider` via constructor
- Handle `CompletableFuture` results synchronously or provide async variants
- Consider adding a method to embed an `EnrichedContext` by converting it to a query string

**Step 1: Write the failing test**
- Test that `embedText(String)` calls `LlmProvider.generateEmbedding()` and returns the result
- Test that `embedTexts(List<String>)` calls `LlmProvider.generateEmbeddings()`
- Use Mockito to mock `LlmProvider`

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=EmbeddingServiceTest -q"
```
Expected: FAIL - class not found

**Step 3: Write minimal implementation**
- Create class with `LlmProvider` dependency
- Implement `embedText()` awaiting the `CompletableFuture`
- Implement `embedTexts()` for batch embedding
- Consider adding `embedContext(EnrichedContext)` that formats alert + context into query string

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=EmbeddingServiceTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/EmbeddingService.java src/test/java/com/oracle/runbook/rag/EmbeddingServiceTest.java
git commit -m "feat(rag): add EmbeddingService for vector generation"
```

---

## Task 3: VectorStoreRepository Interface [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/VectorStoreRepository.java`
- Test: `src/test/java/com/oracle/runbook/rag/VectorStoreRepositoryTest.java`

**Hints:**
- Interface defines contract for vector store operations
- Methods: `store(RunbookChunk)`, `storeBatch(List<RunbookChunk>)`, `findSimilar(float[] embedding, int topK)`
- `findSimilar` returns `List<RunbookChunk>` with similarity scores in order
- Keep interface implementation-agnostic (no Oracle-specific types)
- Reference: `docs/DESIGN.md` lines 667-668

**Step 1: Write the failing test**
- Test interface compilation and method contracts (stub test for interface)
- Alternatively, write tests for a mock implementation that verifies expected method signatures

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=VectorStoreRepositoryTest -q"
```
Expected: FAIL - interface not found

**Step 3: Write minimal implementation**
- Create interface with method signatures
- Add Javadoc explaining expected behavior

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=VectorStoreRepositoryTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/VectorStoreRepository.java src/test/java/com/oracle/runbook/rag/VectorStoreRepositoryTest.java
git commit -m "feat(rag): add VectorStoreRepository interface"
```

---

## Task 4: OracleVectorStoreRepository Implementation [L]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/OracleVectorStoreRepository.java`
- Test: `src/test/java/com/oracle/runbook/rag/OracleVectorStoreRepositoryTest.java`

**Hints:**
- Implements `VectorStoreRepository` using LangChain4j's `OracleEmbeddingStore`
- Inject `DataSource` or connection details via constructor
- Map between domain `RunbookChunk` and LangChain4j `TextSegment`/`Embedding` types
- Use `OracleEmbeddingStore.builder()` to configure the store
- Handle chunk metadata (tags, applicableShapes) as custom metadata fields
- Reference LangChain4j Oracle documentation for table schema

**Step 1: Write the failing test**
- Unit tests with mocked `OracleEmbeddingStore` (constructor injection)
- Test `store()` correctly converts `RunbookChunk` to LangChain4j types
- Test `findSimilar()` invokes the embedding store and maps results back

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=OracleVectorStoreRepositoryTest -q"
```
Expected: FAIL - class not found

**Step 3: Write minimal implementation**
- Implement `store()` converting `RunbookChunk.embedding()` to `Embedding`
- Implement `findSimilar()` using `OracleEmbeddingStore.findRelevant()`
- Map LangChain4j `EmbeddingMatch` back to `RunbookChunk`
- Handle null-safe metadata extraction

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=OracleVectorStoreRepositoryTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/OracleVectorStoreRepository.java src/test/java/com/oracle/runbook/rag/OracleVectorStoreRepositoryTest.java
git commit -m "feat(rag): add OracleVectorStoreRepository with LangChain4j"
```

---

## Task 5: RunbookRetriever Interface [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/RunbookRetriever.java`
- Test: `src/test/java/com/oracle/runbook/rag/RunbookRetrieverTest.java`

**Hints:**
- Interface signature from DESIGN.md: `List<RetrievedChunk> retrieve(EnrichedContext context, int topK)`
- Returns `RetrievedChunk` which includes similarity score and metadata boost
- Keep interface simple - single method
- Reference: `docs/DESIGN.md` lines 277-279

**Step 1: Write the failing test**
- Test interface exists and compiles
- Optional: create mock implementation to verify contract

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=RunbookRetrieverTest -q"
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Single-method interface with Javadoc

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=RunbookRetrieverTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/RunbookRetriever.java src/test/java/com/oracle/runbook/rag/RunbookRetrieverTest.java
git commit -m "feat(rag): add RunbookRetriever interface"
```

---

## Task 6: DefaultRunbookRetriever Implementation [L]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/DefaultRunbookRetriever.java`
- Test: `src/test/java/com/oracle/runbook/rag/DefaultRunbookRetrieverTest.java`

**Hints:**
- Implements `RunbookRetriever` interface
- Dependencies: `EmbeddingService`, `VectorStoreRepository`
- Algorithm:
  1. Embed the `EnrichedContext` into a query vector
  2. Call `VectorStoreRepository.findSimilar()` to get candidate chunks
  3. Apply metadata boost based on matching tags and `applicableShapes`
  4. Calculate `finalScore = similarityScore + metadataBoost`
  5. Sort by `finalScore` descending, return top-K
- Shape matching: use regex patterns from `applicableShapes` against `ResourceMetadata.shape()`
- Tag matching: boost chunks whose tags overlap with alert dimensions/labels

**Step 1: Write the failing test**
- Mock `EmbeddingService` and `VectorStoreRepository`
- Test that chunks with matching tags get higher `metadataBoost`
- Test that shape patterns correctly match/don't match
- Test sorting by `finalScore`

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=DefaultRunbookRetrieverTest -q"
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Inject dependencies via constructor
- Implement `retrieve()` with embedding, search, boost, and sort logic
- Extract boost calculation into private helper methods

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=DefaultRunbookRetrieverTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/DefaultRunbookRetriever.java src/test/java/com/oracle/runbook/rag/DefaultRunbookRetrieverTest.java
git commit -m "feat(rag): add DefaultRunbookRetriever with re-ranking"
```

---

## Task 7: PromptTemplates Constants [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/PromptTemplates.java`
- Test: `src/test/java/com/oracle/runbook/rag/PromptTemplatesTest.java`

**Hints:**
- Class with static final String constants for prompt templates
- Main template: `CHECKLIST_GENERATION_PROMPT` from DESIGN.md lines 379-398
- Template placeholders: `{alert.title}`, `{resource.displayName}`, `{resource.shape}`, `{metrics}`, `{logs}`, `{retrieved_chunks}`
- Consider adding helper method `formatPrompt(EnrichedContext, List<RetrievedChunk>)` that fills in placeholders
- Keep prompts as constants for easy modification/versioning

**Step 1: Write the failing test**
- Test that template constants contain expected placeholders
- Test `formatPrompt()` correctly substitutes values

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=PromptTemplatesTest -q"
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Define `CHECKLIST_GENERATION_PROMPT` constant
- Implement `formatPrompt()` using `String.replace()` or `MessageFormat`

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=PromptTemplatesTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/PromptTemplates.java src/test/java/com/oracle/runbook/rag/PromptTemplatesTest.java
git commit -m "feat(rag): add PromptTemplates with checklist generation prompt"
```

---

## Task 8: ChecklistGenerator Interface [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/ChecklistGenerator.java`
- Test: `src/test/java/com/oracle/runbook/rag/ChecklistGeneratorTest.java`

**Hints:**
- Interface signature from DESIGN.md: `DynamicChecklist generate(EnrichedContext context, List<RetrievedChunk> relevantChunks)`
- Returns `DynamicChecklist` domain model
- Keep interface simple - single method
- Reference: `docs/DESIGN.md` lines 350-355

**Step 1: Write the failing test**
- Test interface exists and compiles
- Optional: create mock implementation to verify contract

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=ChecklistGeneratorTest -q"
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Single-method interface with Javadoc

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=ChecklistGeneratorTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/ChecklistGenerator.java src/test/java/com/oracle/runbook/rag/ChecklistGeneratorTest.java
git commit -m "feat(rag): add ChecklistGenerator interface"
```

---

## Task 9: DefaultChecklistGenerator Implementation [L]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/DefaultChecklistGenerator.java`
- Test: `src/test/java/com/oracle/runbook/rag/DefaultChecklistGeneratorTest.java`

**Hints:**
- Implements `ChecklistGenerator` interface
- Dependencies: `LlmProvider`, `PromptTemplates`
- Algorithm:
  1. Format prompt using `PromptTemplates.formatPrompt()`
  2. Call `LlmProvider.generateText()` with prompt and `GenerationConfig`
  3. Parse LLM response into `DynamicChecklist` structure
  4. Extract source runbooks from `RetrievedChunk.chunk().runbookPath()`
- Response parsing: expect structured output (numbered steps with commands)
- Consider adding response parsing helper or using structured output mode if LLM supports it
- Handle LLM errors gracefully (return error checklist or throw specific exception)

**Step 1: Write the failing test**
- Mock `LlmProvider` to return a sample structured response
- Test that checklist is correctly parsed from LLM output
- Test that `sourceRunbooks` is populated from input chunks
- Test error handling when LLM returns malformed output

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=DefaultChecklistGeneratorTest -q"
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Inject `LlmProvider` via constructor
- Implement `generate()` with prompt formatting, LLM call, and response parsing
- Create private helper for parsing LLM text response into `ChecklistStep` objects
- Populate `DynamicChecklist` with all required fields

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=DefaultChecklistGeneratorTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/DefaultChecklistGenerator.java src/test/java/com/oracle/runbook/rag/DefaultChecklistGeneratorTest.java
git commit -m "feat(rag): add DefaultChecklistGenerator with LLM integration"
```

---

## Task 10: RagPipelineService Integration [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/rag/RagPipelineService.java`
- Test: `src/test/java/com/oracle/runbook/rag/RagPipelineServiceTest.java`

**Hints:**
- Top-level service that orchestrates the RAG pipeline
- Dependencies: `RunbookRetriever`, `ChecklistGenerator`
- Single method: `DynamicChecklist process(EnrichedContext context, int topK)`
- Coordinates retrieval and generation steps
- Add logging at key steps for observability
- Consider making `topK` configurable with default value

**Step 1: Write the failing test**
- Mock `RunbookRetriever` and `ChecklistGenerator`
- Test that `process()` calls retriever then generator
- Test that returned checklist is from generator

**Step 2: Run test to verify it fails**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=RagPipelineServiceTest -q"
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Inject dependencies via constructor
- Implement `process()` orchestrating the pipeline

**Step 4: Run test to verify it passes**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest=RagPipelineServiceTest -q"
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/rag/RagPipelineService.java src/test/java/com/oracle/runbook/rag/RagPipelineServiceTest.java
git commit -m "feat(rag): add RagPipelineService for pipeline orchestration"
```

---

## Task 11: Run All RAG Tests [S]

**Files:**
- None (verification only)

**Step 1: Run all RAG pipeline tests**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -Dtest='com.oracle.runbook.rag.*Test' -q"
```
Expected: All 9 test classes PASS

**Step 2: Run full test suite to verify no regressions**
```powershell
wsl bash -c "cd /mnt/c/Users/bwend/repos/ops-scribe && mvn test -q"
```
Expected: All tests PASS

**Step 3: Final commit**
```powershell
git add .
git commit -m "feat(rag): complete RAG pipeline implementation"
```

---

## Summary

| Task | Component | Complexity | Dependencies |
|------|-----------|------------|--------------|
| 1 | LangChain4j Oracle Dependency | [S] | None |
| 2 | EmbeddingService | [M] | LlmProvider (from ports-interfaces) |
| 3 | VectorStoreRepository Interface | [S] | None |
| 4 | OracleVectorStoreRepository | [L] | VectorStoreRepository, LangChain4j |
| 5 | RunbookRetriever Interface | [S] | None |
| 6 | DefaultRunbookRetriever | [L] | EmbeddingService, VectorStoreRepository |
| 7 | PromptTemplates | [S] | None |
| 8 | ChecklistGenerator Interface | [S] | None |
| 9 | DefaultChecklistGenerator | [L] | LlmProvider, PromptTemplates |
| 10 | RagPipelineService | [M] | RunbookRetriever, ChecklistGenerator |
| 11 | All Tests | [S] | All above |

**Parallelizable:** Tasks 3, 5, 7, 8 can be done in parallel (interfaces only, no dependencies)

**Critical Path:** Task 1 → Task 2 → Task 4 → Task 6 → Task 10
