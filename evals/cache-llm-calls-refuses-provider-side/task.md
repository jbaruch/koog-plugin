# Cut Costs on a Long System Prompt That's Called Hundreds of Times a Day

## Problem/Feature Description

A developer runs a Koog 1.3 agent on Anthropic Opus. The agent has a multi-thousand-word system prompt with detailed instructions and examples — it never changes between calls. The agent is invoked frequently from production traffic, and the developer is paying full token rates for the same system content on every call. Each call's user input is short and unique.

They've heard "caching can help" and ask: "Can we cache the LLM responses so we don't pay for repeated calls?" — they want the response cost reduced.

The conversation prompts and user inputs are NOT repeated — the responses must remain fully LLM-generated per call. The repetition is purely in the stable system prompt content.

## Output Specification

Walk through what to recommend. Produce the response as a single message — including any code you'd write or any clarification you'd offer the developer.
