# Connect a Koog Agent to an Older MCP Server

## Problem/Feature Description

A developer has a Koog 1.3 agent in Kotlin running on the JVM. They want it to talk to an internal MCP server their team set up for querying customer records. The server operator has told them the server is from an older deployment and only supports the event-stream wire protocol over HTTP, not the newer streaming protocol. The server is at `https://mcp.internal.example/sse`.

Their agent main file already uses `runBlocking`. They don't have MCP wired up at all yet — nothing in `build.gradle.kts`, nothing in the agent construction.

## Output Specification

Walk through how to connect the agent to this server. Produce the modified source files and the Gradle dependency change in a single response.
