# Cache a Long System Prompt for Anthropic Calls

## Problem/Feature Description

A developer runs a Koog 1.3 agent against Anthropic Opus. The agent has a multi-thousand-word system prompt full of domain instructions, style guides, and examples. It's invoked frequently (~10× per minute) and they're seeing high token bills — most of which is the system prompt being re-billed on every call.

They want to mark the system prompt as cacheable so subsequent calls within Anthropic's TTL hit the cache and pay the reduced rate. They asked specifically about how to set the cache breakpoint.

## Output Specification

Walk through how to mark the prompt as cacheable. Produce the prompt definition as a single Kotlin snippet, labeled.
