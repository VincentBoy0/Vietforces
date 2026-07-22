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
 * Routes all chat completion requests through the Supabase Edge Function
 * openai-proxy. The OpenAI key lives only in Supabase secrets — not in the APK.
 *
 *  - [completeJson] asks the model for a single JSON object (used for grading,
 *    mascot reactions and the learning path).
 *  - [completeChat] is a free-form multi-turn variant used by the roleplay
 *    tutor, returning the assistant's plain-text reply.
 *
 * Authorization uses the Supabase anon key. The proxy attaches the real
 * OPENAI_API_KEY server-side before forwarding to api.openai.com.
 */
object OpenAiClient {

    private val ENDPOINT: String
        get() = "${BuildConfig.SUPABASE_URL}/functions/v1/openai-proxy"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val anonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    private val model: String get() = BuildConfig.OPENAI_MODEL

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    /** True when the Supabase URL is configured (proxy is reachable). */
    fun isConfigured(): Boolean = BuildConfig.SUPABASE_URL.isNotBlank()

    /** Thrown when the Supabase URL is missing so callers can show a config hint. */
    class NotConfiguredException : IOException("Supabase URL chưa được cấu hình — kiểm tra local.properties")

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
            .addHeader("Authorization", "Bearer $anonKey")
            .addHeader("apikey", anonKey)
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
