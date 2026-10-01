# Expose a Koog Agent on an HTTP Endpoint

## Problem/Feature Description

A developer has a working Koog 1.3 agent (constructed in a `main()` with `runBlocking`). They want to host it behind a Ktor HTTP server — a single `POST /agent` route that takes the request body as the agent's input, runs the agent, and returns the result as the response body.

They have an existing Ktor `Application.module()` function in their codebase; they want the agent wired in alongside their other Ktor configuration. Their LLM provider is OpenAI; the API key comes from `OPENAI_API_KEY`.

## Output Specification

Walk through the wiring. Produce the modified `Application.module()` (or equivalent) and the Gradle dependency change as a single response, labeled.
