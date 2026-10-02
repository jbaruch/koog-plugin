# Make a Long-Running Agent Survive Server Restarts

## Problem/Feature Description

A developer runs a Koog 1.3 agent for batch-processing tasks that can take 30 minutes per run. The agent's host server is occasionally restarted (planned maintenance, deploys, crashes). When this happens mid-run, the agent loses everything and has to start over — which means re-doing 30 minutes of LLM and tool work.

The developer wants the agent to survive these interruptions: when the host process comes back up, the agent should resume from approximately where it stopped, not from scratch. The save points should be automatic — the developer does not want to sprinkle save-state calls through their code.

They've heard about snapshots in Koog and ask: "Can I add snapshots so the agent recovers automatically after the server restarts?"

## Output Specification

Implement the recovery configuration in the project files and document how it satisfies the request for automatic recovery. Include any clarification needed about the developer's wording.
