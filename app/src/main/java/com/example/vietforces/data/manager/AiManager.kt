package com.example.vietforces.data.manager

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.vietforces.data.model.AiCallResult
import com.example.vietforces.data.model.AiGradingResult
import com.example.vietforces.data.model.AiMistake
import com.example.vietforces.data.model.ChatMessage
import com.example.vietforces.data.model.RoleplayScenario
import com.example.vietforces.data.model.RoleplayTurn
import com.example.vietforces.data.model.LearningPathItem
import com.example.vietforces.data.model.LearningPlan
import com.example.vietforces.data.model.LearningWeakness
import com.example.vietforces.data.model.MascotFeedback
import com.example.vietforces.data.model.WritingFeedback
import com.example.vietforces.data.remote.OpenAiClient
import com.example.vietforces.data.storage.PreferencesManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-level entry point for all AI features (§6, §9, §12).
 *
 * Responsibilities:
 *  - Hold the user's AI on/off toggles (persisted).
 *  - Build prompts, call [OpenAiClient], and map the JSON reply into typed models.
 *  - Never throw to the UI: every call returns an [AiCallResult] with a friendly
 *    fallback message when AI is off, unconfigured, or the network fails.
 */
object AiManager {

    /** Master toggle for AI grading / feedback (§7.8, §12). */
    var aiFeedbackEnabled by mutableStateOf(true)
        private set

    /** Toggle for AI-generated mascot reactions (§5, §7.8). */
    var aiMascotEnabled by mutableStateOf(true)
        private set

    fun loadFromPreferences() {
        try {
            aiFeedbackEnabled = PreferencesManager.getAiFeedbackEnabled()
            aiMascotEnabled = PreferencesManager.getAiMascotEnabled()
        } catch (e: Exception) {
            // Prefs not ready yet — keep defaults.
        }
    }

    fun updateAiFeedback(enabled: Boolean) {
        aiFeedbackEnabled = enabled
        runCatching { PreferencesManager.saveAiFeedbackEnabled(enabled) }
    }

    fun updateAiMascot(enabled: Boolean) {
        aiMascotEnabled = enabled
        runCatching { PreferencesManager.saveAiMascotEnabled(enabled) }
    }

    /** True when AI can actually be used (toggle on + key present). */
    fun isAvailable(): Boolean = aiFeedbackEnabled && OpenAiClient.isConfigured()

    // ==================== §6.2 — Grade a short paragraph ====================

    suspend fun gradeWriting(topicTitle: String, text: String): AiCallResult<WritingFeedback> {
        if (!aiFeedbackEnabled) {
            return AiCallResult.Error("Tính năng AI đang tắt. Bật trong Cài đặt để chấm bài.", isConfigError = true)
        }
        val system = """
            Bạn là giáo viên tiếng Việt thân thiện, chấm đoạn văn ngắn của người nước ngoài đang học tiếng Việt.
            Hãy đánh giá theo: ngữ pháp, dấu thanh, dấu câu, từ vựng, tính tự nhiên, đúng chủ đề, mạch lạc.
            Giọng điệu nhẹ nhàng, động viên, KHÔNG chê bai.
            Chỉ trả về DUY NHẤT một JSON đúng cấu trúc sau, không thêm chữ nào ngoài JSON:
            {
              "score": <số nguyên 0-10>,
              "overallFeedback": "<nhận xét chung ngắn gọn bằng tiếng Việt>",
              "mistakes": [
                {"type":"<missing_tone|punctuation|spelling|word_order|word_choice|missing_subject|other>",
                 "text":"<đoạn sai>", "suggestion":"<sửa lại>", "explanation":"<giải thích ngắn>"}
              ],
              "correctedVersion": "<bản viết lại đã sửa>",
              "weaknessTags": ["<tag điểm yếu>"]
            }
        """.trimIndent()
        val user = "Chủ đề: \"$topicTitle\"\nBài viết của người học:\n\"\"\"\n$text\n\"\"\""
        return call(system, user) { parseWritingFeedback(it) }
    }

    // ==================== §6.1 / §6.3 — Grade an open / near answer ====================

    suspend fun gradeOpenAnswer(
        question: String,
        expectedAnswer: String,
        userAnswer: String
    ): AiCallResult<AiGradingResult> {
        if (!aiFeedbackEnabled) {
            return AiCallResult.Error("Tính năng AI đang tắt.", isConfigError = true)
        }
        val system = """
            Bạn chấm câu trả lời tiếng Việt của người mới học. Câu trả lời của người dùng có thể khác
            đáp án mẫu nhưng vẫn đúng ý (thiếu dấu, thiếu từ phụ, diễn đạt khác). Hãy đánh giá độ tương hợp
            về Ý NGHĨA, không chỉ so khớp chuỗi. Giọng động viên, không chê bai.
            Chỉ trả về DUY NHẤT JSON sau:
            {
              "score": <0-10>,
              "isAcceptable": <true|false>,
              "feedback": "<nhận xét ngắn>",
              "mistakes": [{"type":"<...>","text":"<...>","suggestion":"<...>","explanation":"<...>"}],
              "suggestedAnswer": "<đáp án gợi ý hoặc null>",
              "weaknessTags": ["<tag>"]
            }
        """.trimIndent()
        val user = "Câu hỏi: \"$question\"\nĐáp án mẫu: \"$expectedAnswer\"\nNgười dùng trả lời: \"$userAnswer\""
        return call(system, user) { parseGrading(it) }
    }

    // ==================== §5 — Mascot reaction ====================

    private val mascotStyles = listOf(
        "hài hước, dí dỏm",
        "nhẹ nhàng, ân cần",
        "năng động, cổ vũ mạnh mẽ",
        "kiểu đàn anh thân thiện",
        "tinh nghịch, đáng yêu",
        "điềm đạm, truyền cảm hứng"
    )

    suspend fun mascotReact(context: String): AiCallResult<MascotFeedback> {
        if (!aiMascotEnabled) {
            return AiCallResult.Error("Mascot AI đang tắt.", isConfigError = true)
        }
        val style = mascotStyles.random()
        val system = """
            Bạn là linh vật gà trống vui vẻ đồng hành cùng người học tiếng Việt. Dựa trên ngữ cảnh học tập,
            hãy nói MỘT câu ngắn (tối đa 25 từ), tích cực, động viên, có thể nhắc nhẹ lỗi. KHÔNG chê bai.
            Phong cách lần này: $style.
            QUAN TRỌNG: mỗi lần phải nói MỘT CÁCH KHÁC NHAU, sáng tạo, đa dạng từ ngữ.
            TUYỆT ĐỐI tránh lặp lại các mẫu câu sáo mòn như "Không sao đâu", "Hãy nhớ đáp án đúng là".
            Chỉ trả về DUY NHẤT JSON:
            {"emotion":"<happy|encouraging|proud|thinking|celebrating>","message":"<câu nói tiếng Việt>","shouldEncourage":<true|false>}
        """.trimIndent()
        return call(system, context, temperature = 1.0) { parseMascot(it) }
    }

    // ==================== §6.4 — Personalised learning path ====================

    suspend fun buildLearningPlan(statsSummary: String): AiCallResult<LearningPlan> {
        if (!aiFeedbackEnabled) {
            return AiCallResult.Error("Tính năng AI đang tắt.", isConfigError = true)
        }
        val system = """
            Bạn phân tích dữ liệu học tiếng Việt của người dùng và tạo lộ trình học cá nhân.
            Dựa trên thống kê (điểm yếu, lỗi hay gặp, chế độ làm kém), đề xuất các bài luyện ưu tiên.
            recommendedMode phải thuộc: image_to_word, word_to_image, sentence_order, fill_blank,
            word_chain, word_search, syllable_match, writing.
            Chỉ trả về DUY NHẤT JSON:
            {
              "summary":"<tóm tắt ngắn>",
              "mainWeaknesses":[{"type":"<...>","description":"<...>","recommendedPractice":"<...>"}],
              "learningPath":[{"title":"<...>","description":"<...>","targetWeakness":"<...>",
                 "recommendedMode":"<...>","priority":<số 1-5>}]
            }
        """.trimIndent()
        return call(system, statsSummary) { parseLearningPlan(it) }
    }

    // ==================== Roleplay conversation tutor ====================

    private fun roleplaySystem(scenario: RoleplayScenario): String {
        val steps = if (scenario.goalSteps.isEmpty()) "(không có)"
            else scenario.goalSteps.joinToString("; ")
        return """
        Bạn vừa ĐÓNG VAI trong một tình huống, vừa âm thầm làm GIA SƯ tiếng Việt cho người nước ngoài.

        VAI CỦA BẠN:
        ${scenario.persona}
        Mục tiêu của người học: ${scenario.goal}
        Các bước cần hoàn thành: $steps

        CÁCH ĐÓNG VAI (trường "reply"):
        - "reply" BẮT BUỘC là một câu thoại có nội dung, TUYỆT ĐỐI không để trống.
        - Nói tiếng Việt TỰ NHIÊN nhưng ĐƠN GIẢN, câu NGẮN (1–2 câu), giữ đúng vai, thân thiện, kiên nhẫn.
        - Nếu người học nói LẠC ĐỀ / không liên quan tình huống: nhẹ nhàng từ chối rồi KÉO NGAY về mục tiêu.
          TUYỆT ĐỐI không tự bịa hay đoán xem họ muốn gì (ví dụ không tự hỏi "bạn muốn gọi cà phê đúng không?"
          khi họ chưa hề nói vậy).
        - KHÔNG chê bai. KHÔNG dùng tiếng Anh trong "reply" trừ khi người học hoàn toàn bí.

        SỬA LỖI (trường "corrections"):
        - Soi RIÊNG câu CUỐI CÙNG mà người học vừa gõ. "text" PHẢI trích ĐÚNG NGUYÊN VĂN từ câu đó —
          TUYỆT ĐỐI không bịa ra lỗi không có, không tự thêm/bớt dấu rồi coi là lỗi.
        - Kiểm tra: thiếu dấu thanh, sai chính tả, sai ngữ pháp / trật tự từ, dùng từ chưa tự nhiên.
          Mỗi lỗi nêu đoạn sai + cách sửa + giải thích NGẮN.
        - Nếu câu của người học thực sự VIẾT KHÔNG DẤU / THIẾU DẤU THANH thì thêm lỗi type = "missing_tone".
          Nhưng nếu câu ĐÃ CÓ ĐỦ DẤU rồi thì TUYỆT ĐỐI không báo "missing_tone".
        - Nếu người học viết bằng tiếng Anh / ngôn ngữ khác, gợi ý câu tiếng Việt tương ứng (type "word_choice").
        - Khi câu đã đúng dấu, đúng ngữ pháp và tự nhiên: "corrections" = [].
        - "type" thuộc: missing_tone | spelling | word_order | word_choice | grammar | punctuation | other

        GỢI Ý (trường "suggestions"):
        - 1–2 câu tiếng Việt ĐƠN GIẢN người học có thể nói tiếp để tiến tới mục tiêu, kèm nghĩa tiếng Anh
          trong ngoặc đơn. Luôn đưa khi họ lạc đề hoặc đang bí; nếu họ đang đi đúng hướng có thể để [].

        THEO DÕI MỤC TIÊU (trường "goalsMet"):
        - Dựa trên TOÀN BỘ hội thoại, liệt kê những bước trong "Các bước cần hoàn thành" mà người học ĐÃ làm
          được tính đến hiện tại. Dùng ĐÚNG NGUYÊN VĂN từng bước. Chưa làm được bước nào thì để [].

        Chỉ trả về DUY NHẤT một JSON đúng cấu trúc sau, không thêm chữ nào ngoài JSON:
        {
          "reply": "<câu thoại của bạn theo vai>",
          "offTopic": <true|false>,
          "corrections": [{"type":"<...>","text":"<đoạn sai>","suggestion":"<sửa lại>","explanation":"<giải thích ngắn>"}],
          "suggestions": ["<câu gợi ý (English gloss)>"],
          "goalsMet": ["<bước đã hoàn thành>"]
        }
        """.trimIndent()
    }

    /** Generate the NPC's next turn (reply + corrections + suggestions + goals). */
    suspend fun roleplayReply(
        scenario: RoleplayScenario,
        history: List<ChatMessage>
    ): AiCallResult<RoleplayTurn> {
        return try {
            val messages = listOf(ChatMessage("system", roleplaySystem(scenario))) + history
            var turn = requestRoleplayTurn(messages)
            // gpt-4o-mini occasionally returns an essentially empty object (blank
            // reply, no corrections). One retry almost always fixes it and brings
            // back the corrections too, instead of leaving a dead "..." bubble.
            if (turn.reply.isBlank()) {
                Log.w("AiManager", "roleplay blank reply — retrying once")
                turn = requestRoleplayTurn(messages)
            }
            if (turn.reply.isBlank()) {
                turn = turn.copy(reply = "Dạ, bạn nói lại giúp mình một chút nhé?")
            }
            AiCallResult.Success(turn)
        } catch (e: OpenAiClient.NotConfiguredException) {
            AiCallResult.Error("Chưa cấu hình API key.", isConfigError = true)
        } catch (e: Exception) {
            Log.e("AiManager", "roleplayReply failed", e)
            AiCallResult.Error("Không kết nối được AI lúc này. Thử lại nhé. (${e.message})")
        }
    }

    private suspend fun requestRoleplayTurn(messages: List<ChatMessage>): RoleplayTurn {
        val raw = OpenAiClient.completeJsonChat(messages, temperature = 0.6)
        return parseRoleplayTurn(JSONObject(sanitize(raw)))
    }

    private fun parseRoleplayTurn(o: JSONObject) = RoleplayTurn(
        reply = o.optString("reply", "").trim(),
        offTopic = o.optBoolean("offTopic", false),
        corrections = parseMistakes(o.optJSONArray("corrections")),
        suggestions = parseTags(o.optJSONArray("suggestions")),
        goalsMet = parseTags(o.optJSONArray("goalsMet"))
    )

    /** Suggest one simple Vietnamese line the learner could say next (with EN gloss). */
    suspend fun roleplayHint(
        scenario: RoleplayScenario,
        history: List<ChatMessage>
    ): AiCallResult<String> {
        return try {
            val convo = history.joinToString("\n") {
                val who = if (it.role == "assistant") "NPC" else "Khách"
                "$who: ${it.content}"
            }
            val system = "Bạn là trợ lý giúp người nước ngoài học tiếng Việt. " +
                "Hãy gợi ý DUY NHẤT MỘT câu tiếng Việt đơn giản mà người học có thể nói tiếp " +
                "trong tình huống, kèm nghĩa tiếng Anh trong ngoặc đơn. " +
                "Chỉ trả về đúng câu gợi ý, không giải thích thêm."
            val user = "Tình huống: ${scenario.title}. Mục tiêu: ${scenario.goal}.\n" +
                "Hội thoại đến hiện tại:\n$convo\n\nGợi ý câu tiếp theo cho Khách:"
            val msgs = listOf(ChatMessage("system", system), ChatMessage("user", user))
            AiCallResult.Success(OpenAiClient.completeChat(msgs, temperature = 0.6, maxTokens = 80))
        } catch (e: OpenAiClient.NotConfiguredException) {
            AiCallResult.Error("Chưa cấu hình API key.", isConfigError = true)
        } catch (e: Exception) {
            AiCallResult.Error("Không lấy được gợi ý. (${e.message})")
        }
    }

    // ==================== Shared call + error handling (§12) ====================

    private suspend fun <T> call(
        system: String,
        user: String,
        temperature: Double = 0.4,
        parse: (JSONObject) -> T
    ): AiCallResult<T> {
        return try {
            val raw = OpenAiClient.completeJson(system, user, temperature = temperature)
            val json = JSONObject(sanitize(raw))
            AiCallResult.Success(parse(json))
        } catch (e: OpenAiClient.NotConfiguredException) {
            AiCallResult.Error(
                "Chưa cấu hình API key. Thêm OPENAI_API_KEY vào local.properties.",
                isConfigError = true
            )
        } catch (e: Exception) {
            AiCallResult.Error("Không kết nối được AI lúc này. Vui lòng thử lại. (${e.message})")
        }
    }

    /** Strip accidental markdown fences so JSONObject can parse the reply. */
    private fun sanitize(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start >= 0 && end > start) raw.substring(start, end + 1) else raw
    }

    // ==================== JSON → model parsers ====================

    private fun parseMistakes(arr: JSONArray?): List<AiMistake> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { o ->
                AiMistake(
                    type = o.optString("type", "other"),
                    text = o.optString("text", ""),
                    suggestion = o.optString("suggestion", ""),
                    explanation = o.optString("explanation", "")
                )
            }
        }
    }

    private fun parseTags(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }.filter { it.isNotBlank() }
    }

    private fun parseWritingFeedback(o: JSONObject) = WritingFeedback(
        score = o.optInt("score", 0).coerceIn(0, 10),
        overallFeedback = o.optString("overallFeedback", ""),
        mistakes = parseMistakes(o.optJSONArray("mistakes")),
        correctedVersion = o.optString("correctedVersion", ""),
        weaknessTags = parseTags(o.optJSONArray("weaknessTags"))
    )

    private fun parseGrading(o: JSONObject) = AiGradingResult(
        score = o.optInt("score", 0).coerceIn(0, 10),
        isAcceptable = o.optBoolean("isAcceptable", false),
        feedback = o.optString("feedback", ""),
        mistakes = parseMistakes(o.optJSONArray("mistakes")),
        suggestedAnswer = o.optString("suggestedAnswer").takeIf { it.isNotBlank() && it != "null" },
        weaknessTags = parseTags(o.optJSONArray("weaknessTags"))
    )

    private fun parseMascot(o: JSONObject) = MascotFeedback(
        emotion = o.optString("emotion", "happy"),
        message = o.optString("message", ""),
        shouldEncourage = o.optBoolean("shouldEncourage", true)
    )

    private fun parseLearningPlan(o: JSONObject): LearningPlan {
        val weaknesses = o.optJSONArray("mainWeaknesses")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let {
                    LearningWeakness(
                        type = it.optString("type", "other"),
                        description = it.optString("description", ""),
                        recommendedPractice = it.optString("recommendedPractice", "")
                    )
                }
            }
        } ?: emptyList()
        val path = o.optJSONArray("learningPath")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let {
                    LearningPathItem(
                        title = it.optString("title", ""),
                        description = it.optString("description", ""),
                        targetWeakness = it.optString("targetWeakness", ""),
                        recommendedMode = it.optString("recommendedMode", ""),
                        priority = it.optInt("priority", 3),
                        isCompleted = false
                    )
                }
            }
        } ?: emptyList()
        return LearningPlan(
            summary = o.optString("summary", ""),
            mainWeaknesses = weaknesses,
            learningPath = path
        )
    }
}
