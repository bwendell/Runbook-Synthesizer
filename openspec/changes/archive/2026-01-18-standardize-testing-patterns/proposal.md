# Change: Standardize Java Testing Patterns

## Why

An investigation of the test codebase using the `testing-patterns-java` skill revealed **6 categories of pattern violations** across ~50 unit test files and ~17 integration tests. These inconsistencies reduce code maintainability, produce less informative test failures, and violate the established testing standards documented in the project's skills.

Key issues discovered:
- **Inconsistent assertion libraries**: ~50 files use JUnit assertions while ~17 use AssertJ (the recommended approach)
- **Mock verify() anti-patterns**: Tests verify mock behavior instead of actual outcomes
- **Reflection-based testing**: Tests check implementation details via reflection instead of behavior
- **Duplicate test data construction**: Tests inline test data instead of using `TestFixtures`

## What Changes

### Test Quality Standards
- Migrate all tests to use **AssertJ** fluent assertions
- Replace mock `verify()` calls with **outcome-based assertions**
- Remove reflection-based contract tests and replace with **behavioral tests**
- Expand `TestFixtures` with additional fixture files

### Code Changes (by priority)
1. **HIGH**: Fix anti-pattern tests (`WebhookDispatcherTest.java`, `OciLoggingAdapterTest.java`)
2. **MEDIUM**: Migrate ~50 unit test files from JUnit to AssertJ assertions
3. **MEDIUM**: Update tests to use `TestFixtures` instead of inline data construction
4. **LOW**: Clean up `Objects.requireNonNull()` wrappers in `TestFixturesTest.java`

### Spec Modifications
- **MODIFIED** `integration-tests`: Add requirement for assertion library standards
- **ADDED** `integration-tests`: New requirement for test fixture usage

## Impact

- **Affected specs**: `integration-tests`
- **Affected code**: 
  - ~50 unit test files in `src/test/java/.../`
  - 3 files with mock verify() anti-patterns
  - 2 files with reflection-based testing
  - `TestFixtures.java` and fixture resources
- **Risk**: Low - changes are purely in test code, no production impact
- **Effort**: Medium - bulk migration can be systematized

## Reference

Investigation findings documented in: [testing_patterns_investigation.md](file:///C:/Users/bwend/.gemini/antigravity/brain/d882368d-8238-4018-91e6-0f3b80a55cda/testing_patterns_investigation.md)

Skill reference: [testing-patterns-java/SKILL.md](file:///c:/Users/bwend/repos/ops-scribe/.agent/skills/testing-patterns-java/SKILL.md)
