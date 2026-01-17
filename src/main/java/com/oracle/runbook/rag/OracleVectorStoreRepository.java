package com.oracle.runbook.rag;

import com.oracle.runbook.domain.RunbookChunk;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;

import java.util.List;
import java.util.Objects;

/**
 * Oracle Database 23ai implementation of {@link VectorStoreRepository} using
 * LangChain4j's EmbeddingStore abstraction.
 * <p>
 * This implementation maps between domain {@link RunbookChunk} objects and
 * LangChain4j's {@link TextSegment}/{@link Embedding} types, storing runbook
 * metadata for later filtering and retrieval.
 *
 * @see VectorStoreRepository
 * @see EmbeddingStore
 */
public class OracleVectorStoreRepository implements VectorStoreRepository {

	private static final String METADATA_ID = "id";
	private static final String METADATA_RUNBOOK_PATH = "runbookPath";
	private static final String METADATA_SECTION_TITLE = "sectionTitle";
	private static final String METADATA_TAGS = "tags";
	private static final String METADATA_APPLICABLE_SHAPES = "applicableShapes";

	private final EmbeddingStore<TextSegment> embeddingStore;

	/**
	 * Creates a new OracleVectorStoreRepository with the given embedding store.
	 *
	 * @param embeddingStore
	 *            the LangChain4j embedding store to use
	 * @throws NullPointerException
	 *             if embeddingStore is null
	 */
	public OracleVectorStoreRepository(EmbeddingStore<TextSegment> embeddingStore) {
		this.embeddingStore = Objects.requireNonNull(embeddingStore, "embeddingStore cannot be null");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void store(RunbookChunk chunk) {
		Objects.requireNonNull(chunk, "chunk cannot be null");

		TextSegment segment = toTextSegment(chunk);
		Embedding embedding = Embedding.from(chunk.embedding());

		embeddingStore.add(embedding, segment);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void storeBatch(List<RunbookChunk> chunks) {
		Objects.requireNonNull(chunks, "chunks cannot be null");

		List<TextSegment> segments = chunks.stream().map(this::toTextSegment).toList();

		List<Embedding> embeddings = chunks.stream()
				.map(chunk -> Embedding.from(chunk.embedding()))
				.toList();

		embeddingStore.addAll(embeddings, segments);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<RunbookChunk> search(float[] queryEmbedding, int topK) {
		Objects.requireNonNull(queryEmbedding, "queryEmbedding cannot be null");
		if (topK <= 0) {
			throw new IllegalArgumentException("topK must be positive");
		}

		Embedding query = Embedding.from(queryEmbedding);
		EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
				.queryEmbedding(query)
				.maxResults(topK)
				.build();

		EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);

		return result.matches().stream()
				.map(this::toRunbookChunk)
				.toList();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(String runbookPath) {
		Objects.requireNonNull(runbookPath, "runbookPath cannot be null");

		// Create a filter to match documents by runbookPath metadata
		Filter filter = new IsEqualTo(METADATA_RUNBOOK_PATH, runbookPath);
		embeddingStore.removeAll(filter);
	}

	/**
	 * Converts a domain RunbookChunk to a LangChain4j TextSegment with metadata.
	 */
	private TextSegment toTextSegment(RunbookChunk chunk) {
		Metadata metadata = new Metadata();
		metadata.put(METADATA_ID, chunk.id());
		metadata.put(METADATA_RUNBOOK_PATH, chunk.runbookPath());
		if (chunk.sectionTitle() != null) {
			metadata.put(METADATA_SECTION_TITLE, chunk.sectionTitle());
		}
		metadata.put(METADATA_TAGS, String.join(",", chunk.tags()));
		metadata.put(METADATA_APPLICABLE_SHAPES, String.join(",", chunk.applicableShapes()));

		return TextSegment.from(chunk.content(), metadata);
	}

	/**
	 * Converts a LangChain4j EmbeddingMatch back to a domain RunbookChunk.
	 */
	private RunbookChunk toRunbookChunk(EmbeddingMatch<TextSegment> match) {
		TextSegment segment = match.embedded();
		Metadata metadata = segment.metadata();

		String id = metadata.getString(METADATA_ID);
		String runbookPath = metadata.getString(METADATA_RUNBOOK_PATH);
		String sectionTitle = metadata.getString(METADATA_SECTION_TITLE);
		String content = segment.text();

		// Parse comma-separated tags and shapes back to lists
		String tagsStr = metadata.getString(METADATA_TAGS);
		List<String> tags = (tagsStr != null && !tagsStr.isEmpty())
				? List.of(tagsStr.split(","))
				: List.of();

		String shapesStr = metadata.getString(METADATA_APPLICABLE_SHAPES);
		List<String> applicableShapes = (shapesStr != null && !shapesStr.isEmpty())
				? List.of(shapesStr.split(","))
				: List.of();

		// Extract embedding from match
		float[] embedding = match.embedding().vector();

		return new RunbookChunk(id, runbookPath, sectionTitle, content, tags, applicableShapes, embedding);
	}
}
