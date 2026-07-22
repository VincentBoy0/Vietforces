package com.example.vietforces.data.service

import com.example.vietforces.data.repository.AuthRepository
import com.example.vietforces.data.repository.ProgressRepository
import com.example.vietforces.data.storage.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles one-time migration of local guest progress to Supabase on first login (ONBOARD-03).
 *
 * Gate: [PreferencesManager.getMigrationCompleted()] — once true, subsequent calls are no-ops.
 * This prevents re-migration on every login even if the user reinstalls.
 *
 * Scope of migration: ELO, currentStreak, longestStreak, words_learned_count, last_practiced.
 * Spaced-repetition weights (EncounteredItemsManager) are deferred — see CONTEXT.md deferred list.
 */
@Singleton
class MigrationService @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val authRepository: AuthRepository
) {

    /**
     * Migrate local guest progress to the cloud exactly once.
     *
     * - Returns [Result.success] immediately if migration already completed or user is not logged in.
     * - On first successful sync, sets the migration_completed flag so this never runs again.
     */
    suspend fun migrateIfNeeded(): Result<Unit> {
        // Guard: skip if already migrated
        if (PreferencesManager.getMigrationCompleted()) return Result.success(Unit)

        // Require an authenticated user
        authRepository.currentUserId
            ?: return Result.failure(Exception("Not logged in — migration deferred"))

        // Push local progress to cloud
        val syncResult = progressRepository.syncToCloud()
        if (syncResult.isFailure) return syncResult

        // Mark migration as done — idempotent: if the flag is cleared it just re-syncs the same data
        PreferencesManager.setMigrationCompleted(true)

        return Result.success(Unit)
    }
}
