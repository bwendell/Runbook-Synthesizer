# Domain Models Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Create all core domain models for Runbook-Synthesizer as immutable Java record classes.

**Architecture:** Hexagonal/Clean Architecture domain layer - pure data models with no infrastructure dependencies. All models are immutable records with validation in compact constructors where needed.

**Tech Stack:** Java 25, JUnit 5, AssertJ

---

## Task 1: AlertSeverity Enum [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/AlertSeverity.java`
- Test: `src/test/java/com/oracle/runbook/domain/AlertSeverityTest.java`

**Hints:**
- Simple enum with three values: `CRITICAL`, `WARNING`, `INFO`
- Consider adding a `fromString()` factory method for parsing OCI alarm severity strings
- Test enum values exist and fromString handles case-insensitivity

**Step 1: Write the failing test**
- Test that `AlertSeverity.fromString("critical")` returns `CRITICAL`
- Test that unknown values throw `IllegalArgumentException`

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=AlertSeverityTest -f pom.xml
```
Expected: FAIL - class not found

**Step 3: Write minimal implementation**
- Create enum with `CRITICAL`, `WARNING`, `INFO`
- Add static `fromString(String)` method with case-insensitive matching

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=AlertSeverityTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/AlertSeverity.java src/test/java/com/oracle/runbook/domain/AlertSeverityTest.java
git commit -m "feat(domain): add AlertSeverity enum"
```

---

## Task 2: Alert Record [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/Alert.java`
- Test: `src/test/java/com/oracle/runbook/domain/AlertTest.java`

**Hints:**
- Record with 8 components: `id`, `title`, `message`, `severity`, `sourceService`, `dimensions`, `labels`, `timestamp`, `rawPayload`
- Use Java 25 record with compact constructor for null validation
- `dimensions` and `labels` are `Map<String, String>` - consider defensive copies
- Reference: `docs/DESIGN.md` lines 109-123

**Step 1: Write the failing test**
- Test construction with all required fields
- Test that null `id` or `title` throws `NullPointerException`
- Test immutability of dimensions/labels maps

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=AlertTest -f pom.xml
```
Expected: FAIL - class not found

**Step 3: Write minimal implementation**
- Create record with required components
- Add compact constructor with `Objects.requireNonNull` for mandatory fields
- Wrap dimensions/labels in `Map.copyOf()` for immutability

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=AlertTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/Alert.java src/test/java/com/oracle/runbook/domain/AlertTest.java
git commit -m "feat(domain): add Alert record with validation"
```

---

## Task 3: ResourceMetadata Record [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/ResourceMetadata.java`
- Test: `src/test/java/com/oracle/runbook/domain/ResourceMetadataTest.java`

**Hints:**
- Record with 7 components: `ocid`, `displayName`, `compartmentId`, `shape`, `availabilityDomain`, `freeformTags`, `definedTags`
- Tags are `Map<String, String>` - use defensive copies
- Reference: `docs/DESIGN.md` lines 178-186

**Step 1: Write the failing test**
- Test construction with all fields
- Test null `ocid` throws exception
- Test tag maps are immutable copies

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=ResourceMetadataTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create record matching DESIGN.md specification
- Validate required fields, wrap maps in `Map.copyOf()`

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=ResourceMetadataTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/ResourceMetadata.java src/test/java/com/oracle/runbook/domain/ResourceMetadataTest.java
git commit -m "feat(domain): add ResourceMetadata record"
```

---

## Task 4: MetricSnapshot Record [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/MetricSnapshot.java`
- Test: `src/test/java/com/oracle/runbook/domain/MetricSnapshotTest.java`

**Hints:**
- Record with 5 components: `metricName`, `namespace`, `value`, `unit`, `timestamp`
- Value is primitive `double`, timestamp is `java.time.Instant`
- Reference: `docs/DESIGN.md` lines 188-194

**Step 1: Write the failing test**
- Test construction with valid metric data
- Test null `metricName` or `timestamp` throws exception

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=MetricSnapshotTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create record with compact constructor validation

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=MetricSnapshotTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/MetricSnapshot.java src/test/java/com/oracle/runbook/domain/MetricSnapshotTest.java
git commit -m "feat(domain): add MetricSnapshot record"
```

---

## Task 5: LogEntry Record [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/LogEntry.java`
- Test: `src/test/java/com/oracle/runbook/domain/LogEntryTest.java`

**Hints:**
- Record with 5 components: `id`, `timestamp`, `level`, `message`, `metadata`
- Level could be an enum (`DEBUG`, `INFO`, `WARN`, `ERROR`) or String
- Metadata is `Map<String, String>` for flexible key-value pairs

**Step 1: Write the failing test**
- Test construction with all fields
- Test metadata map is defensively copied

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=LogEntryTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create record with optional `LogLevel` enum or String level
- Defensive copy metadata map

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=LogEntryTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/LogEntry.java src/test/java/com/oracle/runbook/domain/LogEntryTest.java
git commit -m "feat(domain): add LogEntry record"
```

---

## Task 6: EnrichedContext Record [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/EnrichedContext.java`
- Test: `src/test/java/com/oracle/runbook/domain/EnrichedContextTest.java`

**Hints:**
- Record with 5 components: `alert`, `resource`, `recentMetrics`, `recentLogs`, `customProperties`
- Depends on: `Alert`, `ResourceMetadata`, `MetricSnapshot`, `LogEntry`
- Lists and Map must be defensively copied
- Reference: `docs/DESIGN.md` lines 170-176

**Step 1: Write the failing test**
- Test construction with embedded domain objects
- Test lists are immutable copies (cannot modify after construction)
- Test null alert throws exception

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=EnrichedContextTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create record composing other domain models
- Use `List.copyOf()` and `Map.copyOf()` for defensive copies

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=EnrichedContextTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/EnrichedContext.java src/test/java/com/oracle/runbook/domain/EnrichedContextTest.java
git commit -m "feat(domain): add EnrichedContext record"
```

---

## Task 7: RunbookChunk Record [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/RunbookChunk.java`
- Test: `src/test/java/com/oracle/runbook/domain/RunbookChunkTest.java`

**Hints:**
- Record with 7 components: `id`, `runbookPath`, `sectionTitle`, `content`, `tags`, `applicableShapes`, `embedding`
- Embedding is `float[]` - must defensively copy array
- Tags and applicableShapes are `List<String>`
- Reference: `docs/DESIGN.md` lines 256-264

**Step 1: Write the failing test**
- Test construction with all fields
- Test embedding array is defensively copied (mutating original doesn't affect record)
- Test tags list is immutable

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=RunbookChunkTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create record with `Arrays.copyOf()` for embedding array
- Use `List.copyOf()` for tags and shapes

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=RunbookChunkTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/RunbookChunk.java src/test/java/com/oracle/runbook/domain/RunbookChunkTest.java
git commit -m "feat(domain): add RunbookChunk record with embedding"
```

---

## Task 8: RetrievedChunk Record [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/RetrievedChunk.java`
- Test: `src/test/java/com/oracle/runbook/domain/RetrievedChunkTest.java`

**Hints:**
- Record with 4 components: `chunk`, `similarityScore`, `metadataBoost`, `finalScore`
- Wraps `RunbookChunk` with retrieval scoring information
- Reference: `docs/DESIGN.md` lines 281-286

**Step 1: Write the failing test**
- Test construction with valid chunk and scores
- Test null chunk throws exception

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=RetrievedChunkTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create simple record with null validation for chunk

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=RetrievedChunkTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/RetrievedChunk.java src/test/java/com/oracle/runbook/domain/RetrievedChunkTest.java
git commit -m "feat(domain): add RetrievedChunk record"
```

---

## Task 9: StepPriority Enum [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/StepPriority.java`
- Test: `src/test/java/com/oracle/runbook/domain/StepPriorityTest.java`

**Hints:**
- Enum with values: `HIGH`, `MEDIUM`, `LOW`
- Consider ordinal comparison for prioritization

**Step 1: Write the failing test**
- Test enum values exist
- Test ordinal ordering (HIGH < MEDIUM < LOW numerically or reverse)

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=StepPriorityTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create simple enum with three values

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=StepPriorityTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/StepPriority.java src/test/java/com/oracle/runbook/domain/StepPriorityTest.java
git commit -m "feat(domain): add StepPriority enum"
```

---

## Task 10: ChecklistStep Record [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/ChecklistStep.java`
- Test: `src/test/java/com/oracle/runbook/domain/ChecklistStepTest.java`

**Hints:**
- Record with 7 components: `order`, `instruction`, `rationale`, `currentValue`, `expectedValue`, `priority`, `commands`
- `order` is `int`, `commands` is `List<String>`
- Reference: `docs/DESIGN.md` lines 366-374

**Step 1: Write the failing test**
- Test construction with all fields
- Test commands list is immutable
- Test null instruction throws exception

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=ChecklistStepTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create record with `List.copyOf()` for commands
- Validate required fields

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=ChecklistStepTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/ChecklistStep.java src/test/java/com/oracle/runbook/domain/ChecklistStepTest.java
git commit -m "feat(domain): add ChecklistStep record"
```

---

## Task 11: DynamicChecklist Record [M]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/DynamicChecklist.java`
- Test: `src/test/java/com/oracle/runbook/domain/DynamicChecklistTest.java`

**Hints:**
- Record with 6 components: `alertId`, `summary`, `steps`, `sourceRunbooks`, `generatedAt`, `llmProviderUsed`
- `steps` is `List<ChecklistStep>`, `sourceRunbooks` is `List<String>`
- Reference: `docs/DESIGN.md` lines 357-364

**Step 1: Write the failing test**
- Test construction with all fields
- Test lists are immutable
- Test null alertId throws exception

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=DynamicChecklistTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create record with defensive list copies
- Validate required fields

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=DynamicChecklistTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/DynamicChecklist.java src/test/java/com/oracle/runbook/domain/DynamicChecklistTest.java
git commit -m "feat(domain): add DynamicChecklist record"
```

---

## Task 12: GenerationConfig Record [S]

**Files:**
- Create: `src/main/java/com/oracle/runbook/domain/GenerationConfig.java`
- Test: `src/test/java/com/oracle/runbook/domain/GenerationConfigTest.java`

**Hints:**
- Record with 3 components: `temperature`, `maxTokens`, `modelOverride`
- `temperature` is `double`, `maxTokens` is `int`, `modelOverride` is `Optional<String>`
- Consider validation: temperature 0.0-1.0, maxTokens > 0
- Reference: `docs/DESIGN.md` lines 315-319

**Step 1: Write the failing test**
- Test construction with valid config
- Test temperature validation (throws for negative or > 1.0)
- Test maxTokens validation (throws for <= 0)

**Step 2: Run test to verify it fails**
```powershell
mvn test -Dtest=GenerationConfigTest -f pom.xml
```
Expected: FAIL

**Step 3: Write minimal implementation**
- Create record with validation in compact constructor

**Step 4: Run test to verify it passes**
```powershell
mvn test -Dtest=GenerationConfigTest -f pom.xml
```
Expected: PASS

**Step 5: Commit**
```powershell
git add src/main/java/com/oracle/runbook/domain/GenerationConfig.java src/test/java/com/oracle/runbook/domain/GenerationConfigTest.java
git commit -m "feat(domain): add GenerationConfig record"
```

---

## Task 13: Run All Domain Tests [S]

**Files:**
- None (verification only)

**Step 1: Run all domain model tests**
```powershell
mvn test -Dtest="com.oracle.runbook.domain.*Test" -f pom.xml
```
Expected: All 12 test classes PASS

**Step 2: Verify test coverage (optional)**
```powershell
mvn jacoco:report -f pom.xml
```
Expected: Domain package has >90% line coverage

**Step 3: Final commit**
```powershell
git add .
git commit -m "feat(domain): complete domain models with tests"
```

---

## Summary

| Task | Model | Complexity | Dependencies |
|------|-------|------------|--------------|
| 1 | AlertSeverity | [S] | None |
| 2 | Alert | [M] | AlertSeverity |
| 3 | ResourceMetadata | [S] | None |
| 4 | MetricSnapshot | [S] | None |
| 5 | LogEntry | [S] | None |
| 6 | EnrichedContext | [M] | Alert, ResourceMetadata, MetricSnapshot, LogEntry |
| 7 | RunbookChunk | [M] | None |
| 8 | RetrievedChunk | [S] | RunbookChunk |
| 9 | StepPriority | [S] | None |
| 10 | ChecklistStep | [M] | StepPriority |
| 11 | DynamicChecklist | [M] | ChecklistStep |
| 12 | GenerationConfig | [S] | None |
| 13 | All Tests | [S] | All above |

**Parallelizable:** Tasks 1, 3, 4, 5, 7, 9 can be done in parallel (no dependencies)
