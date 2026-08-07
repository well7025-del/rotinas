package br.com.rotinas.etiquetas

/**
 * Um item do pedido, já normalizado: "3 camisa São Paulo M" vira
 * quantidade=3, descricao="Camisa São Paulo", tamanho="M".
 */
data class OrderItem(
    val quantity: Int,
    val description: String,
    val size: String = "",
) {
    /** Linha pronta para impressão, sem a coluna de quantidade. */
    fun label(): String =
        if (size.isBlank()) description else "$description  -  $size"
}

/**
 * O pedido inteiro, do jeito que sai da fala e entra na etiqueta.
 */
data class Order(
    val customer: String,
    val place: String = "",
    val items: List<OrderItem> = emptyList(),
    val note: String = "",
) {
    val totalPieces: Int get() = items.sumOf { it.quantity }

    val isEmpty: Boolean
        get() = customer.isBlank() && place.isBlank() && items.isEmpty() && note.isBlank()

    /**
     * Texto simples do pedido, usado na tela de conferência e como formato
     * de edição manual (o mesmo texto volta a ser lido pelo [OrderParser]).
     */
    fun toPlainText(): String = buildString {
        append(customer)
        if (place.isNotBlank()) append(", ").append(place)
        if (items.isNotEmpty()) {
            append(": ")
            append(
                items.joinToString(", ") { item ->
                    val size = if (item.size.isBlank()) "" else " ${item.size}"
                    "${item.quantity} ${item.description}$size"
                },
            )
        }
        if (note.isNotBlank()) append(" (").append(note).append(")")
    }
}
