# Compress History Between Phases of a Long Run

## Problem/Feature Description

A developer has a Koog 1.3 agent that runs through several phases — exploration, drafting, review — each involving many LLM round-trips. By the end of the exploration phase the context is bloated with intermediate tool results that aren't useful for the drafting phase, and they're running into context-window limits during drafting.

They want to summarize the exploration phase down to a TL;DR before drafting begins. They've already located the boundary in their strategy — it's a specific node where exploration ends and drafting starts.

## Output Specification

Walk through how to compress the history at that boundary. Produce the relevant node body modification as a snippet, labeled.
