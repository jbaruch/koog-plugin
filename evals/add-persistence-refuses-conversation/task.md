# Make a Chatbot Remember Conversations Per User

## Problem/Feature Description

A developer is building a customer-support chatbot with a Koog 1.3 agent. They've noticed that every time a user reloads the page, the agent starts fresh — there's no memory of what was just discussed. Users have to re-explain their context every time.

The developer wants conversations to be remembered per user account. When a user signs in tomorrow, the agent should pick up where the conversation left off. They have a Postgres database available and the user's identifier is `userId`.

They ask: "How do I add persistence to my agent so it can restore where it was?"

## Output Specification

Walk through what to recommend. Produce the response as a single message — including any code you'd write or any clarification you'd offer the developer.
