# Send a User-Uploaded Screenshot to the Agent

## Problem/Feature Description

A developer is building a Koog 1.3 agent that helps developers debug UI bugs. The agent's input is a screenshot the user uploads plus a short text question ("what's wrong with this UI?"). The agent runs against Anthropic Opus, which accepts images.

In their app, the input arrives as a `java.io.File` reference to the uploaded screenshot. They want to wire it through a custom strategy that takes the file as input, builds a user message with the image attached, and asks the LLM to describe the problem.

## Output Specification

Walk through the strategy. Produce the strategy and the node body that builds the message as a single Kotlin snippet, labeled.
