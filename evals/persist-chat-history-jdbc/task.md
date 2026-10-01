# Returning Users Lose Their Conversation

## Problem/Feature Description

A developer is building a customer-support chatbot powered by a Koog 1.3 agent. They have a bug report from users: "When I come back tomorrow, the bot doesn't remember anything we talked about — I have to re-explain my whole problem from scratch."

The agent today is a plain `AIAgent(...)` invocation per HTTP request. Each authenticated user has a stable `userId` that the route handler already has in scope. The developer has a Postgres database available; the JDBC URL and credentials are in environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`).

They asked for whichever Koog feature fits this.

## Output Specification

Walk through what to install and how to wire it into the agent's invocation pattern. Produce the modified agent construction and the route/handler code as a single response, labeled.
