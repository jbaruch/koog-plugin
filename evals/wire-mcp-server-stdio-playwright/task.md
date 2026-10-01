# Connect an Agent to a Locally Launched Playwright MCP Server

## Problem/Feature Description

A developer is building a Koog 1.3 agent that needs to drive a real browser — open pages, click, fill forms, take screenshots. They want to use the Playwright MCP server. They explicitly say they don't want to host the server separately: they want it launched as a local process from their app, communicating over the process's stdio streams (the way most npx-launched MCP servers work).

The command they want to launch is `npx -y @playwright/mcp@latest`. The agent's main file already uses `runBlocking`.

## Output Specification

Walk through how to add the Playwright MCP tools to the agent. Produce the modified source and Gradle dependency change in a single response.
