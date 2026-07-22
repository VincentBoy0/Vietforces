package com.example.vietforces.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for repository-layer bindings.
 *
 * [ProgressRepository] and [RemoteProgressSource] both use @Singleton @Inject constructor —
 * Hilt auto-binds them. No explicit @Provides needed.
 *
 * Add @Provides / @Binds here when future repositories require factory logic or interface binding.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule
