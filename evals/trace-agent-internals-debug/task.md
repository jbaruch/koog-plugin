# Find Out Why an Agent Is Stuck in a Loop

## Problem/Feature Description

A developer has a Koog 1.3 agent with a custom graph strategy. In production-like testing, the agent occasionally loops forever — it bounces between two specific nodes without ever exiting on the text-reply branch. Their existing OpenTelemetry traces show the calls happening but don't reveal *why* the edge predicate picks the loop branch.

They want a more detailed trace — node entries, edge predicate evaluations, payload data — so they can reproduce the bug and see exactly which predicate is misfiring. They explicitly want detailed local logs they can read line by line, not production dashboards.

## Output Specification

Walk through what to install and how to capture the trace locally. Produce the modified agent construction and Gradle changes as a single response, labeled.
