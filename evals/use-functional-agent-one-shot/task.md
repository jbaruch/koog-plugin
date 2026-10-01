# Build a Minimal Text-Transform Agent

## Problem/Feature Description

A developer wants a tiny Koog 1.3 agent whose entire job is: take a String, call the LLM with a fixed system prompt asking for a formal-English rewrite, trim whitespace from the response, return it. No tools, no branching, no retries, no subgraphs.

They asked for "the simplest possible shape — I don't want to learn the graph DSL for this." They're on OpenAI; API key is in the environment.

## Output Specification

Walk through what to build. Produce the agent construction as a single Kotlin snippet, labeled.
