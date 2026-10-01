# Expose an Account Lookup to the Agent

## Problem/Feature Description

A developer has a Koog 1.3 agent that helps customer-support staff look up account information. They want the agent to call into their existing account-lookup logic — a `suspend fun queryAccount(req: AccountLookupRequest): AccountLookupResult` that lives in their project today and is also consumed by other (non-agent) code paths.

The function's signature is fixed by those other consumers:

```kotlin
@Serializable
data class AccountLookupRequest(
    val accountId: String,
    val region: String,
    val includeArchived: Boolean,
)

@Serializable
data class AccountLookupResult(
    val displayName: String,
    val tier: String,
    val openTickets: Int,
    val lastSeenIso: String,
)

suspend fun queryAccount(req: AccountLookupRequest): AccountLookupResult
```

Make `queryAccount` callable by the agent.

## Output Specification

Walk through how to expose this query to the agent. Produce the modified or new source files in a single response, with each file labeled.
