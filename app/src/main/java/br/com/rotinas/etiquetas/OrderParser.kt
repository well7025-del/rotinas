package br.com.rotinas.etiquetas

import br.com.rotinas.etiquetas.TextTools.fold
import br.com.rotinas.etiquetas.TextTools.squeeze
import br.com.rotinas.etiquetas.TextTools.titleCase

/**
 * Transforma a frase ditada em um [Order] estruturado.
 *
 * Exemplos que precisam funcionar:
 *  - "pedido de Aline Soares Caruaru"
 *  - "pedido Wellington Oliveira, Palmares: 2 camisa sport G, 3 camisa São Paulo M"
 *
 * A estratégia é em camadas: quando a fala vem com pontuação (o reconhecedor
 * do Android costuma inserir vírgula e dois-pontos), usamos a pontuação, que é
 * precisa. Sem pontuação, caímos em heurísticas: lista de cidades conhecidas
 * para separar nome de local, e palavras de quantidade para achar onde começam
 * os itens.
 */
class OrderParser(knownPlaces: Collection<String> = emptyList()) {

    private val foldedPlaces: Set<String> = knownPlaces.map { fold(it) }.filter { it.isNotEmpty() }.toSet()

    fun parse(rawText: String): Order {
        val text = squeeze(stripOpening(spokenPunctuation(rawText)))
        if (text.isBlank()) return Order(customer = "")

        val (withoutNote, note) = extractNote(text)
        val (headerPart, itemsPart) = splitHeaderAndItems(withoutNote)
        val (customer, place) = parseHeader(headerPart)

        return Order(
            customer = customer,
            place = place,
            items = parseItems(itemsPart),
            note = note,
        )
    }

    // ---------------------------------------------------------------- limpeza

    /** "vírgula" e "dois pontos" ditados viram pontuação de verdade. */
    private fun spokenPunctuation(text: String): String =
        text.replace(Regex("(?i)\\s*\\bdois\\s+pontos\\b\\s*"), ": ")
            .replace(Regex("(?i)\\s*\\bv[ií]rgula\\b\\s*"), ", ")
            .replace(Regex("(?i)\\s*\\bponto\\s+e\\s+v[ií]rgula\\b\\s*"), "; ")

    /** Remove o "cabeçalho falado": "novo pedido de", "encomenda para", etc. */
    private fun stripOpening(text: String): String =
        text.replace(
            Regex(
                "(?i)^\\s*(novo|nova)?\\s*(pedido|pedidos|encomenda|encomendas|nota|etiqueta)s?\\b" +
                    "\\s*(de|do|da|dos|das|para|pra|p/)?\\s*",
            ),
            "",
        )

    /** Observação entre parênteses ou depois de "obs". */
    private fun extractNote(text: String): Pair<String, String> {
        Regex("\\(([^)]*)\\)\\s*$").find(text)?.let { match ->
            return text.removeRange(match.range).trim() to match.groupValues[1].trim()
        }
        Regex("(?i)\\s*\\b(obs|observa[çc][ãa]o|observa[çc][õo]es)\\b\\s*:?\\s*").find(text)?.let { match ->
            val note = text.substring(match.range.last + 1).trim()
            if (note.isNotEmpty()) return text.substring(0, match.range.first).trim() to note
        }
        return text to ""
    }

    // -------------------------------------------------- separar nome x itens

    private fun splitHeaderAndItems(text: String): Pair<String, String> {
        val colon = text.indexOf(':')
        if (colon >= 0) {
            return text.substring(0, colon).trim() to text.substring(colon + 1).trim()
        }

        Regex("(?i)\\s*\\b(itens|item|contendo|levando|sendo)\\b\\s*").find(text)?.let { match ->
            return text.substring(0, match.range.first).trim() to
                text.substring(match.range.last + 1).trim()
        }

        val start = findItemsStart(text)
        if (start > 0) {
            return text.substring(0, start).trim().trimEnd(',', ';') to text.substring(start).trim()
        }
        return text.trim() to ""
    }

    /**
     * Sem pontuação nenhuma: os itens começam na primeira palavra de quantidade
     * que ainda seja seguida por uma descrição. Exige pelo menos uma palavra de
     * cabeçalho antes, para não engolir o nome do cliente.
     */
    private fun findItemsStart(text: String): Int {
        val tokens = tokenize(text)
        for (index in 1 until tokens.size - 1) {
            if (startsNewItem(tokens, index)) return tokens[index].start
        }
        return -1
    }

    /**
     * Um número só abre um item novo se vier seguido do nome de uma peça.
     * Em "2 camisa 10 do Brasil" o "10" é o número da camisa, não uma
     * quantidade — o que denuncia isso é a preposição logo depois.
     */
    private fun startsNewItem(tokens: List<Token>, index: Int): Boolean {
        if (quantityOf(tokens[index].text) == null) return false
        val next = tokens.getOrNull(index + 1)?.text ?: return false
        return fold(next) !in ITEM_BLOCKERS
    }

    // ------------------------------------------------------ nome + localidade

    private fun parseHeader(header: String): Pair<String, String> {
        if (header.isBlank()) return "" to ""

        val comma = header.indexOf(',')
        if (comma >= 0) {
            val name = header.substring(0, comma).trim()
            val place = header.substring(comma + 1).trim().trimEnd(',', ';', '.')
            return titleCase(name) to titleCase(place)
        }

        placeBySuffix(header)?.let { return it }
        placeByMarker(header)?.let { return it }

        return titleCase(header) to ""
    }

    /** Casa o final da frase com a lista de cidades conhecidas (maior casamento primeiro). */
    private fun placeBySuffix(header: String): Pair<String, String>? {
        if (foldedPlaces.isEmpty()) return null
        val words = header.split(" ").filter { it.isNotEmpty() }
        val maxWords = minOf(4, words.size - 1)
        for (count in maxWords downTo 1) {
            val candidate = words.takeLast(count).joinToString(" ")
            if (fold(candidate) in foldedPlaces) {
                val name = dropTrailingConnectors(words.dropLast(count))
                if (name.isNotEmpty()) {
                    return titleCase(name.joinToString(" ")) to titleCase(candidate)
                }
            }
        }
        return null
    }

    /** "Aline Soares para Caruaru" / "Aline Soares em Palmares". */
    private fun placeByMarker(header: String): Pair<String, String>? {
        val match = Regex("(?i)\\s\\b(para|pra|em|cidade de|cidade)\\b\\s").findAll(header).lastOrNull()
            ?: return null
        val name = header.substring(0, match.range.first).trim()
        val place = header.substring(match.range.last + 1).trim()
        if (name.isEmpty() || place.isEmpty()) return null
        if (place.split(" ").size > 4) return null
        return titleCase(name) to titleCase(place)
    }

    private fun dropTrailingConnectors(words: List<String>): List<String> {
        var end = words.size
        while (end > 0 && fold(words[end - 1]) in HEADER_CONNECTORS) end--
        return words.subList(0, end)
    }

    // ------------------------------------------------------------- os itens

    private fun parseItems(itemsPart: String): List<OrderItem> {
        if (itemsPart.isBlank()) return emptyList()

        val pieces = if (itemsPart.any { it == ',' || it == ';' }) {
            itemsPart.split(',', ';')
        } else {
            splitByQuantity(itemsPart)
        }

        return pieces.mapNotNull { parseItem(it) }
    }

    /** Fallback sem pontuação: cada palavra de quantidade abre um item novo. */
    private fun splitByQuantity(text: String): List<String> {
        val tokens = tokenize(text)
        if (tokens.isEmpty()) return emptyList()

        val cuts = mutableListOf(0)
        for (index in 1 until tokens.size - 1) {
            // Só corta se o item atual já tem descrição e ainda sobra palavra depois.
            if (startsNewItem(tokens, index)) cuts += tokens[index].start
        }
        cuts += text.length

        return cuts.zipWithNext { from, to -> text.substring(from, to).trim() }
            .filter { it.isNotEmpty() }
    }

    private fun parseItem(piece: String): OrderItem? {
        var words = tokenize(piece).map { it.text }.toMutableList()
        if (words.isEmpty()) return null

        var quantity = 1
        quantityOf(words.first())?.let {
            quantity = it
            words.removeAt(0)
            // "2 unidades de camisa" -> descarta o contador e a preposição
            while (words.isNotEmpty() && fold(words.first()) in QUANTITY_FILLERS) words.removeAt(0)
        }
        if (words.isEmpty()) return null

        val size = extractSize(words)
        words = words.filter { it.isNotBlank() }.toMutableList()
        if (words.isEmpty()) return null

        return OrderItem(
            quantity = quantity.coerceAtLeast(1),
            description = titleCase(words.joinToString(" ")),
            size = size,
        )
    }

    /**
     * Tira o tamanho do fim da lista de palavras (modifica [words] no lugar).
     * Aceita "G", "GG", "tamanho 42" e a letra falada por extenso ("gê", "eme").
     */
    private fun extractSize(words: MutableList<String>): String {
        if (words.size < 2) return ""

        val last = fold(words.last()).trim('.', '-')
        val explicitMarker = words.size >= 3 && fold(words[words.size - 2]) in SIZE_MARKERS

        SIZE_WORDS[last]?.let { size ->
            words.removeAt(words.size - 1)
            if (explicitMarker) words.removeAt(words.size - 1)
            return size
        }

        // Número só vira tamanho quando anunciado ("tamanho 42", "número 40"),
        // senão seria confundido com número de camisa.
        if (explicitMarker && last.toIntOrNull() != null) {
            words.removeAt(words.size - 1)
            words.removeAt(words.size - 1)
            return last
        }
        return ""
    }

    // ------------------------------------------------------------ auxiliares

    private data class Token(val text: String, val start: Int)

    private fun tokenize(text: String): List<Token> =
        Regex("[^\\s,;:]+").findAll(text).map { Token(it.value, it.range.first) }.toList()

    /** Converte "3", "3x" ou "três" em número. Devolve null se não for quantidade. */
    private fun quantityOf(token: String): Int? {
        val clean = fold(token).trim('.', '-')
        if (clean.isEmpty()) return null

        Regex("^(\\d{1,3})x?$").find(clean)?.let { match ->
            return match.groupValues[1].toIntOrNull()?.takeIf { it in 1..999 }
        }
        return NUMBER_WORDS[clean]
    }

    private companion object {
        val HEADER_CONNECTORS = setOf("de", "da", "do", "dos", "das", "em", "para", "pra", "no", "na", "cidade", "-")

        /** Depois de um número, estas palavras indicam que ele não era quantidade. */
        val ITEM_BLOCKERS = setOf(
            "de", "do", "da", "dos", "das", "e", "no", "na", "nos", "nas", "com", "para", "pra",
            "reais", "real", "anos", "ano",
        )

        val QUANTITY_FILLERS = setOf(
            "x", "unidade", "unidades", "peca", "pecas", "pç", "pcs", "de", "do", "da", "dos", "das",
        )

        val SIZE_MARKERS = setOf("tamanho", "numero", "num", "tam")

        val SIZE_WORDS = mapOf(
            "pp" to "PP", "p" to "P", "m" to "M", "g" to "G", "gg" to "GG",
            "xg" to "XG", "xgg" to "XGG", "xxg" to "XXG", "eg" to "EG", "egg" to "EGG",
            "u" to "U", "un" to "U", "unico" to "U",
            // letras ditadas por extenso
            "pe" to "P", "eme" to "M", "ge" to "G", "gege" to "GG", "xis ge" to "XG",
        )

        val NUMBER_WORDS = mapOf(
            "um" to 1, "uma" to 1, "dois" to 2, "duas" to 2, "tres" to 3, "quatro" to 4,
            "cinco" to 5, "seis" to 6, "sete" to 7, "oito" to 8, "nove" to 9, "dez" to 10,
            "onze" to 11, "doze" to 12, "treze" to 13, "quatorze" to 14, "catorze" to 14,
            "quinze" to 15, "dezesseis" to 16, "dezessete" to 17, "dezoito" to 18,
            "dezenove" to 19, "vinte" to 20, "trinta" to 30, "quarenta" to 40,
            "cinquenta" to 50, "cem" to 100,
        )
    }
}
