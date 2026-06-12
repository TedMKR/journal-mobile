package com.journal.core.common.config

data class AppConfig(
    val apiBaseUrl: String,
    val openApiUrl: String,
    val keycloakBaseUrl: String,
    val keycloakRealm: String,
    val keycloakClientId: String
)
