package com.example.vietforces.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vietforces.data.repository.AuthRepository
import com.example.vietforces.data.repository.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the UI operation state for auth actions (sign in, sign up, etc.).
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * ViewModel for authentication screens.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuthState.Loading)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signIn(email, password)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(toFriendlyMessage(it, "Đăng nhập thất bại")) }
        }
    }

    fun signUp(email: String, password: String, username: String = "") {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signUp(email, password, username)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(toFriendlyMessage(it, "Đăng ký thất bại")) }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.signInWithGoogle()
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(toFriendlyMessage(it, "Đăng nhập Google thất bại")) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            authRepository.resetPassword(email)
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(toFriendlyMessage(it, "Gửi email thất bại")) }
        }
    }

    fun clearUiState() {
        _uiState.value = AuthUiState.Idle
    }

    /**
     * Maps raw Supabase/network exceptions to user-friendly Vietnamese messages.
     * Supabase-kt throws RestException with messages like:
     * "Error Code: invalid_credentials, Message: Invalid login credentials, URL: https://..."
     */
    private fun toFriendlyMessage(e: Throwable, fallback: String): String {
        val raw = e.message?.lowercase() ?: return fallback
        return when {
            "invalid_credentials" in raw || "invalid login" in raw || "wrong" in raw ->
                "Email hoặc mật khẩu không đúng"
            "email" in raw && ("taken" in raw || "already" in raw || "exists" in raw) ->
                "Email này đã được đăng ký"
            "user already registered" in raw ->
                "Tài khoản đã tồn tại"
            "password" in raw && ("short" in raw || "weak" in raw || "length" in raw || "characters" in raw) ->
                "Mật khẩu phải có ít nhất 6 ký tự"
            "email" in raw && ("invalid" in raw || "format" in raw || "not valid" in raw) ->
                "Địa chỉ email không hợp lệ"
            "network" in raw || "unable to resolve" in raw || "connect" in raw || "timeout" in raw ->
                "Không có kết nối mạng. Vui lòng thử lại"
            "rate limit" in raw || "too many" in raw ->
                "Bạn thử quá nhiều lần. Vui lòng đợi vài phút"
            "email not confirmed" in raw ->
                "Vui lòng xác nhận email trước khi đăng nhập"
            else -> fallback
        }
    }
}
