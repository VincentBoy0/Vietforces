package com.example.vietforces.data.manager

import com.example.vietforces.data.storage.PreferencesManager
import kotlin.math.exp
import kotlin.random.Random

/**
 * Represents a game mode for tracking encountered items
 */
enum class GameMode(val key: String) {
    IMAGE_TO_WORD("image_to_word"),
    WORD_TO_IMAGE("word_to_image"),
    SENTENCE_ORDER("sentence_order"),
    FILL_BLANK("fill_blank"),
    WORD_CHAIN("word_chain"),
    WORD_SEARCH("word_search"),
    SYLLABLE_MATCH("syllable_match")
}

/**
 * Data class representing an encountered item's history
 */
data class EncounteredItem(
    val itemId: String,
    val encounterCount: Int = 0,
    val lastEncounteredTime: Long = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0
) {
    /**
     * Calculate weight for this item (lower weight = less likely to be selected)
     * Uses spaced repetition algorithm inspired by SM-2
     */
    fun calculateWeight(currentTime: Long): Double {
        if (encounterCount == 0) {
            // Never encountered - high weight (prioritize new items)
            return 1.0
        }

        // Time factor: items encountered longer ago should have higher weight
        val hoursSinceLastEncounter = (currentTime - lastEncounteredTime) / (1000.0 * 60 * 60)
        val timeFactor = 1.0 - exp(-hoursSinceLastEncounter / 24.0) // Approaches 1.0 after 24 hours

        // Encounter count factor: reduce weight for frequently seen items
        val encounterFactor = 1.0 / (1.0 + encounterCount * 0.3)

        // Performance factor: items answered incorrectly should appear more often
        val totalAnswers = correctCount + wrongCount
        val performanceFactor = if (totalAnswers > 0) {
            val errorRate = wrongCount.toDouble() / totalAnswers
            0.5 + errorRate * 0.5 // Range: 0.5 (all correct) to 1.0 (all wrong)
        } else {
            0.7 // Default for unanswered
        }

        // Combine factors with minimum weight to ensure all items can still appear
        val weight = (timeFactor * 0.4 + encounterFactor * 0.3 + performanceFactor * 0.3)
        return maxOf(0.1, weight) // Minimum 10% weight
    }
}

/**
 * Manager for tracking encountered items across different game modes.
 * Uses weighted random selection to reduce probability of recently seen items.
 */
object EncounteredItemsManager {

    // In-memory cache of encountered items per game mode
    private val encounteredItems = mutableMapOf<GameMode, MutableMap<String, EncounteredItem>>()

    private var isInitialized = false

    /**
     * Initialize manager and load data from SharedPreferences
     */
    fun loadFromPreferences() {
        if (isInitialized) return
        try {
            GameMode.entries.forEach { mode ->
                encounteredItems[mode] = PreferencesManager.loadEncounteredItems(mode.key).toMutableMap()
            }
            isInitialized = true
        } catch (e: Exception) {
            // Initialize with empty maps if prefs not available
            GameMode.entries.forEach { mode ->
                encounteredItems[mode] = mutableMapOf()
            }
        }
    }

    /**
     * Record that an item was encountered in a game mode
     */
    fun recordEncounter(gameMode: GameMode, itemId: String, wasCorrect: Boolean? = null) {
        val items = encounteredItems.getOrPut(gameMode) { mutableMapOf() }
        val existing = items[itemId] ?: EncounteredItem(itemId)

        val updated = existing.copy(
            encounterCount = existing.encounterCount + 1,
            lastEncounteredTime = System.currentTimeMillis(),
            correctCount = if (wasCorrect == true) existing.correctCount + 1 else existing.correctCount,
            wrongCount = if (wasCorrect == false) existing.wrongCount + 1 else existing.wrongCount
        )

        items[itemId] = updated

        // Save to preferences
        try {
            PreferencesManager.saveEncounteredItems(gameMode.key, items)
        } catch (e: Exception) {
            // Ignore if prefs not initialized
        }
    }

    /**
     * Get weighted random selection from a list of items.
     * Items encountered less frequently or longer ago are more likely to be selected.
     *
     * @param gameMode The game mode
     * @param allItemIds All available item IDs
     * @param count Number of items to select
     * @param excludeIds IDs to exclude from selection
     * @return List of selected item IDs
     */
    fun selectWeightedItems(
        gameMode: GameMode,
        allItemIds: List<String>,
        count: Int,
        excludeIds: Set<String> = emptySet()
    ): List<String> {
        val items = encounteredItems[gameMode] ?: mutableMapOf()
        val currentTime = System.currentTimeMillis()

        // Filter out excluded IDs
        val availableIds = allItemIds.filter { it !in excludeIds }
        if (availableIds.isEmpty()) return emptyList()

        // Calculate weights for each item
        val weights = availableIds.map { id ->
            val encountered = items[id] ?: EncounteredItem(id)
            id to encountered.calculateWeight(currentTime)
        }

        // Weighted random selection
        val selected = mutableListOf<String>()
        val remaining = weights.toMutableList()

        repeat(minOf(count, remaining.size)) {
            if (remaining.isEmpty()) return@repeat

            val totalWeight = remaining.sumOf { it.second }
            var randomValue = Random.nextDouble() * totalWeight

            for ((id, weight) in remaining) {
                randomValue -= weight
                if (randomValue <= 0) {
                    selected.add(id)
                    remaining.removeAll { it.first == id }
                    break
                }
            }

            // Fallback if no item was selected
            if (selected.size <= it && remaining.isNotEmpty()) {
                val fallback = remaining.first()
                selected.add(fallback.first)
                remaining.removeAll { it.first == fallback.first }
            }
        }

        return selected
    }

    /**
     * Get a single weighted random item
     */
    fun selectWeightedItem(
        gameMode: GameMode,
        allItemIds: List<String>,
        excludeIds: Set<String> = emptySet()
    ): String? {
        return selectWeightedItems(gameMode, allItemIds, 1, excludeIds).firstOrNull()
    }

    /**
     * Shuffle items with weighted probability (items seen less are more likely to be first)
     */
    fun <T> shuffleWeighted(
        gameMode: GameMode,
        items: List<T>,
        idExtractor: (T) -> String
    ): List<T> {
        if (items.isEmpty()) return items

        val ids = items.map { idExtractor(it) }
        val selectedOrder = selectWeightedItems(gameMode, ids, items.size)

        return selectedOrder.mapNotNull { id ->
            items.find { idExtractor(it) == id }
        }
    }

    /**
     * Get encounter statistics for a game mode
     */
    fun getStats(gameMode: GameMode): Map<String, EncounteredItem> {
        return encounteredItems[gameMode]?.toMap() ?: emptyMap()
    }

    /**
     * Clear all encounter history for a game mode
     */
    fun clearHistory(gameMode: GameMode) {
        encounteredItems[gameMode] = mutableMapOf()
        try {
            PreferencesManager.saveEncounteredItems(gameMode.key, emptyMap())
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Clear all encounter history for all modes
     */
    fun clearAllHistory() {
        GameMode.entries.forEach { mode ->
            clearHistory(mode)
        }
    }
}

