# Make a Koog Agent Cancellable from an IDE Plugin

## Problem/Feature Description

A developer is building an IDE plugin that drives a Koog 1.3 agent. The plugin's user can cancel a long-running agent run from the IDE (user clicks "Stop"). The developer wants the agent to honor that cancellation promptly — including interrupting whatever tool the agent is currently running.

The agent has several tools, including a long-running `analyzeRepository` tool that walks the codebase (can take 30+ seconds).

## Output Specification

Walk through what to install, what protocol to use, and what the `analyzeRepository` tool needs to do to honor cancellation. Produce the relevant code as a single response, labeled.
