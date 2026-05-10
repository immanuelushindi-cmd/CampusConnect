package com.campusconnect.app.utils

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val cause: Throwable? = null) : UiState<Nothing>()

    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
    val isError get() = this is Error

    fun getOrNull(): T? = (this as? Success)?.data
}

// Sync-specific state used in ViewModels that show a separate refresh indicator
sealed class SyncState {
    data object Idle : SyncState()
    data object Loading : SyncState()
    data object Success : SyncState()
    data class Error(val msg: String) : SyncState()
}
