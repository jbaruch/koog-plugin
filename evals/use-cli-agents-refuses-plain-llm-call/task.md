# Summarise Tickets on the Request Path

## Problem/Feature Description

A Koog 1.2 service summarises a support ticket on every inbound HTTP request. It
currently calls Anthropic through a `PromptExecutor` with an API key, and the team is
watching the API bill go up.

One of the engineers noticed that Koog can call the Claude Code CLI directly, and
points out that everyone on the team already has a Claude subscription. He wants to
swap the summarisation call to `CliAIAgent.claude(...)` so the summaries come out of
the subscriptions instead of the API account.

The endpoint currently serves this call in roughly two seconds and is on the
synchronous request path for the support console.

## Output Specification

Advise the engineer and produce whatever code change you recommend, with each file
labeled.
