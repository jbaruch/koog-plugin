# Make a Long-Running Agent Survive Server Restarts

## Problem/Feature Description

A developer runs a Koog 1.3 agent for batch-processing tasks that can take 30 minutes per run. The agent's host server is occasionally restarted (planned maintenance, deploys, crashes). When this happens mid-run, the agent loses everything and has to start over — which means re-doing 30 minutes of LLM and tool work.

The developer wants the agent to survive these interruptions: when the host process comes back up, the agent should resume from approximately where it stopped, not from scratch. The save points should be automatic — the developer does not want to sprinkle save-state calls through their code.

They've heard about Koog's snapshot feature and ask: "Can I add snapshots so the agent recovers automatically after the server restarts?"

## Output Specification

Walk through what to recommend. Produce the response as a single message — including any code you'd write or any clarification you'd offer the developer.
