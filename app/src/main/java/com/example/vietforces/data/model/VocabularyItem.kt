package com.example.vietforces.data.model

import androidx.annotation.DrawableRes

/**
 * Represents a vocabulary item in the learning system.
 * This is the core data model used across all game modes.
 *
 * @param id Unique identifier for the vocabulary item
 * @param word The Vietnamese word (e.g., "mèo", "bàn")
 * @param classifier Optional classifier word (e.g., "con", "cái", "chiếc")
 * @param imageResId Resource ID for the image representing this word
 * @param distractors List of wrong answers for multiple choice modes
 * @param pronunciation Optional IPA or simplified pronunciation guide
 * @param difficulty Difficulty level from 1 (easiest) to 5 (hardest)
 * @param category Category for grouping (e.g., "animals", "furniture")
 * @param isLearned Whether the user has learned this word
 * @param lastPracticed Timestamp of last practice (for spaced repetition)
 */
data class VocabularyItem(
    val id: String,
    val word: String,
    val classifier: String? = null,
    @DrawableRes val imageResId: Int,
    val distractors: List<String> = emptyList(),
    val pronunciation: String? = null,
    val difficulty: Int = 1,
    val category: String = "general",
    val isLearned: Boolean = false,
    val lastPracticed: Long = 0L
) {
    /**
     * Returns the full word with classifier if available
     * e.g., "con mèo", "cái bàn"
     */
    val fullWord: String
        get() = if (classifier != null) "$classifier $word" else word

    /**
     * Returns syllables of the word for syllable matching game
     */
    val syllables: List<String>
        get() = if (classifier != null) listOf(classifier, word) else listOf(word)
}

/**
 * Represents a sentence for sentence-based exercises
 */
data class SentenceItem(
    val id: String,
    val fullSentence: String,
    val words: List<String>,
    val blankWordIndex: Int, // For fill-in-the-blank mode
    val translation: String? = null,
    val difficulty: Int = 1,
    val category: String = "general"
) {
    /**
     * Returns the sentence with a blank at the specified position
     */
    val sentenceWithBlank: String
        get() = words.mapIndexed { index, word ->
            if (index == blankWordIndex) "______" else word
        }.joinToString(" ")

    /**
     * Returns the word that should fill the blank
     */
    val blankWord: String
        get() = words.getOrElse(blankWordIndex) { "" }
}

/**
 * User progress data
 */
data class UserProgress(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val eloRating: Int = 1000,
    val totalWordsLearned: Int = 0,
    val totalExercisesCompleted: Int = 0,
    val dailyPracticeHistory: Map<String, Int> = emptyMap() // date -> count
)

/**
 * Notification/Achievement data
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

