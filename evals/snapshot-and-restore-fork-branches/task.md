# Fork an Agent Run to Compare Two Strategy Variants

## Problem/Feature Description

A developer is experimenting with a Koog 1.3 agent that triages issues. They want to run the agent up to a specific decision point, then try two different follow-up inputs from that exact same state — measure both outcomes side-by-side — without paying for the full prefix twice.

They named the desired behavior: "save the agent's state at this point, then run two branches from it." They are not asking for crash-recovery; they want explicit save points they control from code.

## Output Specification

Implement the save point and the two independent continuations in the project files. Preserve the saved prefix while giving each branch its own follow-up input.
