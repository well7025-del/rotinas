package br.com.rotinas.etiquetas

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderParserTest {

    private val parser = OrderParser(
        listOf("Caruaru", "Palmares", "Recife", "Garanhuns", "Santa Cruz do Capibaribe", "Sao Paulo"),
    )

    // ------------------------------------------- os dois exemplos do pedido

    @Test
    fun `nome e cidade sem pontuacao nenhuma`() {
        val order = parser.parse("pedido de Aline Soares Caruaru")

        assertEquals("Aline Soares", order.customer)
        assertEquals("Caruaru", order.place)
        assertTrue(order.items.isEmpty())
    }

    @Test
    fun `pedido completo com virgula e dois pontos`() {
        val order = parser.parse(
            "pedido Wellington Oliveira, Palmares: 2 camisa sport G, 3 camisa São Paulo M",
        )

        assertEquals("Wellington Oliveira", order.customer)
        assertEquals("Palmares", order.place)
        assertEquals(
            listOf(
                OrderItem(2, "Camisa Sport", "G"),
                OrderItem(3, "Camisa São Paulo", "M"),
            ),
            order.items,
        )
        assertEquals(5, order.totalPieces)
    }

    @Test
    fun `mesmo pedido quando o reconhecedor nao poe pontuacao`() {
        val order = parser.parse(
            "pedido Wellington Oliveira Palmares 2 camisa sport G 3 camisa São Paulo M",
        )

        assertEquals("Wellington Oliveira", order.customer)
        assertEquals("Palmares", order.place)
        assertEquals(
            listOf(
                OrderItem(2, "Camisa Sport", "G"),
                OrderItem(3, "Camisa São Paulo", "M"),
            ),
            order.items,
        )
    }

    // ------------------------------------------------------------ variações

    @Test
    fun `quantidade falada por extenso`() {
        val order = parser.parse("pedido Maria Silva Garanhuns duas calças jeans")

        assertEquals("Maria Silva", order.customer)
        assertEquals("Garanhuns", order.place)
        assertEquals(listOf(OrderItem(2, "Calças Jeans")), order.items)
    }

    @Test
    fun `pontuacao ditada em voz alta`() {
        val order = parser.parse("pedido Ana vírgula Recife dois pontos 1 vestido P")

        assertEquals("Ana", order.customer)
        assertEquals("Recife", order.place)
        assertEquals(listOf(OrderItem(1, "Vestido", "P")), order.items)
    }

    @Test
    fun `tamanho anunciado por numero`() {
        val order = parser.parse("pedido Carlos, Recife: 1 calça tamanho 42")

        assertEquals(listOf(OrderItem(1, "Calça", "42")), order.items)
    }

    @Test
    fun `numero no meio da descricao nao vira tamanho`() {
        val order = parser.parse("pedido Carlos, Recife: 2 camisa 10 do Brasil")

        assertEquals(1, order.items.size)
        assertEquals(2, order.items[0].quantity)
        assertEquals("", order.items[0].size)
        assertTrue(order.items[0].description.contains("10"))
    }

    @Test
    fun `cidade de nome composto`() {
        val order = parser.parse("pedido de Joana Lima Santa Cruz do Capibaribe")

        assertEquals("Joana Lima", order.customer)
        assertEquals("Santa Cruz do Capibaribe", order.place)
    }

    @Test
    fun `cidade anunciada com preposicao`() {
        val order = parser.parse("pedido de Aline Soares de Caruaru")

        assertEquals("Aline Soares", order.customer)
        assertEquals("Caruaru", order.place)
    }

    @Test
    fun `cidade fora da lista aparece depois do marcador para`() {
        val order = parser.parse("pedido de Rita Nunes para Xique-Xique")

        assertEquals("Rita Nunes", order.customer)
        assertEquals("Xique-Xique", order.place)
    }

    @Test
    fun `sem cidade fica so o nome`() {
        val order = parser.parse("pedido João Pedro")

        assertEquals("João Pedro", order.customer)
        assertEquals("", order.place)
        assertTrue(order.items.isEmpty())
    }

    @Test
    fun `observacao entre parenteses`() {
        val order = parser.parse("pedido Ana, Recife: 1 vestido P (entregar sábado)")

        assertEquals("entregar sábado", order.note)
        assertEquals(listOf(OrderItem(1, "Vestido", "P")), order.items)
    }

    @Test
    fun `unidades e preposicao entre a quantidade e a peca`() {
        val order = parser.parse("pedido Ana, Recife: 3 unidades de bermuda GG")

        assertEquals(listOf(OrderItem(3, "Bermuda", "GG")), order.items)
    }

    @Test
    fun `texto vazio nao quebra`() {
        val order = parser.parse("   ")

        assertTrue(order.isEmpty)
    }

    @Test
    fun `parser sem lista de cidades continua funcionando`() {
        val order = OrderParser().parse("pedido Ana, Recife: 1 vestido P")

        assertEquals("Ana", order.customer)
        assertEquals("Recife", order.place)
    }

    // ----------------------------------------- ida e volta pela tela de edição

    @Test
    fun `texto simples volta a ser o mesmo pedido`() {
        val original = parser.parse(
            "pedido Wellington Oliveira, Palmares: 2 camisa sport G, 3 camisa São Paulo M",
        )
        val reparsed = parser.parse(original.toPlainText())

        assertEquals(original.customer, reparsed.customer)
        assertEquals(original.place, reparsed.place)
        assertEquals(original.items, reparsed.items)
    }
}
