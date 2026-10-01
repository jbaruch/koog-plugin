# Send Agent Traces to Langfuse

## Problem/Feature Description

A developer is running a Koog 1.3 agent in production and wants traces and token-usage metrics to land in Langfuse. They have a Langfuse project set up; the keys and session ID are already in their environment (`LANGFUSE_PUBLIC_KEY`, `LANGFUSE_SECRET_KEY`, `LANGFUSE_SESSION_ID`). Their agent is currently a plain `AIAgent(...)` call with no features installed.

They want verbose mode on so they can see the trace flow during initial rollout.

## Output Specification

Walk through how to add Langfuse-backed observability. Produce the modified agent construction and the Gradle dependency change as a single response, labeled.
