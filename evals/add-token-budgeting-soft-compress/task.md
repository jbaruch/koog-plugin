# Cap an Agent's Token Spend with Graceful Degradation

## Problem/Feature Description

A developer runs a Koog 1.3 agent that processes user-submitted tasks. They've had incidents where a single complex task consumed tens of thousands of tokens and produced a large bill. They want to cap each run at 50,000 tokens.

They explicitly say: "Don't just abort when the cap is hit — try to continue by summarizing the history first, and only fail if summarization can't reclaim enough budget." The agent runs against OpenAI's GPT-4o.

## Output Specification

Walk through what to install and configure. Produce the modified agent construction and Gradle changes as a single response, labeled.
