package br.com.rotinas.etiquetas

import java.text.Normalizer
import java.util.Locale

/** Utilidades de texto compartilhadas pelo parser e pelo desenho da etiqueta. */
object TextTools {

    private val PT_BR = Locale.forLanguageTag("pt-BR")

    /** Palavras que ficam em minúsculo no meio de nomes próprios. */
    private val LOWERCASE_PARTICLES = setOf(
        "de", "da", "do", "das", "dos", "e", "di", "du", "del", "della", "la", "das",
    )

    private val COMBINING_MARKS = Regex("\\p{Mn}+")

    /** Remove acentos e passa para minúsculo — usado só para comparar, nunca para exibir. */
    fun fold(text: String): String =
        COMBINING_MARKS.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
            .lowercase(PT_BR)
            .trim()

    fun upper(text: String): String = text.uppercase(PT_BR)

    /** Colapsa espaços repetidos e remove espaço antes de pontuação. */
    fun squeeze(text: String): String =
        text.replace(Regex("\\s+"), " ")
            .replace(Regex("\\s+([,;:.])"), "$1")
            .trim()

    /**
     * "wellington OLIVEIRA da silva" -> "Wellington Oliveira da Silva".
     * Preserva siglas curtas já em maiúsculo (PP, GG, XG).
     */
    fun titleCase(text: String): String {
        val words = squeeze(text).split(" ").filter { it.isNotEmpty() }
        return words.mapIndexed { index, word ->
            when {
                word.length <= 3 && word == word.uppercase(PT_BR) && word.any { it.isLetter() } -> word
                index > 0 && fold(word) in LOWERCASE_PARTICLES -> word.lowercase(PT_BR)
                else -> capitalizeWord(word)
            }
        }.joinToString(" ")
    }

    /** Trata hífen e apóstrofo: "sao joao-do-monte" -> "Sao Joao-Do-Monte". */
    private fun capitalizeWord(word: String): String {
        val builder = StringBuilder(word.length)
        var startOfWord = true
        for (char in word) {
            if (startOfWord && char.isLetter()) {
                builder.append(char.uppercaseChar())
                startOfWord = false
            } else {
                builder.append(char.lowercaseChar())
                if (char == '-' || char == '\'' || char == '.') startOfWord = true
            }
        }
        return builder.toString()
    }
}
