package com.oracle.runbook.integration.containers;

import com.oracle.runbook.domain.RunbookChunk;
import com.oracle.runbook.rag.VectorStoreRepository;
import java.util.List;

/**
 * Seeds runbook chunks into the vector store for E2E testing.
 *
 * <p>This class provides pre-built runbook scenarios for common test cases:
 *
 * <ul>
 *   <li>Memory troubleshooting runbooks
 *   <li>CPU troubleshooting runbooks
 *   <li>Disk troubleshooting runbooks
 *   <li>Network troubleshooting runbooks
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * RunbookSeeder seeder = new RunbookSeeder(vectorStore, embeddingService);
 * seeder.seedMemoryRunbook();
 * seeder.seedAllRunbooks();
 * }</pre>
 *
 * <p>Follows the testing-patterns skill: factory functions with sensible defaults and property
 * overrides.
 */
public class RunbookSeeder {

  private final VectorStoreRepository vectorStore;
  private final EmbeddingGenerator embeddingGenerator;

  /**
   * Functional interface for embedding generation.
   *
   * <p>Allows injection of real (Ollama) or test embedding generators.
   */
  @FunctionalInterface
  public interface EmbeddingGenerator {
    /**
     * Generate an embedding for the given text.
     *
     * @param text the text to embed
     * @return 768-dimensional float array
     */
    float[] embed(String text);
  }

  /**
   * Creates a RunbookSeeder with the given vector store and embedding generator.
   *
   * @param vectorStore the vector store to seed chunks into
   * @param embeddingGenerator the embedding generator (real or test)
   */
  public RunbookSeeder(VectorStoreRepository vectorStore, EmbeddingGenerator embeddingGenerator) {
    this.vectorStore = vectorStore;
    this.embeddingGenerator = embeddingGenerator;
  }

  /**
   * Seeds a memory troubleshooting runbook with multiple chunks.
   *
   * <p>Includes chunks for:
   *
   * <ul>
   *   <li>Memory investigation (free -h, top)
   *   <li>Memory cleanup (cache clearing)
   *   <li>OOM troubleshooting
   * </ul>
   */
  public void seedMemoryRunbook() {
    String runbookPath = "runbooks/memory-troubleshooting.md";

    List<RunbookChunk> chunks =
        List.of(
            createChunk(
                "mem-001",
                runbookPath,
                "Memory Investigation",
                "Use 'free -h' to check available memory. Watch for low 'available' values. "
                    + "Use 'top -o %MEM' to identify memory-hungry processes.",
                List.of("memory", "linux", "investigation")),
            createChunk(
                "mem-002",
                runbookPath,
                "Memory Cleanup",
                "Clear page cache with 'sync; echo 3 > /proc/sys/vm/drop_caches'. "
                    + "Restart high-memory services if safe. Check for memory leaks.",
                List.of("memory", "linux", "cleanup")),
            createChunk(
                "mem-003",
                runbookPath,
                "OOM Troubleshooting",
                "Check dmesg for OOM killer messages: 'dmesg | grep -i oom'. "
                    + "Review /var/log/messages for killed processes. "
                    + "Consider increasing swap or adding memory.",
                List.of("memory", "oom", "linux")));

    vectorStore.storeBatch(chunks);
  }

  /**
   * Seeds a CPU troubleshooting runbook with multiple chunks.
   *
   * <p>Includes chunks for:
   *
   * <ul>
   *   <li>CPU usage investigation
   *   <li>Process identification
   *   <li>Load average analysis
   * </ul>
   */
  public void seedCpuRunbook() {
    String runbookPath = "runbooks/cpu-troubleshooting.md";

    List<RunbookChunk> chunks =
        List.of(
            createChunk(
                "cpu-001",
                runbookPath,
                "CPU Usage Investigation",
                "Use 'top' or 'htop' to view CPU usage. Check 'mpstat -P ALL 1' "
                    + "for per-core breakdown. High %iowait indicates disk bottleneck.",
                List.of("cpu", "linux", "performance")),
            createChunk(
                "cpu-002",
                runbookPath,
                "Load Average Analysis",
                "Check 'uptime' for load averages. Values above core count indicate "
                    + "CPU saturation. Use 'vmstat 1' for detailed system stats.",
                List.of("cpu", "linux", "load")),
            createChunk(
                "cpu-003",
                runbookPath,
                "Process Investigation",
                "Use 'ps aux --sort=-%cpu' to find CPU-hungry processes. "
                    + "Use 'strace -p PID' or 'perf top' for deep analysis. "
                    + "Consider nice/renice for runaway processes.",
                List.of("cpu", "linux", "process")));

    vectorStore.storeBatch(chunks);
  }

  /**
   * Seeds a disk troubleshooting runbook with multiple chunks.
   *
   * <p>Includes chunks for:
   *
   * <ul>
   *   <li>Disk space investigation
   *   <li>I/O performance analysis
   *   <li>Disk cleanup procedures
   * </ul>
   */
  public void seedDiskRunbook() {
    String runbookPath = "runbooks/disk-troubleshooting.md";

    List<RunbookChunk> chunks =
        List.of(
            createChunk(
                "disk-001",
                runbookPath,
                "Disk Space Investigation",
                "Use 'df -h' to check filesystem usage. Use 'du -sh /*' to find "
                    + "large directories. Check for large log files in /var/log.",
                List.of("disk", "linux", "storage")),
            createChunk(
                "disk-002",
                runbookPath,
                "I/O Performance",
                "Use 'iostat -x 1' for I/O statistics. High await times indicate "
                    + "disk contention. Use 'iotop' to find I/O-heavy processes.",
                List.of("disk", "linux", "io", "performance")));

    vectorStore.storeBatch(chunks);
  }

  /**
   * Seeds a network troubleshooting runbook with multiple chunks.
   *
   * <p>Includes chunks for:
   *
   * <ul>
   *   <li>Connectivity testing
   *   <li>DNS troubleshooting
   *   <li>Network performance analysis
   * </ul>
   */
  public void seedNetworkRunbook() {
    String runbookPath = "runbooks/network-troubleshooting.md";

    List<RunbookChunk> chunks =
        List.of(
            createChunk(
                "net-001",
                runbookPath,
                "Connectivity Testing",
                "Use 'ping' to test basic connectivity. Use 'traceroute' to identify "
                    + "routing issues. Check 'netstat -tuln' for listening services.",
                List.of("network", "linux", "connectivity")),
            createChunk(
                "net-002",
                runbookPath,
                "DNS Troubleshooting",
                "Use 'nslookup' or 'dig' to test DNS resolution. Check /etc/resolv.conf "
                    + "for configured nameservers. Try 'host' for quick lookups.",
                List.of("network", "linux", "dns")));

    vectorStore.storeBatch(chunks);
  }

  /**
   * Seeds all available runbooks.
   *
   * <p>Convenience method that calls all seed* methods.
   */
  public void seedAllRunbooks() {
    seedMemoryRunbook();
    seedCpuRunbook();
    seedDiskRunbook();
    seedNetworkRunbook();
  }

  /**
   * Creates a custom runbook chunk with real embedding.
   *
   * @param id unique chunk identifier
   * @param runbookPath the source runbook file path
   * @param sectionTitle the section title within the runbook
   * @param content the actual text content
   * @param tags categorization tags
   * @return the created RunbookChunk
   */
  public RunbookChunk createChunk(
      String id, String runbookPath, String sectionTitle, String content, List<String> tags) {
    float[] embedding = embeddingGenerator.embed(content);
    return new RunbookChunk(
        id, runbookPath, sectionTitle, content, tags, List.of("VM.*"), embedding);
  }

  /**
   * Creates a test embedding generator that produces deterministic embeddings.
   *
   * <p>This is useful for tests that don't need real semantic similarity but still need consistent,
   * reproducible embeddings.
   *
   * @return a deterministic embedding generator
   */
  public static EmbeddingGenerator deterministicEmbeddingGenerator() {
    return text -> {
      // Create deterministic embedding based on text hash
      int hash = text.hashCode();
      float[] embedding = new float[768];
      java.util.Random random = new java.util.Random(hash);
      for (int i = 0; i < 768; i++) {
        embedding[i] = random.nextFloat();
      }
      // Normalize to unit vector
      float norm = 0;
      for (float v : embedding) {
        norm += v * v;
      }
      norm = (float) Math.sqrt(norm);
      for (int i = 0; i < 768; i++) {
        embedding[i] /= norm;
      }
      return embedding;
    };
  }
}
