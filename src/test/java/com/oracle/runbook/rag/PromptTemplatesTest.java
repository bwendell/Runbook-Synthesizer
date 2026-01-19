package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PromptTemplates}.
 *
 * <p>Validates that prompt templates contain required sections and placeholders to ensure LLM
 * prompt structure is maintained across changes.
 */
class PromptTemplatesTest {

  @Nested
  @DisplayName("SYSTEM_PROMPT tests")
  class SystemPromptTests {

    @Test
    @DisplayName("SYSTEM_PROMPT contains ALERT CONTEXT section reference")
    void systemPromptContainsAlertContextSection() {
      assertThat(PromptTemplates.SYSTEM_PROMPT).contains("ALERT CONTEXT");
    }

    @Test
    @DisplayName("SYSTEM_PROMPT contains RUNBOOK SECTIONS reference")
    void systemPromptContainsRunbookSectionsSection() {
      assertThat(PromptTemplates.SYSTEM_PROMPT)
          .containsIgnoringCase("RUNBOOK")
          .containsIgnoringCase("SECTION");
    }

    @Test
    @DisplayName("SYSTEM_PROMPT contains INSTRUCTIONS section")
    void systemPromptContainsInstructionsSection() {
      assertThat(PromptTemplates.SYSTEM_PROMPT).contains("INSTRUCTIONS");
    }

    @Test
    @DisplayName("SYSTEM_PROMPT contains all required sections")
    void systemPromptContainsRequiredSections() {
      assertThat(PromptTemplates.SYSTEM_PROMPT)
          .as("SYSTEM_PROMPT must contain ALERT CONTEXT, RUNBOOK SECTIONS, and INSTRUCTIONS")
          .contains("ALERT CONTEXT")
          .containsIgnoringCase("runbook")
          .contains("INSTRUCTIONS");
    }
  }

  @Nested
  @DisplayName("CONTEXT_TEMPLATE tests")
  class ContextTemplateTests {

    @Test
    @DisplayName(
        "CONTEXT_TEMPLATE has 5 placeholders for title, severity, message, resource, shape")
    void contextTemplateHasAllPlaceholders() {
      // Count occurrences of %s
      String template = PromptTemplates.CONTEXT_TEMPLATE;
      long placeholderCount = template.chars().filter(ch -> ch == '%').count();

      // Each %s counts as one placeholder
      assertThat(placeholderCount)
          .as(
              "CONTEXT_TEMPLATE should have 5 placeholders (title, severity, message, resource, shape)")
          .isEqualTo(5);
    }

    @Test
    @DisplayName("CONTEXT_TEMPLATE contains expected field labels")
    void contextTemplateContainsExpectedLabels() {
      assertThat(PromptTemplates.CONTEXT_TEMPLATE)
          .contains("Title:")
          .contains("Severity:")
          .contains("Message:")
          .contains("Resource:")
          .containsIgnoringCase("Shape");
    }
  }

  @Nested
  @DisplayName("CHUNK_TEMPLATE tests")
  class ChunkTemplateTests {

    @Test
    @DisplayName("CHUNK_TEMPLATE has 3 placeholders for runbook name, section, content")
    void chunkTemplateHasAllPlaceholders() {
      String template = PromptTemplates.CHUNK_TEMPLATE;
      long placeholderCount = template.chars().filter(ch -> ch == '%').count();

      assertThat(placeholderCount)
          .as("CHUNK_TEMPLATE should have 3 placeholders (runbook name, section, content)")
          .isEqualTo(3);
    }

    @Test
    @DisplayName("CHUNK_TEMPLATE contains expected field labels")
    void chunkTemplateContainsExpectedLabels() {
      assertThat(PromptTemplates.CHUNK_TEMPLATE)
          .contains("Runbook:")
          .containsIgnoringCase("Section")
          .containsIgnoringCase("Content");
    }
  }

  @Nested
  @DisplayName("GENERATE_INSTRUCTION tests")
  class GenerateInstructionTests {

    @Test
    @DisplayName("GENERATE_INSTRUCTION is not empty")
    void generateInstructionNotEmpty() {
      assertThat(PromptTemplates.GENERATE_INSTRUCTION)
          .as("GENERATE_INSTRUCTION must not be empty")
          .isNotBlank();
    }

    @Test
    @DisplayName("GENERATE_INSTRUCTION mentions checklist generation")
    void generateInstructionMentionsChecklist() {
      assertThat(PromptTemplates.GENERATE_INSTRUCTION).containsIgnoringCase("checklist");
    }
  }
}
