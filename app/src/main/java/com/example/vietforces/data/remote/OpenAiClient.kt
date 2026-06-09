package com.example.vietforces.data.remote

import com.example.vietforces.BuildConfig
import com.example.vietforces.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Low-level client for the OpenAI Chat Completions API (gpt-4o-mini).
 *
 *  - [completeJson] asks the model for a single JSON object (used for grading,
 *    mascot reactions and the learning path).
 *  - [completeChat] is a free-form multi-turn variant used by the roleplay
 *    tutor, returning the assistant's plain-text reply.
 *
 * The API key is injected at build time from local.properties via BuildConfig.
 * NOTE: embedding the key is fine for a demo/coursework build, but such an APK
 * must never be published. For production, move calls behind a server proxy.
 */
object OpenAiClient {

    private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val apiKey: String get() = BuildConfig.OPENAI_API_KEY
    private val model: String get() = BuildConfig.OPENAI_MODEL

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /** True when an API key was provided at build time. */
    fun isConfigured(): Boolean = apiKey.isNotBlank()

    /** Thrown when the key is missing so callers can show a config hint. */
    class NotConfiguredException : IOException("OpenAI API key chưa được cấu hình")

    /**
     * Send a system + user prompt and return the assistant content as a JSON
     * object string (forced via `response_format = json_object`).
     */
    suspend fun completeJson(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double = 0.4,
        maxTokens: Int = 900
    ): String {
        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
                put(JSONObject().put("role", "user").put("content", userPrompt))
            })
        }
        return send(payload)
    }

    /**
     * Multi-turn chat that forces a single JSON object reply (via
     * `response_format = json_object`). [messages] is the full conversation
     * (system + alternating user/assistant turns). Used by the roleplay tutor so
     * one call can return the NPC line plus corrections, suggestions and goals.
     */
    suspend fun completeJsonChat(
        messages: List<ChatMessage>,
        temperature: Double = 0.6,
        maxTokens: Int = 800
    ): String {
        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("response_format", JSONObject().put("type", "json_object"))
            put("messages", JSONArray().apply {
                messages.forEach { m ->
                    put(JSONObject().put("role", m.role).put("content", m.content))
                }
            })
        }
        return send(payload)
    }

    /**
     * Multi-turn chat. [messages] is the full conversation (system + alternating
     * user/assistant turns). Returns the assistant's free-form text reply.
     */
    suspend fun completeChat(
        messages: List<ChatMessage>,
        temperature: Double = 0.8,
        maxTokens: Int = 300
    ): String {
        val payload = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("messages", JSONArray().apply {
                messages.forEach { m ->
                    put(JSONObject().put("role", m.role).put("content", m.content))
                }
            })
        }
        return send(payload)
    }

    /** Execute one request on the IO dispatcher and return the message content. */
    private suspend fun send(payload: JSONObject): String = withContext(Dispatchers.IO) {
        if (!isConfigured()) throw NotConfiguredException()

        val request = Request.Builder()
            .url(ENDPOINT)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(JSON_MEDIA))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiMessage = runCatching {
                    JSONObject(body).getJSONObject("error").getString("message")
                }.getOrNull()
                throw IOException("OpenAI API ${response.code}: ${apiMessage ?: "lỗi không xác định"}")
            }
            return@withContext extractContent(body)
        }
    }

    /** Pull `choices[0].message.content` out of the API response. */
    private fun extractContent(responseBody: String): String {
        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
            ?: throw IOException("Phản hồi AI không hợp lệ")
        if (choices.length() == 0) throw IOException("AI không trả về nội dung")
        val content = choices.getJSONObject(0)
            .optJSONObject("message")
            ?.optString("content")
            .orEmpty()
        if (content.isBlank()) throw IOException("AI trả về nội dung rỗng")
        return content.trim()
    }
}
