# See What the Agent Is Doing During a Demo

## Problem/Feature Description

A developer is preparing a conference demo of a Koog 1.3 agent. The agent calls several tools during a run. The developer wants the audience to follow along — they want a lightweight stdout surface that shows when each tool begins AND when it completes, so the audience can track the agent's progress in real time on a projector at the back of a room.

They explicitly do NOT want production telemetry — no Langfuse, no OTel collector, no metric dashboards. They just want the lightest-weight thing Koog offers that surfaces per-step activity to the developer's terminal during the demo.

The agent is otherwise unchanged from a basic `AIAgent(...)` call.

## Output Specification

Walk through what to add. Produce the modified agent construction and Gradle dependency change as a single response, labeled.
