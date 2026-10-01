# Expose a Koog Agent to an Orchestration Dashboard

## Problem/Feature Description

A team is building an internal orchestration dashboard that manages a fleet of Koog 1.3 agents. The dashboard needs to drive individual agents with full lifecycle control: it must be able to start agent runs, receive streaming progress updates as the agent works through its strategy, and cancel runs that are taking too long or are no longer needed. The dashboard itself is the client — it is tooling written by the team, not another agent doing its own planning.

The team has an existing Koog 1.3 agent defined with `AIAgent(...)` that currently only runs locally. They want to expose it so the dashboard can connect, invoke it, subscribe to progress events during the run, and issue cancellation requests at any point during execution. The dashboard team will handle the client side; your job is to wire the agent so it speaks the right protocol on the server side.

## Output Specification

Produce a Kotlin code snippet (or file `agent_setup.kt`) showing the updated agent definition wired for the dashboard. Include the required build dependency in a `build.gradle.kts` snippet. Explain in a short `notes.md` which protocol was chosen and why it fits the dashboard's requirements better than the alternative inter-agent or tool-host protocols in the ecosystem.
