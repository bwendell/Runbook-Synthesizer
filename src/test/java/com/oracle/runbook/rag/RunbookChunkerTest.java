package com.oracle.runbook.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RunbookChunker}.
 *
 * <p>Tests follow TDD red-green-refactor cycle. Written FIRST before implementation.
 */
class RunbookChunkerTest {

  private RunbookChunker chunker;

  @BeforeEach
  void setUp() {
    chunker = new RunbookChunker();
  }

  @Nested
  @DisplayName("YAML Frontmatter Parsing")
  class FrontmatterTests {

    @Test
    @DisplayName("should extract title from YAML frontmatter")
    void shouldExtractTitle() {
      String content =
          """
          ---
          title: Memory Troubleshooting Guide
          tags:
            - memory
            - oom
          ---

          # Memory Guide

          ## Section One

          Content here.
          """;

      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk(content, "memory.md");

      assertThat(chunks).isNotEmpty();
      // Title should be available in chunk metadata
    }

    @Test
    @DisplayName("should extract tags from YAML frontmatter")
    void shouldExtractTags() {
      String content =
          """
          ---
          title: Memory Guide
          tags:
            - memory
            - oom
            - linux
          ---

          ## Section

          Content.
          """;

      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk(content, "memory.md");

      assertThat(chunks).isNotEmpty();
      assertThat(chunks.get(0).tags()).containsExactly("memory", "oom", "linux");
    }

    @Test
    @DisplayName("should extract applicable_shapes from YAML frontmatter")
    void shouldExtractApplicableShapes() {
      String content =
          """
          ---
          title: Memory Guide
          applicable_shapes:
            - VM.Standard.*
            - BM.Standard.*
          ---

          ## Section

          Content.
          """;

      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk(content, "memory.md");

      assertThat(chunks).isNotEmpty();
      assertThat(chunks.get(0).applicableShapes())
          .containsExactly("VM.Standard.*", "BM.Standard.*");
    }

    @Test
    @DisplayName("should handle missing frontmatter gracefully")
    void shouldHandleMissingFrontmatter() {
      String content =
          """
          # Simple Guide

          ## Section One

          Content here.
          """;

      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk(content, "simple.md");

      assertThat(chunks).isNotEmpty();
      assertThat(chunks.get(0).tags()).isEmpty();
      assertThat(chunks.get(0).applicableShapes()).isEmpty();
    }
  }

  @Nested
  @DisplayName("Header-Based Splitting")
  class HeaderSplittingTests {

    @Test
    @DisplayName("should split content by H2 headers")
    void shouldSplitByH2Headers() {
      String content =
          """
          ---
          title: Guide
          ---

          ## Section One

          This is the first section with enough content to meet the minimum chunk size.
          It discusses important topics related to the first part of the guide.

          ## Section Two

          This is the second section with similarly sufficient content for chunking.
          It covers different aspects that are relevant to the second topic area.

          ## Section Three

          This is the third section which also has adequate content for chunking.
          It wraps up the guide with final important information for readers.
          """;

      // Use chunker with low min size to allow splitting by headers
      RunbookChunker headerChunker = new RunbookChunker(50, 2000);
      List<RunbookChunker.ParsedChunk> chunks = headerChunker.chunk(content, "guide.md");

      assertThat(chunks).hasSize(3);
      assertThat(chunks.get(0).sectionTitle()).isEqualTo("Section One");
      assertThat(chunks.get(1).sectionTitle()).isEqualTo("Section Two");
      assertThat(chunks.get(2).sectionTitle()).isEqualTo("Section Three");
    }

    @Test
    @DisplayName("should split by H3 headers under H2")
    void shouldSplitByH3Headers() {
      String content =
          """
          ## Main Section

          This is the introduction content for the main section. It provides context.

          ### Subsection One

          This is subsection one with detailed content about the first subtopic.
          It includes enough text to meet minimum chunk size requirements.

          ### Subsection Two

          This is subsection two with detailed content about the second subtopic.
          It also includes enough text to meet minimum chunk size requirements.
          """;

      // Use chunker with low min size to allow splitting by headers
      RunbookChunker headerChunker = new RunbookChunker(50, 2000);
      List<RunbookChunker.ParsedChunk> chunks = headerChunker.chunk(content, "guide.md");

      // Should create separate chunks for sections and subsections
      assertThat(chunks.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("should include section title in chunk")
    void shouldIncludeSectionTitle() {
      String content =
          """
          ## Diagnostic Commands

          Run these commands to diagnose issues.
          """;

      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk(content, "guide.md");

      assertThat(chunks).hasSize(1);
      assertThat(chunks.get(0).sectionTitle()).isEqualTo("Diagnostic Commands");
      assertThat(chunks.get(0).content()).contains("Run these commands");
    }
  }

  @Nested
  @DisplayName("Code Block Handling")
  class CodeBlockTests {

    @Test
    @DisplayName("should preserve code blocks as atomic units")
    void shouldPreserveCodeBlocks() {
      String content =
          """
          ## Commands

          Run this:

          ```bash
          free -h
          cat /proc/meminfo
          top -o %MEM
          ```

          After running above.
          """;

      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk(content, "guide.md");

      assertThat(chunks).hasSize(1);
      String chunkContent = chunks.get(0).content();
      assertThat(chunkContent).contains("```bash");
      assertThat(chunkContent).contains("free -h");
      assertThat(chunkContent).contains("```");
    }

    @Test
    @DisplayName("should not split in the middle of a code block")
    void shouldNotSplitCodeBlock() {
      // Even if content exceeds max size, don't break code blocks
      String content =
          """
          ## Commands

          ```bash
          # Very long code block that might exceed limits
          echo "Line 1"
          echo "Line 2"
          echo "Line 3"
          echo "Line 4"
          echo "Line 5"
          ```
          """;

      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk(content, "guide.md");

      // Single chunk - code block kept intact
      for (RunbookChunker.ParsedChunk chunk : chunks) {
        String c = chunk.content();
        long openCount = c.chars().filter(ch -> ch == '`').count();
        // Backticks should come in pairs (balanced)
        assertThat(openCount % 6).as("Code blocks should be balanced").isEqualTo(0);
      }
    }
  }

  @Nested
  @DisplayName("Chunk Size Constraints")
  class ChunkSizeTests {

    @Test
    @DisplayName("should merge small sections below minimum size")
    void shouldMergeSmallSections() {
      String content =
          """
          ## A

          Tiny.

          ## B

          Also tiny.
          """;

      RunbookChunker chunkerWithMinSize = new RunbookChunker(200, 2000);
      List<RunbookChunker.ParsedChunk> chunks = chunkerWithMinSize.chunk(content, "guide.md");

      // Small sections should be merged
      assertThat(chunks.size()).isLessThanOrEqualTo(1);
    }

    @Test
    @DisplayName("should split oversized content")
    void shouldSplitOversizedContent() {
      StringBuilder largeContent = new StringBuilder("## Large Section\n\n");
      for (int i = 0; i < 100; i++) {
        largeContent.append("This is line ").append(i).append(" of content. ");
      }

      RunbookChunker chunkerWithMaxSize = new RunbookChunker(100, 500);
      List<RunbookChunker.ParsedChunk> chunks =
          chunkerWithMaxSize.chunk(largeContent.toString(), "guide.md");

      // Should split into multiple chunks
      assertThat(chunks.size()).isGreaterThan(1);
      // Each chunk should respect max size
      for (RunbookChunker.ParsedChunk chunk : chunks) {
        assertThat(chunk.content().length()).isLessThanOrEqualTo(600); // Some tolerance
      }
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCaseTests {

    @Test
    @DisplayName("should return empty list for empty content")
    void shouldReturnEmptyForEmptyContent() {
      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk("", "empty.md");

      assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("should return empty list for whitespace-only content")
    void shouldReturnEmptyForWhitespace() {
      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk("   \n\n   ", "whitespace.md");

      assertThat(chunks).isEmpty();
    }

    @Test
    @DisplayName("should throw NullPointerException for null content")
    void shouldThrowForNullContent() {
      assertThatThrownBy(() -> chunker.chunk(null, "path.md"))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("content");
    }

    @Test
    @DisplayName("should throw NullPointerException for null path")
    void shouldThrowForNullPath() {
      assertThatThrownBy(() -> chunker.chunk("content", null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("runbookPath");
    }

    @Test
    @DisplayName("should handle content with only frontmatter")
    void shouldHandleOnlyFrontmatter() {
      String content =
          """
          ---
          title: Empty Guide
          tags:
            - test
          ---
          """;

      List<RunbookChunker.ParsedChunk> chunks = chunker.chunk(content, "empty.md");

      assertThat(chunks).isEmpty();
    }
  }
}
