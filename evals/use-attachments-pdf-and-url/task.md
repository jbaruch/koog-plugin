# Contract and Product Image Analyzer

## Problem/Feature Description

A legal-tech startup is building a Koog 1.3 agent that helps procurement teams review vendor contracts alongside product specifications. Each review request pairs a PDF contract (stored on the local filesystem) with a publicly hosted product image URL. The agent must read both artifacts in the same LLM call and return a summary of any concerns found in the contract and whether the product image matches the described item.

The engineering team has chosen to use the Anthropic provider for this agent. The PDF files can be large (tens of megabytes), and the product images are served from a CDN. The team wants both artifacts read in a single LLM round trip rather than two sequential calls, so the model can correlate the contract terms with the image directly.

## Output Specification

Produce a single Kotlin file (`contract_analyzer.kt`) containing:
- The code that accepts a PDF path and image URL as inputs and returns the summary
- A `main` function (or usage comment) that shows how to run the agent with a sample PDF path and image URL

The output file should compile cleanly (no missing imports, valid Kotlin syntax).
