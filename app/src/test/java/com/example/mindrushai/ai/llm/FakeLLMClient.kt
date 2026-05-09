package com.example.mindrushai.ai.llm

/**
 * Test double for [LLMClient]. Returns scripted responses without touching the network,
 * so unit tests stay deterministic and run in milliseconds.
 *
 * Use [responder] to drive behavior per prompt:
 *  - Return a string → that string is the model output.
 *  - Throw → simulate a network/parse error.
 *  - Use the constructor [delayMs] to simulate slow responses (will hit the AIManager timeout).
 */
class FakeLLMClient(
    private val delayMs: Long = 0,
    private val responder: (String) -> String
) : LLMClient {

    var callCount: Int = 0
        private set
    val prompts: MutableList<String> = mutableListOf()

    override suspend fun generate(prompt: String): String {
        callCount++
        prompts.add(prompt)
        if (delayMs > 0) {
            kotlinx.coroutines.delay(delayMs)
        }
        return responder(prompt)
    }
}
