# Let Non-Developers Add Capabilities Without a Release

## Problem/Feature Description

A team runs a Koog 1.2 agent that rewrites outbound customer messages. Their support
leads keep asking for new rewriting behaviours — a refund-apology tone, a
security-incident tone, a churn-risk tone — and today each one is a code change and a
deploy.

They want the leads to be able to drop a file into a directory and have the agent pick
the new behaviour up on the next run, with no recompile and no release. They also want
to be able to see, from the run output, which behaviour the agent actually used on a
given message — right now they cannot tell.

The directory will live in the repository and be edited via pull request.

## Output Specification

Produce the modified source files in a single response, with each file labeled, plus
one example of the file format the support leads would author.
