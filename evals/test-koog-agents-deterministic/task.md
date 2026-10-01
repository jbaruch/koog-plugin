# Write a Deterministic Unit Test for a Koog Agent

## Problem/Feature Description

A developer wants to test a Koog 1.3 agent that classifies support tickets. The agent has a `lookup_priority` tool that consults an internal API. The test should:

1. Verify that for a "server is down" input, the agent calls `lookup_priority` first, then replies with a classification
2. Not hit any real LLM or real internal API
3. Run quickly and produce the same result on every run

The developer asks how to structure the test. They're using JUnit 5.

## Output Specification

Walk through the test structure. Produce a complete Kotlin test class as a single response, labeled.
