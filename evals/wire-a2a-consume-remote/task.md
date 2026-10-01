# Delegate Heavy Analysis to a Remote Agent

## Problem/Feature Description

A developer has a Koog 1.3 agent that handles most user questions locally with a small model. For a small subset of questions that require a much larger model (deep code analysis), a separate team runs a "specialist" agent on their own infrastructure. The specialist exposes an A2A endpoint at `https://analyst.team.example/a2a`; auth is via a bearer token in `ANALYST_API_TOKEN`.

The developer wants the local agent's LLM to decide when a question is heavy enough to deserve the remote specialist, and delegate by calling the remote as if it were a tool.

## Output Specification

Walk through how to wire the remote A2A endpoint as a callable surface. Produce the relevant code and Gradle changes as a single response, labeled.
