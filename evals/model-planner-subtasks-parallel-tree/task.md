# Model a Research Workflow as Parallel Subtasks

## Problem/Feature Description

A developer is building a research-assistant agent. For each question, it should:

1. Decompose the question into 2–4 angles to research in parallel (e.g., "technical history", "current market state", "expert opinions")
2. Run each angle as a parallel subtask, each ending with a structured summary
3. Sequentially compose all summaries into a final answer

They want the workflow expressed in a Koog 1.3 planner agent. They specifically want the angles to run in parallel (not serially), and they want to know which subtask is currently executing while a run is in flight, for debugging.

## Output Specification

Walk through how to model this. Produce the relevant code as a single Kotlin response, labeled.
