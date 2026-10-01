# Make a Long-Running Planner Agent Survive Restarts

## Problem/Feature Description

A developer is running a Koog 1.3 planner agent that processes large batches of work — each run takes 20 minutes and chains many LLM calls. The deployment occasionally restarts (rolling deployments, OOM-kills, host evictions). When that happens the agent loses its progress and has to start from scratch.

They want the agent to write durable checkpoints continuously, so a restart can resume from the last checkpoint rather than starting over. They've got a JDBC-accessible database they can use for the backing store.

## Output Specification

Walk through what to add. Produce the modified agent construction and dependency change as a single response, labeled.
