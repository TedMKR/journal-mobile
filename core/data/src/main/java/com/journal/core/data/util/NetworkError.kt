package com.journal.core.data.util

import retrofit2.HttpException
import java.io.IOException

sealed class NetworkError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkUnavailable(cause: IOException) :
        NetworkError(cause.message ?: "Network unavailable", cause)

    class ServerError(
        val code: Int,
        val body: String? = null,
        cause: Throwable? = null
    ) : NetworkError("Server error $code", cause)

    class AuthError(
        val code: Int,
        val body: String? = null,
        val invalidGrant: Boolean = false,
        cause: Throwable? = null
    ) : NetworkError("Authentication error $code", cause)

    class ValidationError(
        val code: Int,
        val body: String? = null,
        cause: Throwable? = null
    ) : NetworkError("Validation error $code", cause)

    class ConflictError(
        val code: Int,
        val body: String? = null,
        cause: Throwable? = null
    ) : NetworkError("Conflict error $code", cause)

    class Unknown(cause: Throwable) :
        NetworkError(cause.message ?: "Unknown network error", cause)

    companion object {
        fun fromHttp(code: Int, body: String? = null, authEndpoint: Boolean = false): NetworkError {
            val invalidGrant = body?.contains("invalid_grant", ignoreCase = true) == true
            return when {
                authEndpoint && (code == 400 || code == 401) -> AuthError(
                    code = code,
                    body = body,
                    invalidGrant = invalidGrant
                )
                code == 401 || code == 403 -> AuthError(code = code, body = body)
                code == 409 -> ConflictError(code = code, body = body)
                code == 400 || code == 422 -> ValidationError(code = code, body = body)
                code >= 500 -> ServerError(code = code, body = body)
                else -> ServerError(code = code, body = body)
            }
        }
    }
}

fun Throwable.toNetworkError(): NetworkError = when (this) {
    is NetworkError -> this
    is IOException -> NetworkError.NetworkUnavailable(this)
    is HttpException -> NetworkError.fromHttp(
        code = code(),
        body = runCatching { response()?.errorBody()?.string() }.getOrNull()
    )
    else -> NetworkError.Unknown(this)
}

fun Throwable.userMessage(defaultMessage: String): String {
    return when (val error = toNetworkError()) {
        is NetworkError.NetworkUnavailable ->
            "Нет подключения к интернету. Ранее сохранённых данных для этого экрана нет."
        is NetworkError.ServerError ->
            "Сервер временно недоступен. Попробуйте позже."
        is NetworkError.AuthError ->
            "Сессия истекла. Войдите в аккаунт снова."
        is NetworkError.ValidationError ->
            "Не удалось сохранить данные: проверьте введённые значения."
        is NetworkError.ConflictError ->
            "Данные изменились на сервере. Обновите экран и попробуйте снова."
        is NetworkError.Unknown ->
            defaultMessage
    }
}
