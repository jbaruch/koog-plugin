# Remember Which Excuses We've Already Sent

## Problem/Feature Description

A developer is building a Koog 1.3 agent that drafts polite decline messages on the user's behalf. Each time the agent declines an invitation, they want the agent to remember which "excuse flavor" was used (e.g., `EMERGENCY_MEETING`, `FAMILY_OBLIGATION`, `JET_LAG`) and which organizer received it — so on the next decline, the agent can pick a flavor it hasn't used recently for that organizer.

They've been thinking about this as "the agent needs to remember its past decisions across runs". They have a small dataset (a few dozen prior declines, growing slowly), and they'd like it queryable by organizer and by date so the agent can ask "what did I send to Roberto Cortez in the last 90 days?"

They asked which Koog feature fits this.

## Output Specification

Walk through which primitive applies and produce the modified agent construction plus the Gradle dependency change.
