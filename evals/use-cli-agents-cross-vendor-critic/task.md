# Review Step That Does Not Use the Same Model

## Problem/Feature Description

A Koog 1.2 pipeline drafts outbound messages with a Gemini model and then has a second
step decide whether each draft is safe to send. The team has noticed the reviewer is
too generous: it is the same model family that wrote the draft, and it approves almost
everything.

They want the review step to run on **Claude Code**, which their engineers already pay
for individually and are logged into locally. Finance has refused to open an Anthropic
API account, so there is no Anthropic API key available and there will not be one.

The review step must return a typed verdict the graph can branch on:

```kotlin
@Serializable
data class Critique(val approved: Boolean, val feedback: String)
```

They have been burned before by a review step that silently degraded, so they are
explicit: if the reviewer fails for any reason, nothing should be approved.

## Output Specification

Produce the modified source files in a single response, with each file labeled.
