package com.example.vietforces.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents the current authentication state of the user.
 */
sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val userId: String) : AuthState()
    object NotAuthenticated : AuthState()
}

/**
 * Repository interface for authentication operations.
 */
interface AuthRepository {
    val authState: Flow<AuthState>
    val currentUserId: String?
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signInWithGoogle(): Result<Unit>
    suspend fun signOut(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
}

/**
 * Supabase-backed implementation of AuthRepository.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    override val authState: Flow<AuthState> = supabase.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Authenticated -> AuthState.Authenticated(
                status.session.user?.id ?: ""
            )
            is SessionStatus.NotAuthenticated -> AuthState.NotAuthenticated
            else -> AuthState.Loading
        }
    }

    override val currentUserId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(): Result<Unit> {
        return try {
            supabase.auth.signInWith(Google)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            supabase.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
