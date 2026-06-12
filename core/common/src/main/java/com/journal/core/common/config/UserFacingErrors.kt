package com.journal.core.common.config

import java.io.IOException

fun Throwable.userFacingMessage(defaultMessage: String): String {
    val httpCode = httpCodeOrNull()
    return when {
        isNetworkError() ->
            "Нет подключения к интернету. Ранее сохранённых данных для этого экрана нет."

        httpCode == 401 || httpCode == 403 ->
            "Сессия истекла или доступ запрещён. Войдите в аккаунт снова."

        httpCode == 409 ->
            "Данные изменились на сервере. Обновите экран и попробуйте снова."

        httpCode == 400 || httpCode == 422 ->
            "Не удалось сохранить данные: проверьте введённые значения."

        httpCode != null && httpCode >= 500 ->
            "Сервер временно недоступен. Попробуйте позже."

        else -> defaultMessage
    }
}

private fun Throwable.httpCodeOrNull(): Int? =
    causeChain().firstNotNullOfOrNull { throwable ->
        runCatching {
            throwable.javaClass.methods
                .firstOrNull { it.name == "code" && it.parameterCount == 0 }
                ?.invoke(throwable) as? Int
        }.getOrNull()
    }

private fun Throwable.isNetworkError(): Boolean =
    causeChain().any { it is IOException || it.isNetworkMessage() }

private fun Throwable.causeChain(): Sequence<Throwable> =
    generateSequence(this) { it.cause }

private fun Throwable.isNetworkMessage(): Boolean {
    val text = message.orEmpty().lowercase()
    return listOf(
        "failed to connect",
        "connection refused",
        "timeout",
        "timed out",
        "unable to resolve host",
        "software caused connection abort",
        "network is unreachable",
        "no address associated with hostname"
    ).any(text::contains)
}
