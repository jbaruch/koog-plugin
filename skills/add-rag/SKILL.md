---
name: add-rag
description: >
  Add Retrieval-Augmented Generation to a Koog 1.2 agent — pick an embedding source
  (LLM-backed or local), index documents into a vector store, and query the store
  inside the agent's prompt pipeline or as a tool. Use when the user asks to "add
  RAG", "embed and search documents", "use a vector store", "build retrieval-augmented
  generation", or describes grounding the LLM in a corpus.
---

# Add RAG Skill

Process steps in order. Do not skip ahead.

## Step 1 — Add the Modules

RAG in Koog spans three concerns — each is its own module:

```kotlin
implementation("ai.koog:embeddings-base:1.2.0")
implementation("ai.koog:embeddings-llm:1.2.0")     // LLM-backed embeddings (OpenAI/Google/etc.)
implementation("ai.koog:rag-base:1.2.0")
implementation("ai.koog:rag-vector:1.2.0-beta")         // vector store + similarity search
```

Reach past these only for backend-specific vector stores (Pinecone, Qdrant, etc.) — those usually ship as separate `rag-vector-<backend>` artifacts when supported.

Proceed immediately to Step 2.

## Step 2 — Build the Embedder

LLM-backed embeddings via `embeddings-llm` use the same provider you'd use for completions:

```kotlin
import ai.koog.embeddings.llm.OpenAIEmbedder
import ai.koog.prompt.executor.clients.openai.OpenAIModels

val embedder = OpenAIEmbedder(
    apiKey = System.getenv("OPENAI_API_KEY"),
    model = OpenAIModels.Embedding.TextEmbedding3Small,
)
```

Cheaper local embedders exist for self-hosted setups — check the `embeddings-*` module list for what 1.0 ships beyond LLM-backed (Ollama-based embedders, in-process models).

Proceed immediately to Step 3.

## Step 3 — Build the Vector Store

```kotlin
import ai.koog.rag.vector.InMemoryVectorStore

val store = InMemoryVectorStore(embedder = embedder)

// Index documents
documents.forEach { doc ->
    store.add(id = doc.id, text = doc.text, metadata = mapOf("source" to doc.path))
}
```

`InMemoryVectorStore` is fine for demos and small corpora. For production, swap in a persistent backend — JDBC, Pinecone, Qdrant, Weaviate — via the corresponding `rag-vector-*` module if shipped, or implement the `VectorStore` interface against your store of choice.

Proceed immediately to Step 4.

## Step 4 — Wire Retrieval into the Agent

Two integration patterns:

**(a) RAG as a tool** — the LLM decides when to retrieve. Wrap the store in a tool:

```kotlin
@LLMDescription("Search the documentation corpus")
class DocsSearchTool(private val store: VectorStore) : ToolSet {
    @Tool
    @LLMDescription("Search for documents relevant to the query and return the top matches")
    suspend fun search(@LLMDescription("Search query") query: String): String {
        val results = store.search(query, topK = 5)
        return results.joinToString("\n---\n") { "${it.metadata["source"]}: ${it.text}" }
    }
}
```

Register in the agent's `ToolRegistry`. Use this pattern when retrieval is optional — some queries need it, some don't.

**(b) RAG as a prompt augmenter** — retrieval happens automatically before every LLM call. Use a `UserPromptAugmenter` (see `define-prompt`) that calls `store.search(currentInput)` and appends results to the user message. Use this pattern when retrieval is always-on for every input.

Proceed immediately to Step 5.

## Step 5 — Tune Retrieval

- `topK` — number of documents returned. Default 5 is reasonable; raise for broader context, lower for tighter prompts
- Chunking — index full documents or split into chunks? Chunked indexing produces better recall but loses cross-chunk context. The right answer depends on document length and query shape
- Reranking — if recall is high but precision is low, add a reranker between `store.search` and the LLM prompt. Koog doesn't ship a built-in reranker; call a cross-encoder model or a second LLM with a scoring prompt

Finish here.
