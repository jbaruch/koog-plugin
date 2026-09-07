---
name: use-attachments
description: >
  Send non-text content (images, files, audio) to the LLM as message attachments in
  a Koog 1.2 agent — provider-aware encoding and the `attachments` block in the prompt
  DSL. Use when the user asks to "send an image to the LLM", "use multimodal input",
  "attach a file", "pass a PDF", or describes input the LLM should process that isn't
  plain text.
---

# Use Attachments Skill

Process steps in order. Do not skip ahead.

## Step 1 — Confirm the Model Supports Multimodal Input

Not all models accept attachments. Quick guide:

- **OpenAI**: `GPT4o` and later support images. Audio/PDF support varies by exact model
- **Anthropic**: `Opus_4_*`, `Sonnet_4_*` accept images and PDFs
- **Google**: `Gemini_2.5_*` accept images, video, audio
- **Ollama / local models**: depends on the specific model (e.g., LLaVA, Llama 3.2 Vision)

If the user's chosen model doesn't support the attachment type they want, redirect to a supporting model in the same provider before continuing.

Proceed immediately to Step 2.

## Step 2 — Attach Content in the Prompt DSL

The 1.0 prompt DSL exposes an `attachments` block on user turns:

```kotlin
import ai.koog.prompt.dsl.prompt
import java.io.File

val visionPrompt = prompt("describe-screenshot") {
    user(
        text = "What's wrong with this UI?",
        attachments = listOf(
            Attachment.image(File("/path/to/screenshot.png")),
        ),
    )
}
```

Attachment factories cover the common cases:

- `Attachment.image(file)` / `Attachment.image(url)` / `Attachment.image(bytes, mimeType)`
- `Attachment.file(file)` — for PDF/document support on providers that accept them
- `Attachment.audio(file)` — for audio-capable models

Koog handles provider-specific encoding (base64 inlining vs URL reference vs uploaded-blob references) — you pass the file/bytes/URL, the executor adapts to the provider's wire shape.

Proceed immediately to Step 3.

## Step 3 — Use Attachments Inside a Strategy

When attachments come from runtime input (uploads from a Ktor endpoint, file paths from CLI args), build them inside a node body and append to the prompt via `llm.writeSession`:

```kotlin
val strategy = strategy<File, String>("describe-image") {
    val describe by node<File, Message.User>("build-message") { imageFile ->
        Message.User(
            content = "Describe this image in detail.",
            attachments = listOf(Attachment.image(imageFile)),
        )
    }

    val ask by nodeLLMSendMessage()

    edge(nodeStart forwardTo describe)
    edge(describe forwardTo ask)
    edge(ask forwardTo nodeFinish onTextMessage { true })
}
```

For large attachments, prefer URL-based references over inline bytes — base64 inlining inflates request size and counts against token budgets (see `add-token-budgeting`).

Reference example: `examples/simple-examples/.../attachments/` in the repo.

Finish here.
