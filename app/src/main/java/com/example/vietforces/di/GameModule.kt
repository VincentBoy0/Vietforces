package com.example.vietforces.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for game-layer repository bindings.
 *
 * [EloRepository] and [StreakRepository] both use @Singleton @Inject constructor —
 * Hilt auto-binds them. No explicit @Provides or @Binds needed here.
 *
 * [AuthRepository] → [AuthRepositoryImpl] binding already exists in [AuthModule].
 *
 * Add @Provides / @Binds here when future game repositories require factory logic
 * or interface binding.
 */
@Module
@InstallIn(SingletonComponent::class)
object GameModule
