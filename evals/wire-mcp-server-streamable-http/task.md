# Connect an Agent to the GitHub MCP Server

## Problem/Feature Description

A developer has a Koog 1.3 agent in Kotlin. They want it to be able to interact with GitHub — read issues, search PRs, comment, etc. — by connecting to a hosted GitHub MCP server. They have the server's URL (`https://mcp.github.example/v1`) and a personal access token they want passed as a bearer token. The server speaks the current MCP wire protocol; the developer mentions that the operator told them it supports the modern transport, not the older event-stream one.

Their agent currently has no tools registered at all (they removed earlier scaffolding tools). The agent is constructed in a `main()` that already uses `runBlocking`.

## Output Specification

Walk through how to add the GitHub MCP server's tools to the agent. Produce the modified source files and the Gradle dependency change in a single response.
