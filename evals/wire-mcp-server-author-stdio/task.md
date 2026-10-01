# Expose a Calendar ToolSet to Other Agents over MCP

## Problem/Feature Description

A developer has an existing `CalendarTools` class in their Koog 1.3 project — a `ToolSet` with `@Tool`-annotated functions like `getCalendar()` and `getEventAttendees(eventId)`. They've been using it inside one of their own agents.

They now want other agents (built by their team or by external consumers) to call the same functions without copy-pasting the Kotlin source. They asked for the standard way to publish those tools as a service that any MCP-compliant client can invoke.

## Output Specification

Walk through what to install and produce the entry-point file plus the Gradle dependency change.
