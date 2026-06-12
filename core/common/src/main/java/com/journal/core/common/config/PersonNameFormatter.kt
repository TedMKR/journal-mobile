package com.journal.core.common.config

import java.util.Locale

data class PersonNameParts(
    val fullName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val middleName: String? = null,
    val username: String? = null
)

object PersonNameFormatter {
    private val ruLocale = Locale("ru", "RU")
    private val surnamePattern = Regex(
        "(ов|ев|ёв|ин|ын|ский|цкий|ской|цкой|ая|яя|ова|ева|ёва|ина|ына|енко|ук|юк|ич|ко)$"
    )

    fun format(parts: PersonNameParts): String {
        val fullTokens = splitName(parts.fullName)
        val firstTokens = splitName(parts.firstName)
        val lastTokens = splitName(parts.lastName)
        val middleTokens = splitName(parts.middleName)
        val allTokens = uniqueTokens(firstTokens + lastTokens + middleTokens + fullTokens)

        if (allTokens.isEmpty()) {
            return normalizeText(parts.username)
        }

        val surname = chooseSurname(
            fullTokens = fullTokens.ifEmpty { allTokens },
            firstTokens = firstTokens,
            lastTokens = lastTokens
        ) ?: return allTokens.joinToString(" ")

        val rest = allTokens.filterNot { tokenEquals(it, surname) }
        return (listOf(surname) + rest).joinToString(" ")
    }

    fun formatFullName(fullName: String?, fallback: String? = null): String =
        format(PersonNameParts(fullName = fullName, username = fallback))

    fun formatInitials(fullName: String?): String {
        val tokens = splitName(formatFullName(fullName))
        if (tokens.size <= 1) return normalizeText(fullName)

        val surname = tokens.first()
        val initials = tokens
            .drop(1)
            .filterNot { tokenEquals(it, surname) }
            .joinToString("") { "${it.first()}." }

        return if (initials.isBlank()) surname else "$surname $initials"
    }

    fun firstNameFromFullName(fullName: String?): String? {
        val tokens = splitName(formatFullName(fullName))
        return tokens.getOrNull(1)?.takeIf(String::isNotBlank)
            ?: tokens.firstOrNull()?.takeIf(String::isNotBlank)
    }

    private fun normalizeText(value: String?): String =
        value.orEmpty().trim().replace(Regex("\\s+"), " ")

    private fun splitName(value: String?): List<String> =
        normalizeText(value)
            .split(" ")
            .filter { it.isNotBlank() && !Regex("^[.\\s]+$").matches(it) }

    private fun uniqueTokens(tokens: List<String>): List<String> {
        val seen = linkedSetOf<String>()
        return tokens.filter { token ->
            val key = token.lowercase(ruLocale)
            if (seen.contains(key)) {
                false
            } else {
                seen.add(key)
                true
            }
        }
    }

    private fun tokenEquals(a: String, b: String): Boolean =
        a.lowercase(ruLocale) == b.lowercase(ruLocale)

    private fun includesToken(tokens: List<String>, token: String): Boolean =
        tokens.any { tokenEquals(it, token) }

    private fun looksLikeSurname(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        return surnamePattern.containsMatchIn(token.lowercase(ruLocale))
    }

    private fun chooseSurname(
        fullTokens: List<String>,
        firstTokens: List<String>,
        lastTokens: List<String>
    ): String? {
        if (firstTokens.size == 1 && lastTokens.size > 1 && includesToken(lastTokens, firstTokens[0])) {
            return firstTokens[0]
        }

        if (lastTokens.size == 1 && firstTokens.size > 1 && includesToken(firstTokens, lastTokens[0])) {
            return lastTokens[0]
        }

        if (lastTokens.size == 1) return lastTokens[0]

        if (lastTokens.size > 1 && firstTokens.size == 1 && looksLikeSurname(firstTokens[0])) {
            return firstTokens[0]
        }

        if (fullTokens.size >= 2 && looksLikeSurname(fullTokens[1]) && !looksLikeSurname(fullTokens[0])) {
            return fullTokens[1]
        }

        return fullTokens.firstOrNull() ?: lastTokens.firstOrNull()
    }
}
