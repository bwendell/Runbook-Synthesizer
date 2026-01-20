package com.oracle.runbook.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses markdown runbooks into semantic chunks for vector storage.
 *
 * <p>Handles YAML frontmatter extraction, header-based splitting, code block preservation, and
 * chunk size constraints.
 *
 * @see RunbookIngestionService
 */
public class RunbookChunker {

  /** Default minimum chunk size in characters. */
  public static final int DEFAULT_MIN_CHUNK_SIZE = 100;

  /** Default maximum chunk size in characters. */
  public static final int DEFAULT_MAX_CHUNK_SIZE = 2000;

  // Regex patterns
  private static final Pattern FRONTMATTER_PATTERN =
      Pattern.compile("^---\\s*\\n(.*?)\\n---\\s*\\n?", Pattern.DOTALL);
  private static final Pattern TITLE_PATTERN =
      Pattern.compile("^title:\\s*(.+)$", Pattern.MULTILINE);
  private static final Pattern TAGS_PATTERN =
      Pattern.compile("tags:\\s*\\n((?:\\s+-\\s*.+\\n?)+)", Pattern.MULTILINE);
  private static final Pattern SHAPES_PATTERN =
      Pattern.compile("applicable_shapes:\\s*\\n((?:\\s+-\\s*.+\\n?)+)", Pattern.MULTILINE);
  private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("-\\s*(.+)");
  private static final Pattern HEADER_PATTERN =
      Pattern.compile("^(#{2,3})\\s+(.+)$", Pattern.MULTILINE);
  private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[^`]*```", Pattern.DOTALL);

  private final int minChunkSize;
  private final int maxChunkSize;

  /** Creates a chunker with default size constraints. */
  public RunbookChunker() {
    this(DEFAULT_MIN_CHUNK_SIZE, DEFAULT_MAX_CHUNK_SIZE);
  }

  /**
   * Creates a chunker with custom size constraints.
   *
   * @param minChunkSize minimum chunk size in characters
   * @param maxChunkSize maximum chunk size in characters
   */
  public RunbookChunker(int minChunkSize, int maxChunkSize) {
    this.minChunkSize = minChunkSize;
    this.maxChunkSize = maxChunkSize;
  }

  /**
   * Chunks the runbook content into semantic sections.
   *
   * @param content the markdown content to chunk
   * @param runbookPath the path to the source runbook file
   * @return list of parsed chunks, never null
   * @throws NullPointerException if content or runbookPath is null
   */
  public List<ParsedChunk> chunk(String content, String runbookPath) {
    Objects.requireNonNull(content, "content cannot be null");
    Objects.requireNonNull(runbookPath, "runbookPath cannot be null");

    if (content.isBlank()) {
      return List.of();
    }

    // Extract frontmatter metadata
    Frontmatter frontmatter = extractFrontmatter(content);
    String bodyContent = removeFrontmatter(content);

    if (bodyContent.isBlank()) {
      return List.of();
    }

    // Split by headers
    List<Section> sections = splitByHeaders(bodyContent);

    if (sections.isEmpty()) {
      return List.of();
    }

    // Apply size constraints and create chunks
    // Strategy: each section becomes its own chunk if >= minChunkSize
    //           small sections get merged with next section
    //           oversized sections get split
    List<ParsedChunk> chunks = new ArrayList<>();
    StringBuilder mergedContent = new StringBuilder();
    String mergedTitle = null;

    for (Section section : sections) {
      if (section.content().isBlank()) {
        continue;
      }

      String sectionContent = section.content();

      // If we have pending merged content, check if we should flush it
      if (mergedContent.length() > 0) {
        // Check if adding this section would exceed max
        if (mergedContent.length() + sectionContent.length() > maxChunkSize) {
          // Flush the merged content first
          flushChunk(chunks, mergedTitle, mergedContent.toString(), frontmatter);
          mergedContent = new StringBuilder();
          mergedTitle = null;
        }
      }

      // Start new merge buffer if empty
      if (mergedTitle == null) {
        mergedTitle = section.title();
      }
      if (mergedContent.length() > 0) {
        mergedContent.append("\n\n");
      }
      mergedContent.append(sectionContent);

      // Section is big enough - output as its own chunk
      if (mergedContent.length() >= minChunkSize) {
        // Check if it exceeds max size (need to split)
        if (mergedContent.length() > maxChunkSize) {
          List<String> splitParts = splitLargeContent(mergedContent.toString(), maxChunkSize);
          for (int i = 0; i < splitParts.size(); i++) {
            String part = splitParts.get(i);
            if (!part.isBlank()) {
              chunks.add(
                  new ParsedChunk(
                      mergedTitle + (i > 0 ? " (cont.)" : ""),
                      part.trim(),
                      frontmatter.tags(),
                      frontmatter.applicableShapes()));
            }
          }
        } else {
          // Normal case - output as chunk
          chunks.add(
              new ParsedChunk(
                  mergedTitle,
                  mergedContent.toString().trim(),
                  frontmatter.tags(),
                  frontmatter.applicableShapes()));
        }
        mergedContent = new StringBuilder();
        mergedTitle = null;
      }
      // else: section is too small, keep in buffer to merge with next
    }

    // Flush remaining content
    if (mergedContent.length() > 0) {
      String remaining = mergedContent.toString().trim();
      if (!remaining.isBlank()) {
        // If below min size and we have previous chunks, merge with last
        if (remaining.length() < minChunkSize && !chunks.isEmpty()) {
          ParsedChunk last = chunks.remove(chunks.size() - 1);
          chunks.add(
              new ParsedChunk(
                  last.sectionTitle(),
                  last.content() + "\n\n" + remaining,
                  last.tags(),
                  last.applicableShapes()));
        } else {
          chunks.add(
              new ParsedChunk(
                  mergedTitle != null ? mergedTitle : "Untitled",
                  remaining,
                  frontmatter.tags(),
                  frontmatter.applicableShapes()));
        }
      }
    }

    return chunks;
  }

  private void flushChunk(
      List<ParsedChunk> chunks, String title, String content, Frontmatter frontmatter) {
    if (content != null && !content.isBlank()) {
      chunks.add(
          new ParsedChunk(
              title != null ? title : "Untitled",
              content.trim(),
              frontmatter.tags(),
              frontmatter.applicableShapes()));
    }
  }

  private Frontmatter extractFrontmatter(String content) {
    Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
    if (!matcher.find()) {
      return new Frontmatter(null, List.of(), List.of());
    }

    String yaml = matcher.group(1);

    // Extract title
    String title = null;
    Matcher titleMatcher = TITLE_PATTERN.matcher(yaml);
    if (titleMatcher.find()) {
      title = titleMatcher.group(1).trim();
    }

    // Extract tags
    List<String> tags = extractList(yaml, TAGS_PATTERN);

    // Extract applicable_shapes
    List<String> shapes = extractList(yaml, SHAPES_PATTERN);

    return new Frontmatter(title, tags, shapes);
  }

  private List<String> extractList(String yaml, Pattern pattern) {
    Matcher matcher = pattern.matcher(yaml);
    if (!matcher.find()) {
      return List.of();
    }

    String listBlock = matcher.group(1);
    List<String> items = new ArrayList<>();
    Matcher itemMatcher = LIST_ITEM_PATTERN.matcher(listBlock);
    while (itemMatcher.find()) {
      items.add(itemMatcher.group(1).trim());
    }
    return items;
  }

  private String removeFrontmatter(String content) {
    return FRONTMATTER_PATTERN.matcher(content).replaceFirst("");
  }

  private List<Section> splitByHeaders(String content) {
    List<Section> sections = new ArrayList<>();
    Matcher matcher = HEADER_PATTERN.matcher(content);

    int lastEnd = 0;
    String currentTitle = "Introduction";

    while (matcher.find()) {
      // Save content before this header
      if (lastEnd < matcher.start()) {
        String sectionContent = content.substring(lastEnd, matcher.start()).trim();
        if (!sectionContent.isBlank()) {
          sections.add(new Section(currentTitle, sectionContent));
        }
      }

      currentTitle = matcher.group(2).trim();
      lastEnd = matcher.end();
    }

    // Save remaining content after last header
    if (lastEnd < content.length()) {
      String remaining = content.substring(lastEnd).trim();
      if (!remaining.isBlank()) {
        sections.add(new Section(currentTitle, remaining));
      }
    }

    return sections;
  }

  private List<String> splitLargeContent(String content, int maxSize) {
    List<String> parts = new ArrayList<>();

    // Find all code blocks and their positions
    List<int[]> codeBlocks = new ArrayList<>();
    Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(content);
    while (codeMatcher.find()) {
      codeBlocks.add(new int[] {codeMatcher.start(), codeMatcher.end()});
    }

    int start = 0;
    while (start < content.length()) {
      int end = Math.min(start + maxSize, content.length());

      // Check if we're cutting through a code block
      for (int[] block : codeBlocks) {
        if (start < block[1] && end > block[0] && end < block[1]) {
          // We're cutting through a code block - include the whole block
          end = block[1];
          break;
        }
      }

      // Try to break at a paragraph boundary
      if (end < content.length()) {
        int lastParagraph = content.lastIndexOf("\n\n", end);
        if (lastParagraph > start + minChunkSize) {
          end = lastParagraph;
        }
      }

      parts.add(content.substring(start, end));
      start = end;

      // Skip whitespace
      while (start < content.length() && Character.isWhitespace(content.charAt(start))) {
        start++;
      }
    }

    return parts;
  }

  private record Frontmatter(String title, List<String> tags, List<String> applicableShapes) {}

  private record Section(String title, String content) {}

  /**
   * Represents a parsed chunk of runbook content.
   *
   * @param sectionTitle the title of this section
   * @param content the actual text content
   * @param tags semantic tags from frontmatter
   * @param applicableShapes compute shapes this applies to
   */
  public record ParsedChunk(
      String sectionTitle, String content, List<String> tags, List<String> applicableShapes) {

    /** Compact constructor with defensive copies. */
    public ParsedChunk {
      tags = tags != null ? List.copyOf(tags) : List.of();
      applicableShapes = applicableShapes != null ? List.copyOf(applicableShapes) : List.of();
    }
  }
}
