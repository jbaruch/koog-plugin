# Ground an Agent in a Documentation Corpus

## Problem/Feature Description

A developer maintains an internal documentation site (~2000 pages of markdown). They want a Koog 1.3 agent that answers questions about those docs by retrieving relevant pages before responding. They've already loaded the markdown into a `List<Document>` where each `Document` has an `id`, `text`, and `path`.

The agent uses OpenAI. Retrieval should happen when the LLM decides it needs context — they specifically don't want every input augmented with retrieval results (some queries are conversational and don't need docs).

## Output Specification

Walk through the wiring — embedder, vector store, retrieval surface, agent integration. Produce the relevant code as a single Kotlin file, labeled.
