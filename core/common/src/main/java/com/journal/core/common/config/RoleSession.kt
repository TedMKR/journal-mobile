package com.journal.core.common.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class RoleSession(initialRole: String) {
    private val _role = MutableStateFlow(initialRole)
    val role: StateFlow<String> = _role.asStateFlow()

    fun setRole(newRole: String) {
        _role.value = newRole
    }
}
