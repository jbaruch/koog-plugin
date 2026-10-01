# Add a Weather Lookup Function to an Existing Agent

## Problem/Feature Description

A developer has a working Koog 1.3 agent in Kotlin. They want the agent to be able to look up the current weather for a city. They already have a function in their project that does the network call:

```kotlin
fun fetchWeather(city: String, units: String): String {
    // Existing implementation returns a human-readable string like
    // "Lisbon: 22°C, partly cloudy"
}
```

They want this function exposed to the LLM so the agent can call it when a user asks about weather. They mention they'd like it to be straightforward — "nothing fancy, just expose the function."

## Output Specification

Walk through how to expose the function to the agent. Produce the modified source files in a single response, with each file labeled.
